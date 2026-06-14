package com.code.atlas.web.service.context.indexed.engine.context.builder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class GitIgnoreMatcher {

    private final List<Rule> rules;

    private GitIgnoreMatcher(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static GitIgnoreMatcher empty() {
        return new GitIgnoreMatcher(List.of());
    }

    public static GitIgnoreMatcher fromPath(Path gitIgnorePath) {
        if (gitIgnorePath == null || !Files.isRegularFile(gitIgnorePath)) {
            return empty();
        }
        try {
            return fromLines(Files.readAllLines(gitIgnorePath, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            return empty();
        }
    }

    public static GitIgnoreMatcher fromLines(List<String> lines) {
        List<Rule> rules = new ArrayList<>();
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            boolean negated = line.startsWith("!");
            if (negated) {
                line = line.substring(1).trim();
                if (line.isEmpty()) {
                    continue;
                }
            }
            rules.add(new Rule(line, negated));
        }
        return new GitIgnoreMatcher(rules);
    }

    public boolean isIgnored(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || rules.isEmpty()) {
            return false;
        }
        String normalized = normalizePath(relativePath);
        Boolean ignored = null;
        for (Rule rule : rules) {
            if (rule.matches(normalized)) {
                ignored = !rule.negated;
            }
        }
        return Boolean.TRUE.equals(ignored);
    }

    private static String stripComment(String line) {
        int hashIndex = line.indexOf('#');
        if (hashIndex < 0) {
            return line;
        }
        return line.substring(0, hashIndex);
    }

    private static String normalizePath(String relativePath) {
        return relativePath.replace('\\', '/').replaceAll("^/+", "");
    }

    private record Rule(String pattern, boolean negated) {

        boolean matches(String normalizedPath) {
            if (pattern.endsWith("/")) {
                String directory = pattern.substring(0, pattern.length() - 1);
                return matchesDirectory(normalizedPath, directory);
            }
            if (pattern.contains("/")) {
                return matchesPattern(normalizedPath, pattern);
            }
            String fileName = fileName(normalizedPath);
            if (matchesPattern(fileName, pattern)) {
                return true;
            }
            return matchesDirectory(normalizedPath, pattern);
        }

        private boolean matchesDirectory(String normalizedPath, String directoryPattern) {
            if (directoryPattern.isEmpty()) {
                return false;
            }
            String[] segments = normalizedPath.split("/");
            for (String segment : segments) {
                if (matchesPattern(segment, directoryPattern)) {
                    return true;
                }
            }
            return matchesPattern(normalizedPath, directoryPattern);
        }

        private String fileName(String normalizedPath) {
            int slashIndex = normalizedPath.lastIndexOf('/');
            if (slashIndex < 0) {
                return normalizedPath;
            }
            return normalizedPath.substring(slashIndex + 1);
        }

        private boolean matchesPattern(String value, String gitPattern) {
            String regex = toRegex(gitPattern);
            return Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(value)
                    .matches();
        }

        private String toRegex(String gitPattern) {
            StringBuilder regex = new StringBuilder();
            for (int index = 0; index < gitPattern.length(); index++) {
                char current = gitPattern.charAt(index);
                if (current == '*') {
                    regex.append(".*");
                    continue;
                }
                if (".[](){}+?^$|\\".indexOf(current) >= 0) {
                    regex.append('\\');
                }
                regex.append(current);
            }
            return regex.toString();
        }
    }
}
