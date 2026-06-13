package com.code.atlas.web.service.dto;

import java.time.LocalDateTime;

public record IndexStatusDto(
        IndexIntegrityStatus integrity,
        int fileIndexCount,
        int metadataIndexCount,
        LocalDateTime newestUpdatedAt
) {
    public IndexStatusDto {
        if (integrity == null) {
            throw new IllegalArgumentException("Integrity status is required.");
        }
        if (fileIndexCount < 0 || metadataIndexCount < 0) {
            throw new IllegalArgumentException("Index counts cannot be negative.");
        }
    }
}
