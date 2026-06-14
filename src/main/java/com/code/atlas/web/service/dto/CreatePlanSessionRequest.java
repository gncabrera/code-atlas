package com.code.atlas.web.service.dto;

public record CreatePlanSessionRequest(
        String userRequest,
        String outputType,
        Long contextModelId,
        Long planModelId,
        Long projectId
) {

    public CreatePlanSessionRequest {
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("User request is required.");
        }
        if (outputType == null || outputType.isBlank()) {
            throw new IllegalArgumentException("Output type is required.");
        }
        if (contextModelId == null) {
            throw new IllegalArgumentException("Context model is required.");
        }
        if (planModelId == null) {
            throw new IllegalArgumentException("Plan model is required.");
        }
    }
}
