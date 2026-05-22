package com.code.atlas.web.prompt.dto;

import com.code.atlas.web.aimodel.dto.AIModelResponseDto;
import com.code.atlas.web.project.dto.ProjectResponseDto;
import java.util.List;

public record PromptPageMetadataDto(
        List<ProjectResponseDto> projects,
        List<AIModelResponseDto> enabledModels
) {
}
