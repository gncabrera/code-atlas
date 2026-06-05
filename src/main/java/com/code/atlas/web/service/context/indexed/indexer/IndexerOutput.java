package com.code.atlas.web.service.context.indexed.indexer;

import java.util.List;

public record IndexerOutput(
        List<SymbolRow> symbols,
        List<EndpointRow> endpoints,
        List<GraphEdgeRow> graphEdges,
        List<DatabaseRow> databaseRows,
        List<FrontendRow> frontendRows
) {
    public IndexerOutput {
        symbols = symbols == null ? List.of() : List.copyOf(symbols);
        endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
        graphEdges = graphEdges == null ? List.of() : List.copyOf(graphEdges);
        databaseRows = databaseRows == null ? List.of() : List.copyOf(databaseRows);
        frontendRows = frontendRows == null ? List.of() : List.copyOf(frontendRows);
    }

    public static IndexerOutput empty() {
        return new IndexerOutput(List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
