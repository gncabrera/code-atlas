package com.code.atlas.web.service.context.indexed;

public record GraphEdgeView(String source, String target, String relation) {
    public GraphEdgeView {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Graph edge source is required.");
        }
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Graph edge target is required.");
        }
        if (relation == null || relation.isBlank()) {
            throw new IllegalArgumentException("Graph edge relation is required.");
        }
    }
}
