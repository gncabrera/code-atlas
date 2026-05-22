package com.code.atlas.web.project.dto;

public record ProjectRequestDto(
        String path,
        String name,
        String description,
        boolean useAgentsFile
) {
    public ProjectRequestDto {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Project path is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Project description is required.");
        }
    }
}
