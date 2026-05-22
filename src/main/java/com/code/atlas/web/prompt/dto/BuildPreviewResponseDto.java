package com.code.atlas.web.prompt.dto;

public record BuildPreviewResponseDto(
        String aiModelPrompt,
        int estimatedTokens
) {
}
