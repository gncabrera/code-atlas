package com.code.atlas.web.service.dto;

import java.util.List;

public record ProjectRequestDto(
        String path,
        String name,
        String description,
        boolean useAgentsFile,
        boolean useDesignFile,
        List<String> indexerProfiles
) {
    public ProjectRequestDto {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Project path is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }
        indexerProfiles = indexerProfiles == null ? List.of() : List.copyOf(indexerProfiles);
    }
}
