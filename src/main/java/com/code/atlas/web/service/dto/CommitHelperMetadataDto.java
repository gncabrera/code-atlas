package com.code.atlas.web.service.dto;

import java.util.List;

public record CommitHelperMetadataDto(
        List<ProjectResponseDto> projects,
        List<AIModelResponseDto> enabledModels
) {
}
