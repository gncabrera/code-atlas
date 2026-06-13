package com.code.atlas.web.service.dto;

public record FlatProjectIndexDto(
        String filePath,
        String fileExtension,
        long lastModifiedEpoch,
        String contentHash,
        int tokenCount,
        String metadataJson,
        String metadataContentHash
) {
    public FlatProjectIndexDto {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path is required.");
        }
        if (fileExtension == null || fileExtension.isBlank()) {
            throw new IllegalArgumentException("File extension is required.");
        }
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("Content hash is required.");
        }
        if (tokenCount < 0) {
            throw new IllegalArgumentException("Token count cannot be negative.");
        }
    }
}
