package com.code.atlas.web.aimodel.dto;

public record AIModelResponseDto(
        Long id,
        String name,
        boolean enabled,
        int tokensPerMinute,
        int requestsPerMinute,
        int requestsPerDay,
        String apiKey
) {
}
