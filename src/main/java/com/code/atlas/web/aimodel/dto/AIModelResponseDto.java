package com.code.atlas.web.aimodel.dto;

public record AIModelResponseDto(
        Long id,
        String name,
        String description,
        boolean enabled,
        int tokensPerMinute,
        int requestsPerMinute,
        int requestsPerDay,
        String apiKey
) {
}
