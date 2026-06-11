package com.code.atlas.web.service.context.indexed.indexer;

import com.code.atlas.web.domain.IndexerProfile;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AngularIndexer implements LanguageIndexer {

    private static final Pattern EXPORT_TYPE_PATTERN = Pattern.compile(
            "\\bexport\\s+(?:default\\s+)?(?:abstract\\s+)?(class|interface|enum)\\s+([A-Za-z0-9_]+)"
    );
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\bextends\\s+([A-Za-z0-9_]+)");
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("\\bimplements\\s+([A-Za-z0-9_,\\s]+?)[\\{<]");
    private static final Pattern DI_PARAM_PATTERN = Pattern.compile(
            "(?:private|public|protected|readonly)\\s+(?:readonly\\s+)?[A-Za-z0-9_]+\\s*:\\s*([A-Z][A-Za-z0-9_]*)"
    );
    private static final Pattern HTTP_CALL_PATTERN = Pattern.compile(
            "\\.(get|post|put|delete|patch)\\s*(?:<[^>]*>)?\\s*\\(\\s*(['\"`])([^'\"`]*)\\2"
    );
    private static final Pattern ROUTE_COMPONENT_PATTERN = Pattern.compile(
            "component\\s*:\\s*([A-Za-z0-9_]+)"
    );
    private static final Pattern CHILD_SELECTOR_PATTERN = Pattern.compile(
            "<([a-z][a-z0-9]*(?:-[a-z0-9]+)+)"
    );
    private static final Pattern STYLE_IMPORT_PATTERN = Pattern.compile(
            "@(?:use|import|forward)\\s+['\"]([^'\"]+)['\"]"
    );

    @Override
    public IndexerProfile profile() {
        return IndexerProfile.ANGULAR;
    }

    @Override
    public boolean supports(String extension) {
        String normalized = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return "ts".equals(normalized)
                || "html".equals(normalized)
                || "scss".equals(normalized)
                || "css".equals(normalized);
    }

    @Override
    public IndexerOutput index(IndexFileInput input) {
        String content = input.content();
        if (content.isBlank()) {
            return IndexerOutput.empty();
        }
        String extension = input.extension().toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "html" -> indexTemplate(input);
            case "scss", "css" -> indexStyle(input);
            default -> indexTypeScript(content);
        };
    }

    private IndexerOutput indexTypeScript(String content) {
        String fileKind = decoratorKind(content);
        List<SymbolRow> symbols = extractSymbols(content, fileKind);
        String primaryType = symbols.isEmpty() ? "" : symbols.get(0).symbol();
        List<GraphEdgeRow> edges = extractGraphEdges(content, primaryType);
        List<FrontendRow> frontendRows = extractFrontendRows(content, primaryType, fileKind);
        return new IndexerOutput(symbols, List.of(), edges, List.of(), frontendRows);
    }

    private IndexerOutput indexTemplate(IndexFileInput input) {
        String component = componentClassFromFile(input.relativePath());
        List<SymbolRow> symbols = new ArrayList<>();
        if (!component.isBlank()) {
            symbols.add(new SymbolRow(component, "template", 0));
        }
        String source = component.isBlank() ? fileLabel(input.relativePath()) : component;
        Set<String> seen = new LinkedHashSet<>();
        List<GraphEdgeRow> edges = new ArrayList<>();
        Matcher matcher = CHILD_SELECTOR_PATTERN.matcher(input.content());
        while (matcher.find()) {
            String selector = matcher.group(1);
            if (selector.startsWith("ng-") || selector.startsWith("svg-")) {
                continue;
            }
            addEdge(edges, seen, source, selector, "USES");
        }
        return new IndexerOutput(symbols, List.of(), edges, List.of(), List.of());
    }

    private IndexerOutput indexStyle(IndexFileInput input) {
        String component = componentClassFromFile(input.relativePath());
        List<SymbolRow> symbols = new ArrayList<>();
        if (!component.isBlank()) {
            symbols.add(new SymbolRow(component, "style", 0));
        }
        String source = component.isBlank() ? fileLabel(input.relativePath()) : component;
        Set<String> seen = new LinkedHashSet<>();
        List<GraphEdgeRow> edges = new ArrayList<>();
        Matcher matcher = STYLE_IMPORT_PATTERN.matcher(input.content());
        while (matcher.find()) {
            String target = matcher.group(1).trim();
            if (!target.isBlank()) {
                addEdge(edges, seen, source, target, "USES");
            }
        }
        return new IndexerOutput(symbols, List.of(), edges, List.of(), List.of());
    }

    private String decoratorKind(String content) {
        if (content.contains("@Component")) {
            return "component";
        }
        if (content.contains("@Injectable")) {
            return "service";
        }
        if (content.contains("@Directive")) {
            return "directive";
        }
        if (content.contains("@Pipe")) {
            return "pipe";
        }
        if (content.contains("@NgModule")) {
            return "module";
        }
        return "";
    }

    private List<SymbolRow> extractSymbols(String content, String fileKind) {
        Set<String> seen = new LinkedHashSet<>();
        List<SymbolRow> symbols = new ArrayList<>();
        String[] lines = content.split("\n", -1);
        boolean firstClass = true;
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = EXPORT_TYPE_PATTERN.matcher(lines[i]);
            if (!matcher.find()) {
                continue;
            }
            String keyword = matcher.group(1);
            String name = matcher.group(2);
            String kind = keyword;
            if ("class".equals(keyword) && firstClass && !fileKind.isBlank()) {
                kind = fileKind;
                firstClass = false;
            }
            if (seen.add(name + "|" + (i + 1))) {
                symbols.add(new SymbolRow(name, kind, i + 1));
            }
        }
        return symbols;
    }

    private List<GraphEdgeRow> extractGraphEdges(String content, String sourceType) {
        if (sourceType.isBlank()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<GraphEdgeRow> edges = new ArrayList<>();
        Matcher extendsMatcher = EXTENDS_PATTERN.matcher(content);
        if (extendsMatcher.find()) {
            addEdge(edges, seen, sourceType, extendsMatcher.group(1).trim(), "EXTENDS");
        }
        Matcher implementsMatcher = IMPLEMENTS_PATTERN.matcher(content);
        if (implementsMatcher.find()) {
            for (String iface : implementsMatcher.group(1).split(",")) {
                String target = iface.trim();
                if (!target.isBlank()) {
                    addEdge(edges, seen, sourceType, target, "IMPLEMENTS");
                }
            }
        }
        for (String injected : extractInjectedTypes(content)) {
            if (!injected.equals(sourceType)) {
                addEdge(edges, seen, sourceType, injected, "USES");
            }
        }
        if (content.contains("Routes") || content.contains("RouterModule")) {
            Matcher routeMatcher = ROUTE_COMPONENT_PATTERN.matcher(content);
            while (routeMatcher.find()) {
                String component = routeMatcher.group(1).trim();
                if (!component.equals(sourceType)) {
                    addEdge(edges, seen, sourceType, component, "USES");
                }
            }
        }
        return edges;
    }

    private void addEdge(List<GraphEdgeRow> edges, Set<String> seen, String source, String target, String relation) {
        if (seen.add(source + "|" + target + "|" + relation)) {
            edges.add(new GraphEdgeRow(source, target, relation));
        }
    }

    private List<FrontendRow> extractFrontendRows(String content, String component, String fileKind) {
        if (component.isBlank() || (!"component".equals(fileKind) && !"service".equals(fileKind))) {
            return List.of();
        }
        Set<String> seenKeys = new LinkedHashSet<>();
        List<FrontendRow> rows = new ArrayList<>();
        for (String injected : extractInjectedTypes(content)) {
            if (seenKeys.add("svc|" + injected)) {
                rows.add(new FrontendRow(component, injected, ""));
            }
        }
        for (String endpoint : extractEndpoints(content)) {
            if (seenKeys.add("ep|" + endpoint)) {
                rows.add(new FrontendRow(component, "", endpoint));
            }
        }
        if (rows.isEmpty()) {
            rows.add(new FrontendRow(component, "", ""));
        }
        return rows;
    }

    private Set<String> extractInjectedTypes(String content) {
        Set<String> types = new LinkedHashSet<>();
        Matcher matcher = DI_PARAM_PATTERN.matcher(content);
        while (matcher.find()) {
            types.add(matcher.group(1).trim());
        }
        return types;
    }

    static String componentClassFromFile(String relativePath) {
        String fileName = fileLabel(relativePath);
        if (!fileName.endsWith(".component")) {
            return "";
        }
        String core = fileName.substring(0, fileName.length() - ".component".length());
        return toPascalCase(core) + "Component";
    }

    static String fileLabel(String relativePath) {
        String normalized = relativePath == null ? "" : relativePath.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String toPascalCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (String token : value.split("[-_.]")) {
            if (token.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.toString();
    }

    private Set<String> extractEndpoints(String content) {
        Set<String> endpoints = new LinkedHashSet<>();
        Matcher matcher = HTTP_CALL_PATTERN.matcher(content);
        while (matcher.find()) {
            String method = matcher.group(1).toUpperCase(Locale.ROOT);
            String url = matcher.group(3).trim();
            if (url.isBlank() || url.contains("${")) {
                continue;
            }
            if (!url.startsWith("/") && !url.contains("/api") && !url.startsWith("http")) {
                continue;
            }
            endpoints.add(method + " " + url);
        }
        return endpoints;
    }
}
