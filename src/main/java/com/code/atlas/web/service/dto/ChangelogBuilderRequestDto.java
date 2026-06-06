package com.code.atlas.web.service.dto;

import java.util.List;

public record ChangelogBuilderRequestDto(
        Long projectId,
        Long modelId,
        List<String> commitHashes,
        ChangelogExportFormat exportFormat
) {
    public ChangelogBuilderRequestDto {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required.");
        }
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID is required.");
        }
        if (commitHashes == null || commitHashes.isEmpty()) {
            throw new IllegalArgumentException("At least one commit must be selected.");
        }
        if (exportFormat == null) {
            exportFormat = ChangelogExportFormat.MARKDOWN;
        }
    }
}
