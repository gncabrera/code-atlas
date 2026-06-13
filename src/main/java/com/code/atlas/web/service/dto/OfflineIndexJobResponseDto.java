package com.code.atlas.web.service.dto;

import java.time.LocalDateTime;

public record OfflineIndexJobResponseDto(
        Long projectId,
        OfflineIndexJobMode mode,
        OfflineIndexJobStatus status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String errorMessage
) {
    public OfflineIndexJobResponseDto {
        if (projectId == null) {
            throw new IllegalArgumentException("Project id is required.");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Offline index job mode is required.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Offline index job status is required.");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("Offline index job startedAt is required.");
        }
    }
}
