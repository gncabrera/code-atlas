package com.code.atlas.web.service.dto;

public record OfflineIndexRequestDto(Long aiModelId) {
    public OfflineIndexRequestDto {
        if (aiModelId == null) {
            throw new IllegalArgumentException("AI model id is required.");
        }
    }
}
