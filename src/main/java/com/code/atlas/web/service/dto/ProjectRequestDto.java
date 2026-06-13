package com.code.atlas.web.service.dto;

import java.util.List;

public record ProjectRequestDto(
        String path,
        String name,
        String description,
        boolean useAgentsFile,
        boolean useDesignFile,
        List<Long> projectTypeIds
) {
    public ProjectRequestDto {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Project path is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }
        if (projectTypeIds == null || projectTypeIds.isEmpty()) {
            throw new IllegalArgumentException("At least one project type is required.");
        }
    }
}
