package com.code.atlas.web.service.dto;

public record BuildPreviewResponseDto(
        String aiModelPrompt,
        int estimatedTokens
) {
}
