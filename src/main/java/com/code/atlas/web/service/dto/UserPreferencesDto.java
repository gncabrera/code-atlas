package com.code.atlas.web.service.dto;

public record UserPreferencesDto(
        int promptOptimizerDefaultAiModelId,
        int promptOptimizerDefaultPromptModeId,
        String promptOptimizerDefaultContextStrategy,
        int promptOptimizerDefaultContextAiModelId,
        int commitHelperDefaultAiModelId,
        int codeReviewDefaultAiModelId
) {
    public UserPreferencesDto {
        if (promptOptimizerDefaultContextStrategy == null || promptOptimizerDefaultContextStrategy.isBlank()) {
            promptOptimizerDefaultContextStrategy = "deterministic";
        } else {
            promptOptimizerDefaultContextStrategy = promptOptimizerDefaultContextStrategy.trim().toLowerCase();
        }
    }
}
