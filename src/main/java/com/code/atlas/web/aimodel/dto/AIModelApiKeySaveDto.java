package com.code.atlas.web.aimodel.dto;

public record AIModelApiKeySaveDto(
        String name,
        String apiKey,
        String provider,
        boolean isActive
) {
    public AIModelApiKeySaveDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("API key name is required.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key value is required.");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Provider is required.");
        }
    }
}
