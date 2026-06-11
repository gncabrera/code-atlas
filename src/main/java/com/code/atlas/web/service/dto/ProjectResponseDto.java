package com.code.atlas.web.service.dto;

import java.util.List;

public record ProjectResponseDto(
        Long id,
        String path,
        String name,
        String description,
        boolean useAgentsFile,
        boolean useDesignFile,
        List<String> indexerProfiles
) {
}
