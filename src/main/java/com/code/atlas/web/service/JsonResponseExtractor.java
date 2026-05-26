package com.code.atlas.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts a JSON object from noisy AI model text (fences, prose, extra braces) and deserializes it.
 */
public final class JsonResponseExtractor {

    private JsonResponseExtractor() {
    }

    public static <T> T parseResponse(String rawResponse, Class<T> type, ObjectMapper objectMapper) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("AI model returned an empty response.");
        }
        IOException lastFailure = null;
        for (String candidate : collectCandidates(rawResponse.trim())) {
            try {
                return objectMapper.readValue(candidate, type);
            } catch (IOException ex) {
                lastFailure = ex;
            }
        }
        String detail = lastFailure != null ? lastFailure.getMessage() : "no valid JSON object found";
        throw new IllegalArgumentException("Failed parsing JSON response: " + detail, lastFailure);
    }

    static List<String> collectCandidates(String raw) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.addAll(extractJsonFencedBlocks(raw));
        candidates.addAll(extractGenericFencedBlocks(raw));
        candidates.addAll(findBalancedJsonObjects(raw));
        if (raw.startsWith("{") && raw.endsWith("}")) {
            candidates.add(raw);
        }
        return List.copyOf(candidates);
    }

    private static List<String> extractJsonFencedBlocks(String raw) {
        List<String> blocks = new ArrayList<>();
        int searchFrom = 0;
        while (searchFrom < raw.length()) {
            int fenceStart = indexOfIgnoreCase(raw, "```json", searchFrom);
            if (fenceStart < 0) {
                break;
            }
            int contentStart = raw.indexOf('\n', fenceStart);
            if (contentStart < 0) {
                break;
            }
            contentStart++;
            int fenceEnd = raw.indexOf("```", contentStart);
            if (fenceEnd < 0) {
                break;
            }
            blocks.add(raw.substring(contentStart, fenceEnd).trim());
            searchFrom = fenceEnd + 3;
        }
        return blocks;
    }

    private static List<String> extractGenericFencedBlocks(String raw) {
        List<String> blocks = new ArrayList<>();
        int searchFrom = 0;
        while (searchFrom < raw.length()) {
            int fenceStart = raw.indexOf("```", searchFrom);
            if (fenceStart < 0) {
                break;
            }
            int langLineEnd = raw.indexOf('\n', fenceStart);
            if (langLineEnd < 0) {
                break;
            }
            String language = raw.substring(fenceStart + 3, langLineEnd).trim();
            if ("json".equalsIgnoreCase(language)) {
                searchFrom = langLineEnd + 1;
                continue;
            }
            int contentStart = langLineEnd + 1;
            int fenceEnd = raw.indexOf("```", contentStart);
            if (fenceEnd < 0) {
                break;
            }
            String content = raw.substring(contentStart, fenceEnd).trim();
            if (content.startsWith("{")) {
                blocks.add(content);
            }
            searchFrom = fenceEnd + 3;
        }
        return blocks;
    }

    private static List<String> findBalancedJsonObjects(String text) {
        List<String> objects = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int start = text.indexOf('{', index);
            if (start < 0) {
                break;
            }
            int end = findBalancedObjectEnd(text, start);
            if (end < 0) {
                index = start + 1;
                continue;
            }
            objects.add(text.substring(start, end + 1));
            index = end + 1;
        }
        return objects;
    }

    private static int findBalancedObjectEnd(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char current = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int indexOfIgnoreCase(String text, String needle, int fromIndex) {
        String lower = text.toLowerCase();
        return lower.indexOf(needle.toLowerCase(), fromIndex);
    }
}
