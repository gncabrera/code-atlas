package com.code.atlas.web.service.context.indexed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntentExtractionResponse(
        String action,
        List<String> symbols,
        List<String> concepts,
        List<String> capabilities,
        List<String> architecturalRoles,
        List<String> changeImpactAreas,
        boolean frontendImpact,
        double confidence
) {
}
