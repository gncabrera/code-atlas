package com.code.atlas.web.service.dto;

import java.util.List;

public record PlanQuestionDto(String id, String question, List<PlanQuestionOptionDto> options) {

    public PlanQuestionDto {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Question id is required.");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question text is required.");
        }
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Question options are required.");
        }
    }
}
