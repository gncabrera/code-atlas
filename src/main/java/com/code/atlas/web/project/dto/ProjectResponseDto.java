package com.code.atlas.web.project.dto;

public record ProjectResponseDto(
        Long id,
        String path,
        String name,
        String description,
        boolean useAgentsFile
) {
}
