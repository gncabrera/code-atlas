package com.code.atlas.web.service.dto;

public record CommitActionRequestDto(
        Long projectId,
        String commitMessage
) {
    public CommitActionRequestDto {
        if (projectId == null) {
            throw new IllegalArgumentException("Project id is required.");
        }
        if (commitMessage == null || commitMessage.isBlank()) {
            throw new IllegalArgumentException("Commit message is required.");
        }
    }
}
