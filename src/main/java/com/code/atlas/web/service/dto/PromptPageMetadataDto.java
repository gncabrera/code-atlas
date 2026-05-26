package com.code.atlas.web.service.dto;

import java.util.List;

public record PromptPageMetadataDto(
        List<ProjectResponseDto> projects,
        List<AIModelResponseDto> enabledModels,
        List<PromptOptimizerModeDto> promptModes
) {
}
