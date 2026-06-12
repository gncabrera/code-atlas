package com.code.atlas.web.service.context.indexed.engine.context.retriever;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class TermNormalizer {

    private TermNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.toLowerCase(Locale.ROOT).replaceAll("[-_\\s]+", "");
    }

    public static boolean equalsNormalized(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isEmpty() && normalizedLeft.equals(normalizedRight);
    }

    public static boolean containsNormalized(String haystack, String needle) {
        String normalizedNeedle = normalize(needle);
        if (normalizedNeedle.isEmpty()) {
            return false;
        }
        return normalize(haystack).contains(normalizedNeedle);
    }

    public static Set<String> normalizeTerms(Collection<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            String normalizedValue = normalize(value);
            if (!normalizedValue.isEmpty()) {
                normalized.add(normalizedValue);
            }
        }
        return normalized;
    }

}
