package com.code.atlas.web.aimodel.dto;

public record AIModelRequestDto(
        String name,
        boolean enabled,
        int tokensPerMinute,
        int requestsPerMinute,
        int requestsPerDay,
        String apiKey
) {
    public AIModelRequestDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("AI model name is required.");
        }
        if (tokensPerMinute <= 0) {
            throw new IllegalArgumentException("tokensPerMinute must be greater than zero.");
        }
        if (requestsPerMinute < 0) {
            throw new IllegalArgumentException("requestsPerMinute cannot be negative.");
        }
        if (requestsPerDay < 0) {
            throw new IllegalArgumentException("requestsPerDay cannot be negative.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key is required.");
        }
    }
}
