package com.code.atlas.web.service.context.indexed.dto;

import java.util.List;

public record Intent(
        String action,
        List<String> symbols,
        List<String> concepts,
        List<String> capabilities,
        List<String> architecturalRoles,
        List<String> changeImpactAreas,
        boolean frontendImpact,
        double confidence
) {
    public Intent {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Intent action is required.");
        }
        symbols = symbols == null ? List.of() : List.copyOf(symbols);
        concepts = concepts == null ? List.of() : List.copyOf(concepts);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        architecturalRoles = architecturalRoles == null ? List.of() : List.copyOf(architecturalRoles);
        changeImpactAreas = changeImpactAreas == null ? List.of() : List.copyOf(changeImpactAreas);
    }
}
