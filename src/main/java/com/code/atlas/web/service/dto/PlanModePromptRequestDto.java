package com.code.atlas.web.service.dto;

public record PlanModePromptRequestDto(String prompt) {

    public PlanModePromptRequestDto {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt is required.");
        }
    }
}
