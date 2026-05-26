package com.code.atlas.web.service.dto;

public record UserPreferencesDto(
        int promptOptimizerDefaultAiModelId,
        int promptOptimizerDefaultPromptModeId,
        int commitHelperDefaultAiModelId,
        int codeReviewDefaultAiModelId
) {
}
