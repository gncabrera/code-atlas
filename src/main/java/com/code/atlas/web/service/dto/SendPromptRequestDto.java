package com.code.atlas.web.service.dto;

public record SendPromptRequestDto(
        Long projectId,
        Long aiModelId,
        String aiModelPrompt,
        boolean shouldSendAgentsFile,
        boolean shouldSendDesignFile,
        Long promptModeId
) {
    public SendPromptRequestDto {
        if (aiModelId == null) {
            throw new IllegalArgumentException("AI model id is required.");
        }
        if (aiModelPrompt == null || aiModelPrompt.isBlank()) {
            throw new IllegalArgumentException("AI model prompt is required.");
        }
    }
}
