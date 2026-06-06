package com.code.atlas.web.service.context.indexed.indexer;

public record IndexFileInput(String relativePath, String extension, String content) {
    public IndexFileInput {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Relative path is required.");
        }
        extension = extension == null ? "" : extension.trim().toLowerCase();
        content = content == null ? "" : content;
    }
}
