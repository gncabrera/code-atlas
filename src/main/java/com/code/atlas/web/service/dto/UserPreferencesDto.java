package com.code.atlas.web.service.dto;

import com.code.atlas.web.service.context.indexed.ContextStrategy;

public record UserPreferencesDto(
        int promptOptimizerDefaultAiModelId,
        int promptOptimizerDefaultPromptModeId,
        ContextStrategy promptOptimizerDefaultContextStrategy,
        int promptOptimizerDefaultContextAiModelId,
        int commitHelperDefaultAiModelId,
        int codeReviewDefaultAiModelId
) {
    public UserPreferencesDto {
        if (promptOptimizerDefaultContextStrategy == null) {
            promptOptimizerDefaultContextStrategy = ContextStrategy.DETERMINISTIC;
        }
    }
}
