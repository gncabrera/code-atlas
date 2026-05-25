package com.code.atlas.web.service.dto;

import java.time.LocalDateTime;

public record PromptHistoryResponseDto(
        Long id,
        Long projectId,
        String projectName,
        Long aiModelId,
        String aiModelName,
        String mode,
        boolean shouldSendAgentsFile,
        int estimatedTokens,
        String requestPrompt,
        String responsePrompt,
        String status,
        String errorMessage,
        LocalDateTime createdAt
) {
    public PromptHistoryResponseDto {
        if (id == null) {
            throw new IllegalArgumentException("id is required.");
        }
        if (aiModelId == null) {
            throw new IllegalArgumentException("aiModelId is required.");
        }
        if (aiModelName == null || aiModelName.isBlank()) {
            throw new IllegalArgumentException("aiModelName is required.");
        }
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("mode is required.");
        }
        if (requestPrompt == null) {
            throw new IllegalArgumentException("requestPrompt is required.");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required.");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required.");
        }
    }
}
