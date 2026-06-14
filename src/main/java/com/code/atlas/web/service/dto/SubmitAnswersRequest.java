package com.code.atlas.web.service.dto;

import java.util.List;
import java.util.Map;

public record SubmitAnswersRequest(Map<String, String> answers, List<String> selectedSuggestions) {

    public SubmitAnswersRequest {
        if (answers == null) {
            answers = Map.of();
        }
        if (selectedSuggestions == null) {
            selectedSuggestions = List.of();
        }
    }
}
