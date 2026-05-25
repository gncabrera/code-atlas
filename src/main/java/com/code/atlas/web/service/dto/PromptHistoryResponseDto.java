package com.code.atlas.web.service.dto;

import com.code.atlas.web.domain.PromptStatus;

import java.time.LocalDateTime;

public record PromptHistoryResponseDto(
        Long id,
        Long projectId,
        String projectName,
        Long aiModelId,
        String aiModelName,
        String notes,
        int estimatedTokens,
        String requestPrompt,
        String responsePrompt,
        PromptStatus status,
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
        if (requestPrompt == null) {
            throw new IllegalArgumentException("requestPrompt is required.");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required.");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required.");
        }
    }
}
