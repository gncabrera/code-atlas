package com.code.atlas.web.service.context.indexed.context;

import com.code.atlas.web.domain.GraphEdgeEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.SymbolIndexEntry;
import com.code.atlas.web.repository.GraphEdgeRepository;
import com.code.atlas.web.repository.SymbolIndexRepository;
import com.code.atlas.web.service.context.indexed.ContextResult;
import com.code.atlas.web.service.context.indexed.GraphEdgeView;
import com.code.atlas.web.service.context.indexed.IndexedFileLoader;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GraphExpander {

    private final GraphEdgeRepository graphEdgeRepository;
    private final SymbolIndexRepository symbolIndexRepository;
    private final IndexedFileLoader indexedFileLoader;
    private final int maxGraphDepth;
    private final int maxFiles;

    public GraphExpander(
            GraphEdgeRepository graphEdgeRepository,
            SymbolIndexRepository symbolIndexRepository,
            IndexedFileLoader indexedFileLoader,
            @Value("${codeatlas.context.indexed.max-graph-depth:3}") int maxGraphDepth,
            @Value("${codeatlas.context.indexed.max-files:16}") int maxFiles
    ) {
        this.graphEdgeRepository = graphEdgeRepository;
        this.symbolIndexRepository = symbolIndexRepository;
        this.indexedFileLoader = indexedFileLoader;
        this.maxGraphDepth = Math.max(1, maxGraphDepth);
        this.maxFiles = Math.max(1, maxFiles);
    }

    public ContextResult expand(Project project, ContextResult seed) {
        Map<String, RetrievedFile> filesByPath = new LinkedHashMap<>();
        for (RetrievedFile file : seed.files()) {
            filesByPath.put(file.relativePath(), file);
        }
        Set<GraphEdgeView> graphEdges = new LinkedHashSet<>(seed.graph());
        Set<String> visitedSymbols = new LinkedHashSet<>();
        Queue<SymbolDepth> queue = new ArrayDeque<>();

        for (RetrievedFile file : seed.files()) {
            for (String symbol : file.symbols()) {
                enqueueSymbol(queue, visitedSymbols, symbol, 0);
            }
        }

        while (!queue.isEmpty() && filesByPath.size() < maxFiles) {
            SymbolDepth current = queue.poll();
            if (current.depth() >= maxGraphDepth) {
                continue;
            }
            for (GraphEdgeEntry edge : graphEdgeRepository.findByProjectIdAndSource(project.getId(), current.symbol())) {
                graphEdges.add(new GraphEdgeView(edge.getSource(), edge.getTarget(), edge.getRelation()));
                resolveSymbolFile(project, edge.getTarget(), filesByPath);
                enqueueSymbol(queue, visitedSymbols, edge.getTarget(), current.depth() + 1);
            }
            for (GraphEdgeEntry edge : graphEdgeRepository.findByProjectIdAndTarget(project.getId(), current.symbol())) {
                graphEdges.add(new GraphEdgeView(edge.getSource(), edge.getTarget(), edge.getRelation()));
                resolveSymbolFile(project, edge.getSource(), filesByPath);
                enqueueSymbol(queue, visitedSymbols, edge.getSource(), current.depth() + 1);
            }
        }

        return new ContextResult(List.copyOf(filesByPath.values()), List.copyOf(graphEdges));
    }

    private void resolveSymbolFile(Project project, String symbol, Map<String, RetrievedFile> filesByPath) {
        for (SymbolIndexEntry entry : symbolIndexRepository.findByProjectIdAndSymbolIgnoreCase(project.getId(), symbol)) {
            if (!filesByPath.containsKey(entry.getFilePath())) {
                filesByPath.put(
                        entry.getFilePath(),
                        indexedFileLoader.load(
                                project,
                                entry.getFilePath(),
                                50,
                                List.of("Graph expansion via " + symbol)
                        )
                );
            }
        }
    }

    private void enqueueSymbol(Queue<SymbolDepth> queue, Set<String> visited, String symbol, int depth) {
        if (symbol == null || symbol.isBlank() || visited.contains(symbol)) {
            return;
        }
        visited.add(symbol);
        queue.add(new SymbolDepth(symbol, depth));
    }

    private record SymbolDepth(String symbol, int depth) {
    }
}
