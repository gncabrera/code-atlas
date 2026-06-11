package com.code.atlas.web.service.context.indexed.dto;

import java.util.List;

public record Intent(
        String action,
        List<String> entities,
        List<String> operations,
        List<String> layers,
        boolean frontendImpact
) {
    public Intent {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Intent action is required.");
        }
        entities = entities == null ? List.of() : List.copyOf(entities);
        operations = operations == null ? List.of() : List.copyOf(operations);
        layers = layers == null ? List.of() : List.copyOf(layers);
    }
}
