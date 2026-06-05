package com.code.atlas.web.service.context.indexed;

import java.util.List;

public record KnowledgeResult(
        String architectureFacts,
        List<RetrievedFile> files,
        List<GraphEdgeView> graph
) {
    public KnowledgeResult {
        architectureFacts = architectureFacts == null ? "" : architectureFacts;
        files = files == null ? List.of() : List.copyOf(files);
        graph = graph == null ? List.of() : List.copyOf(graph);
    }
}
