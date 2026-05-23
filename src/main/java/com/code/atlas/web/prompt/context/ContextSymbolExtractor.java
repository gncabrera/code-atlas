package com.code.atlas.web.prompt.context;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ContextSymbolExtractor {

    private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("\\bclass\\s+([A-Za-z0-9_]+)");
    private static final Pattern JAVA_METHOD_PATTERN = Pattern.compile(
            "\\b(public|protected|private)\\s+[A-Za-z0-9_<>\\[\\], ?]+\\s+([A-Za-z0-9_]+)\\s*\\("
    );
    private static final Pattern SQL_TABLE_PATTERN = Pattern.compile(
            "\\b(create\\s+table|alter\\s+table|from|join|into|update)\\s+([a-zA-Z0-9_]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ENDPOINT_HINT_PATTERN = Pattern.compile(
            "@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\\(\"([^\"]+)\"\\)"
    );

    public List<String> extractSymbols(String content, String extension, int maxSymbols) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        Set<String> symbols = new LinkedHashSet<>();
        if ("java".equals(extension)) {
            appendJavaSymbols(content, symbols);
        } else if ("sql".equals(extension)) {
            appendSqlSymbols(content, symbols);
        } else {
            appendGenericSymbols(content, symbols);
        }
        return symbols.stream().limit(maxSymbols).toList();
    }

    public List<String> extractEndpointHints(String content, int maxHints) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<String> hints = new ArrayList<>();
        Matcher matcher = ENDPOINT_HINT_PATTERN.matcher(content);
        while (matcher.find() && hints.size() < maxHints) {
            String method = switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
                case "getmapping" -> "GET";
                case "postmapping" -> "POST";
                case "putmapping" -> "PUT";
                case "patchmapping" -> "PATCH";
                case "deletemapping" -> "DELETE";
                default -> "";
            };
            String path = matcher.group(2);
            if (!method.isBlank() && !path.isBlank()) {
                hints.add(method + " " + path);
            }
        }
        return hints;
    }

    private void appendJavaSymbols(String content, Set<String> symbols) {
        Matcher classMatcher = JAVA_CLASS_PATTERN.matcher(content);
        while (classMatcher.find()) {
            symbols.add(classMatcher.group(1));
            if (symbols.size() >= 20) {
                return;
            }
        }
        Matcher methodMatcher = JAVA_METHOD_PATTERN.matcher(content);
        while (methodMatcher.find()) {
            symbols.add(methodMatcher.group(2) + "()");
            if (symbols.size() >= 20) {
                return;
            }
        }
    }

    private void appendSqlSymbols(String content, Set<String> symbols) {
        Matcher matcher = SQL_TABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            symbols.add(matcher.group(2));
            if (symbols.size() >= 20) {
                return;
            }
        }
    }

    private void appendGenericSymbols(String content, Set<String> symbols) {
        Pattern genericPattern = Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]{3,}\\b");
        Matcher matcher = genericPattern.matcher(content);
        while (matcher.find()) {
            symbols.add(matcher.group());
            if (symbols.size() >= 20) {
                return;
            }
        }
    }
}
