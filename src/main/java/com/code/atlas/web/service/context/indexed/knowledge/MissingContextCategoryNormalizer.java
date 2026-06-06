package com.code.atlas.web.service.context.indexed.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MissingContextCategoryNormalizer {

    private static final List<String> CANONICAL_CATEGORIES = List.of(
            "migration",
            "frontend",
            "repository",
            "entity",
            "dto",
            "test",
            "config"
    );

    private MissingContextCategoryNormalizer() {
    }

    public static List<String> normalize(List<String> rawCategories) {
        if (rawCategories == null || rawCategories.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : rawCategories) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String lower = raw.toLowerCase(Locale.ROOT);
            for (String category : CANONICAL_CATEGORIES) {
                if (lower.contains(category)) {
                    normalized.add(category);
                }
            }
        }
        return new ArrayList<>(normalized);
    }
}
