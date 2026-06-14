package com.code.atlas.web.service.dto;

public record PlanQuestionOptionDto(String id, String text, boolean isDefault) {

    public PlanQuestionOptionDto {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Question option id is required.");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Question option text is required.");
        }
    }
}
