package com.code.atlas.web.service.context.indexed.dto;

import java.util.List;

public record KnowledgeResult(
        String architectureFacts,
        List<RetrievedFile> files
) {
    public KnowledgeResult {
        architectureFacts = architectureFacts == null ? "" : architectureFacts;
        files = files == null ? List.of() : List.copyOf(files);
    }
}
