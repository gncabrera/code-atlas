package com.code.atlas.web.service.context;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ContextQueryParser {

    private static final Pattern ENDPOINT_PATTERN = Pattern.compile(
            "(?im)^\\s*endpoint\\s*:\\s*(GET|POST|PUT|PATCH|DELETE)\\s+([^\\s]+)\\s*$"
    );
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-zA-Z0-9_./-]{3,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "feature", "need", "with", "where", "when", "then", "from", "that", "this", "will",
            "and", "for", "the", "api", "endpoint", "schema", "changes", "update", "queries",
            "needed", "auth", "session", "implications", "implement"
    );

    public ContextQuery parse(String userRequest) {
        String normalizedRequest = userRequest == null ? "" : userRequest.trim();
        if (normalizedRequest.isBlank()) {
            throw new IllegalArgumentException("User request is required for context generation.");
        }

        String endpointMethod = "";
        String endpointPath = "";
        Matcher endpointMatcher = ENDPOINT_PATTERN.matcher(normalizedRequest);
        if (endpointMatcher.find()) {
            endpointMethod = endpointMatcher.group(1).toUpperCase(Locale.ROOT);
            endpointPath = endpointMatcher.group(2).trim();
        }

        Set<String> keywords = parseKeywords(normalizedRequest, endpointMethod, endpointPath);
        List<String> focusAreas = parseFocusAreas(normalizedRequest);
        return new ContextQuery(normalizedRequest, endpointMethod, endpointPath, keywords, focusAreas);
    }

    private Set<String> parseKeywords(String request, String endpointMethod, String endpointPath) {
        Set<String> keywords = new LinkedHashSet<>();
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(request.toLowerCase(Locale.ROOT));
        while (tokenMatcher.find()) {
            String token = tokenMatcher.group().trim();
            if (token.length() < 3 || STOP_WORDS.contains(token)) {
                continue;
            }
            keywords.add(token);
            if (token.contains("/")) {
                String[] fragments = token.split("/");
                for (String fragment : fragments) {
                    if (!fragment.isBlank() && fragment.length() >= 3) {
                        keywords.add(fragment);
                    }
                }
            }
        }
        if (!endpointMethod.isBlank()) {
            keywords.add(endpointMethod.toLowerCase(Locale.ROOT));
        }
        if (!endpointPath.isBlank()) {
            keywords.add(endpointPath.toLowerCase(Locale.ROOT));
        }
        return keywords;
    }

    private List<String> parseFocusAreas(String request) {
        String normalized = request.toLowerCase(Locale.ROOT);
        List<String> focusAreas = new ArrayList<>();
        if (normalized.contains("schema") || normalized.contains("migration") || normalized.contains("flyway")) {
            focusAreas.add("migration");
        }
        if (normalized.contains("query") || normalized.contains("queries") || normalized.contains("repository")) {
            focusAreas.add("repository");
        }
        if (normalized.contains("auth") || normalized.contains("session")) {
            focusAreas.add("auth");
        }
        if (normalized.contains("endpoint") || normalized.contains("controller")) {
            focusAreas.add("controller");
        }
        return focusAreas;
    }
}
