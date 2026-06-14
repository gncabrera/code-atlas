package com.code.atlas.web.service.dto;

public record PlanSuggestionDto(
        String title,
        String change,
        String reason,
        String impact,
        String effort,
        String priority,
        String category
) {

    public PlanSuggestionDto {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Suggestion title is required.");
        }
    }
}
