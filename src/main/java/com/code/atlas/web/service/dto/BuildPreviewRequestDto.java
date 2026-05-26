package com.code.atlas.web.service.dto;

public record BuildPreviewRequestDto(
        Long projectId,
        String userRequest,
        boolean shouldSendAgentsFile,
        Long promptModeId
) {
    public BuildPreviewRequestDto {
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("User request is required.");
        }
        if (promptModeId == null) {
            throw new IllegalArgumentException("Prompt mode id is required.");
        }
    }
}
