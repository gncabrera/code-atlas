package com.code.atlas.web.service.context.indexed;

import java.util.List;

public record ContextResult(List<RetrievedFile> files, List<GraphEdgeView> graph) {
    public ContextResult {
        files = files == null ? List.of() : List.copyOf(files);
        graph = graph == null ? List.of() : List.copyOf(graph);
    }
}
