package com.code.atlas.web.service.dto;

public record PromptOptimizerModeDto(
        Long id,
        String code,
        String name,
        String prompt,
        boolean hidden,
        boolean readOnly
) {
    public PromptOptimizerModeDto {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mode code is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Mode name is required.");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Mode prompt is required.");
        }
    }
}
