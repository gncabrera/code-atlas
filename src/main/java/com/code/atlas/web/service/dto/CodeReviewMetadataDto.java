package com.code.atlas.web.service.dto;

import java.util.List;

public record CodeReviewMetadataDto(
        List<ProjectResponseDto> projects,
        List<AIModelResponseDto> enabledModels,
        List<String> branches
) {
}
