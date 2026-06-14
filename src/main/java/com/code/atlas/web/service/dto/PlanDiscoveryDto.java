package com.code.atlas.web.service.dto;

import java.util.List;

public record PlanDiscoveryDto(List<PlanQuestionDto> questions, List<PlanSuggestionDto> suggestions) {

    public PlanDiscoveryDto {
        if (questions == null) {
            questions = List.of();
        }
        if (suggestions == null) {
            suggestions = List.of();
        }
    }
}
