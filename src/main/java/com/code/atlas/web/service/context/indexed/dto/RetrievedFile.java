package com.code.atlas.web.service.context.indexed.dto;

import com.code.atlas.web.domain.ProjectFileIndex;

import java.util.List;

public record RetrievedFile(
        ProjectFileIndex file,
        String language,
        String type,
        int score,
        List<String> reasons,
        List<String> symbols,
        String snippet
) {
    public RetrievedFile {
        if (file == null) {
            throw new IllegalArgumentException("Retrieved file is required.");
        }
        language = language == null ? "" : language;
        type = type == null ? "" : type;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        symbols = symbols == null ? List.of() : List.copyOf(symbols);
        snippet = snippet == null ? "" : snippet;
    }
}
