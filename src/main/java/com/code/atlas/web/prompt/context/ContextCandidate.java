package com.code.atlas.web.prompt.context;

import java.util.List;

public record ContextCandidate(
        String relativePath,
        int score,
        List<String> reasons,
        List<String> symbols,
        String snippet,
        String snippetLanguage
) {
    public ContextCandidate {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Context candidate path is required.");
        }
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        symbols = symbols == null ? List.of() : List.copyOf(symbols);
        snippet = snippet == null ? "" : snippet;
        snippetLanguage = snippetLanguage == null || snippetLanguage.isBlank() ? "text" : snippetLanguage;
    }
}
