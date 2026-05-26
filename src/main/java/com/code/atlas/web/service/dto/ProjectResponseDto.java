package com.code.atlas.web.service.dto;

public record ProjectResponseDto(
        Long id,
        String path,
        String name,
        String description,
        boolean useAgentsFile,
        boolean useDesignFile
) {
}
