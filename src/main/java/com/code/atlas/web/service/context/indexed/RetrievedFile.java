package com.code.atlas.web.service.context.indexed;

import java.util.List;

public record RetrievedFile(
        String relativePath,
        String language,
        String type,
        int score,
        List<String> reasons,
        List<String> symbols,
        String snippet
) {
    public RetrievedFile {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Retrieved file path is required.");
        }
        language = language == null ? "" : language;
        type = type == null ? "" : type;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        symbols = symbols == null ? List.of() : List.copyOf(symbols);
        snippet = snippet == null ? "" : snippet;
    }
}
