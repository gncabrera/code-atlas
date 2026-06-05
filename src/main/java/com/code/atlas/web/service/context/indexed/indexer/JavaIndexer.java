package com.code.atlas.web.service.context.indexed.indexer;

import com.code.atlas.web.service.context.deterministic.ContextSymbolExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class JavaIndexer implements LanguageIndexer {

    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "\\b(class|interface|record|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\bextends\\s+([A-Za-z0-9_]+)");
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("\\bimplements\\s+([A-Za-z0-9_,\\s]+)");
    private static final Pattern FIELD_TYPE_PATTERN = Pattern.compile(
            "private\\s+final\\s+([A-Za-z0-9_]+)\\s+([a-z][A-Za-z0-9_]*)\\s*;"
    );
    private static final Pattern ENTITY_TABLE_PATTERN = Pattern.compile(
            "@Table\\(\\s*name\\s*=\\s*\"([^\"]+)\""
    );
    private static final Pattern ENTITY_CLASS_PATTERN = Pattern.compile("@Entity\\s+(?:public\\s+)?class\\s+([A-Za-z0-9_]+)");
    private static final Pattern REPO_PATTERN = Pattern.compile(
            "interface\\s+([A-Za-z0-9_]+)\\s+extends\\s+JpaRepository<([A-Za-z0-9_]+)"
    );
    private static final Pattern CLASS_REQUEST_MAPPING = Pattern.compile(
            "@RequestMapping\\(\"([^\"]+)\"\\)"
    );
    private static final Pattern METHOD_MAPPING = Pattern.compile(
            "@(Get|Post|Put|Patch|Delete)Mapping(?:\\(\"([^\"]*)\"\\))?"
    );
    private static final Pattern REST_CONTROLLER = Pattern.compile("@RestController");
    private static final Pattern CONTROLLER = Pattern.compile("@Controller");

    private final ContextSymbolExtractor contextSymbolExtractor;

    public JavaIndexer(ContextSymbolExtractor contextSymbolExtractor) {
        this.contextSymbolExtractor = contextSymbolExtractor;
    }

    @Override
    public boolean supports(String extension) {
        return "java".equalsIgnoreCase(extension);
    }

    @Override
    public IndexerOutput index(IndexFileInput input) {
        String content = input.content();
        if (content.isBlank()) {
            return IndexerOutput.empty();
        }
        String primaryType = extractPrimaryType(content);
        List<SymbolRow> symbols = extractSymbols(content);
        List<EndpointRow> endpoints = extractEndpoints(content, primaryType);
        List<GraphEdgeRow> edges = extractGraphEdges(content, primaryType);
        List<DatabaseRow> databaseRows = extractDatabaseRows(content, input.relativePath(), primaryType);
        return new IndexerOutput(symbols, endpoints, edges, databaseRows, List.of());
    }

    private String extractPrimaryType(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    private List<SymbolRow> extractSymbols(String content) {
        List<SymbolRow> symbols = new ArrayList<>();
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = CLASS_PATTERN.matcher(lines[i]);
            if (matcher.find()) {
                symbols.add(new SymbolRow(matcher.group(2), matcher.group(1), i + 1));
            }
        }
        List<String> extracted = contextSymbolExtractor.extractSymbols(content, "java", 30);
        for (String symbol : extracted) {
            if (symbols.stream().noneMatch(row -> row.symbol().equals(symbol.replace("()", "")))) {
                symbols.add(new SymbolRow(symbol.replace("()", ""), "method", 0));
            }
        }
        return symbols;
    }

    private List<EndpointRow> extractEndpoints(String content, String controllerName) {
        if (controllerName.isBlank()) {
            return List.of();
        }
        if (!REST_CONTROLLER.matcher(content).find() && !CONTROLLER.matcher(content).find()) {
            return List.of();
        }
        String basePath = "";
        Matcher baseMatcher = CLASS_REQUEST_MAPPING.matcher(content);
        if (baseMatcher.find()) {
            basePath = baseMatcher.group(1);
        }
        List<EndpointRow> endpoints = new ArrayList<>();
        Matcher methodMatcher = METHOD_MAPPING.matcher(content);
        while (methodMatcher.find()) {
            String method = methodMatcher.group(1).toUpperCase(Locale.ROOT);
            String subPath = methodMatcher.group(2) == null ? "" : methodMatcher.group(2);
            String fullPath = joinPaths(basePath, subPath);
            endpoints.add(new EndpointRow(method, fullPath, controllerName, ""));
        }
        return endpoints;
    }

    private List<GraphEdgeRow> extractGraphEdges(String content, String sourceType) {
        if (sourceType.isBlank()) {
            return List.of();
        }
        List<GraphEdgeRow> edges = new ArrayList<>();
        Matcher extendsMatcher = EXTENDS_PATTERN.matcher(content);
        if (extendsMatcher.find()) {
            edges.add(new GraphEdgeRow(sourceType, extendsMatcher.group(1).trim(), "EXTENDS"));
        }
        Matcher implementsMatcher = IMPLEMENTS_PATTERN.matcher(content);
        if (implementsMatcher.find()) {
            for (String iface : implementsMatcher.group(1).split(",")) {
                String target = iface.trim();
                if (!target.isBlank()) {
                    edges.add(new GraphEdgeRow(sourceType, target, "IMPLEMENTS"));
                }
            }
        }
        Matcher fieldMatcher = FIELD_TYPE_PATTERN.matcher(content);
        while (fieldMatcher.find()) {
            String target = fieldMatcher.group(1);
            if (!target.equals(sourceType) && Character.isUpperCase(target.charAt(0))) {
                edges.add(new GraphEdgeRow(sourceType, target, "USES"));
            }
        }
        return edges;
    }

    private List<DatabaseRow> extractDatabaseRows(String content, String relativePath, String primaryType) {
        List<DatabaseRow> rows = new ArrayList<>();
        Matcher entityMatcher = ENTITY_CLASS_PATTERN.matcher(content);
        if (entityMatcher.find()) {
            String entity = entityMatcher.group(1);
            String table = entity.toLowerCase(Locale.ROOT);
            Matcher tableMatcher = ENTITY_TABLE_PATTERN.matcher(content);
            if (tableMatcher.find()) {
                table = tableMatcher.group(1);
            }
            rows.add(new DatabaseRow(table, entity, "", relativePath));
            return rows;
        }
        Matcher repoMatcher = REPO_PATTERN.matcher(content);
        if (repoMatcher.find()) {
            rows.add(new DatabaseRow("", "", repoMatcher.group(1), relativePath));
            return rows;
        }
        if (relativePath.contains("migration") && relativePath.endsWith(".sql")) {
            rows.add(new DatabaseRow("", "", "", relativePath));
        } else if (primaryType.endsWith("Repository")) {
            rows.add(new DatabaseRow("", "", primaryType, relativePath));
        }
        return rows;
    }

    private String joinPaths(String base, String sub) {
        String normalizedBase = base == null ? "" : base.trim();
        String normalizedSub = sub == null ? "" : sub.trim();
        if (normalizedBase.isEmpty()) {
            return normalizedSub.isEmpty() ? "/" : normalizedSub;
        }
        if (normalizedSub.isEmpty()) {
            return normalizedBase;
        }
        if (normalizedBase.endsWith("/") && normalizedSub.startsWith("/")) {
            return normalizedBase + normalizedSub.substring(1);
        }
        if (!normalizedBase.endsWith("/") && !normalizedSub.startsWith("/")) {
            return normalizedBase + "/" + normalizedSub;
        }
        return normalizedBase + normalizedSub;
    }
}
