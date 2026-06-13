package com.code.atlas.web.service.dto;

public record IndexImportConfirmRequestDto(String preflightId) {
    public IndexImportConfirmRequestDto {
        if (preflightId == null || preflightId.isBlank()) {
            throw new IllegalArgumentException("Preflight id is required.");
        }
    }
}
