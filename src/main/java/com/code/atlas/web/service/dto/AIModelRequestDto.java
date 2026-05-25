package com.code.atlas.web.service.dto;

public record AIModelRequestDto(
        String name,
        String description,
        boolean enabled,
        int tokensPerMinute,
        int requestsPerMinute,
        int requestsPerDay,
        AIModelApiKeyDto apiKey
) {
    public AIModelRequestDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("AI model name is required.");
        }
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters.");
        }
        if (tokensPerMinute < 0) {
            throw new IllegalArgumentException("tokensPerMinute cannot be negative.");
        }
        if (requestsPerMinute < 0) {
            throw new IllegalArgumentException("requestsPerMinute cannot be negative.");
        }
        if (requestsPerDay < 0) {
            throw new IllegalArgumentException("requestsPerDay cannot be negative.");
        }
    }
}
