package com.code.atlas.web.service.context.indexed.dto;

import java.util.List;

public record ContextResult(List<RetrievedFile> files) {
    public ContextResult {
        files = files == null ? List.of() : List.copyOf(files);
    }
}
