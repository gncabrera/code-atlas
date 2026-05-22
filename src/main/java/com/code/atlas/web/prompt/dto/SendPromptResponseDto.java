package com.code.atlas.web.prompt.dto;

public record SendPromptResponseDto(
        String outputPrompt,
        int estimatedTokens
) {
}
