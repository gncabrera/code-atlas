package com.code.atlas.web.service.context;

import java.util.List;
import java.util.Set;

public record ContextQuery(
        String rawRequest,
        String endpointMethod,
        String endpointPath,
        Set<String> keywords,
        List<String> focusAreas
) {
    public ContextQuery {
        if (rawRequest == null || rawRequest.isBlank()) {
            throw new IllegalArgumentException("User request is required for context generation.");
        }
        endpointMethod = endpointMethod == null ? "" : endpointMethod.trim().toUpperCase();
        endpointPath = endpointPath == null ? "" : endpointPath.trim();
        keywords = keywords == null ? Set.of() : Set.copyOf(keywords);
        focusAreas = focusAreas == null ? List.of() : List.copyOf(focusAreas);
    }

    public boolean hasEndpoint() {
        return !endpointMethod.isBlank() && !endpointPath.isBlank();
    }
}
