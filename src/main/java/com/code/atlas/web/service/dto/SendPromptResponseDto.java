package com.code.atlas.web.service.dto;

public record SendPromptResponseDto(
        String outputPrompt,
        int estimatedTokens
) {
}
