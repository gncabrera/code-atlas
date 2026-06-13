package com.code.atlas.web.service.dto;

import java.util.List;

public record ImportPreflightDto(
        String preflightId,
        int totalRecords,
        int overwriteCount,
        int newPathCount,
        int missingOnDiskCount,
        List<String> warnings
) {
    public ImportPreflightDto {
        if (preflightId == null || preflightId.isBlank()) {
            throw new IllegalArgumentException("Preflight id is required.");
        }
        if (totalRecords < 0 || overwriteCount < 0 || newPathCount < 0 || missingOnDiskCount < 0) {
            throw new IllegalArgumentException("Preflight counts cannot be negative.");
        }
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
