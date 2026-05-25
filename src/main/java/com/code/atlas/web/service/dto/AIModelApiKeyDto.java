package com.code.atlas.web.service.dto;

public record AIModelApiKeyDto(
        Long id,
        String name,
        String apiKey,
        String provider,
        boolean isActive
) {
}
