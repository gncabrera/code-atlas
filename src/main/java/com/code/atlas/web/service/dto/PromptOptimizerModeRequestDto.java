package com.code.atlas.web.service.dto;

public record PromptOptimizerModeRequestDto(
        String code,
        String name,
        String prompt,
        boolean hidden
) {
    public PromptOptimizerModeRequestDto {
        if (code != null && code.isBlank()) {
            throw new IllegalArgumentException("Mode code cannot be blank.");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Mode name cannot be blank.");
        }
        if (prompt != null && prompt.isBlank()) {
            throw new IllegalArgumentException("Mode prompt cannot be blank.");
        }
    }
}
