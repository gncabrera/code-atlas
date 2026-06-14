package com.code.atlas.web.service.dto;

public record RefinePlanRequest(String userMessage) {

    public RefinePlanRequest {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("User message is required.");
        }
    }
}
