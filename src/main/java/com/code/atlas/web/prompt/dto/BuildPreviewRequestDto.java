package com.code.atlas.web.prompt.dto;

public record BuildPreviewRequestDto(
        Long projectId,
        String userRequest,
        boolean shouldSendAgentsFile,
        String promptMode
) {
    public BuildPreviewRequestDto {
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("User request is required.");
        }
    }
}
