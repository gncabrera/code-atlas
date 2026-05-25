package com.code.atlas.web.service.dto;

public record GenerateCommitRequestDto(
        Long projectId,
        Long aiModelId
) {
    public GenerateCommitRequestDto {
        if (projectId == null) {
            throw new IllegalArgumentException("Project id is required.");
        }
        if (aiModelId == null) {
            throw new IllegalArgumentException("AI model id is required.");
        }
    }
}
