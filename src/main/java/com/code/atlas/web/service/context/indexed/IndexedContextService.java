package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.ProjectIndexService;
import com.code.atlas.web.repository.SymbolIndexRepository;
import com.code.atlas.web.service.context.indexed.context.DeterministicRetriever;
import com.code.atlas.web.service.context.indexed.context.GraphExpander;
import com.code.atlas.web.service.context.indexed.context.SecondRetriever;
import com.code.atlas.web.service.context.indexed.indexer.IndexBuildService;
import com.code.atlas.web.service.context.indexed.intent.IntentExtractionService;
import com.code.atlas.web.service.context.indexed.knowledge.ArchitectureSummarizer;
import com.code.atlas.web.service.context.indexed.knowledge.MissingContextDetector;
import com.code.atlas.web.service.context.indexed.prompt.IndexedContextAssembler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IndexedContextService {

    private final ProjectIndexService projectIndexService;
    private final IndexBuildService indexBuildService;
    private final SymbolIndexRepository symbolIndexRepository;
    private final IntentExtractionService intentExtractionService;
    private final DeterministicRetriever deterministicRetriever;
    private final GraphExpander graphExpander;
    private final MissingContextDetector missingContextDetector;
    private final SecondRetriever secondRetriever;
    private final ArchitectureSummarizer architectureSummarizer;
    private final IndexedContextAssembler indexedContextAssembler;

    public IndexedContextService(
            ProjectIndexService projectIndexService,
            IndexBuildService indexBuildService,
            SymbolIndexRepository symbolIndexRepository,
            IntentExtractionService intentExtractionService,
            DeterministicRetriever deterministicRetriever,
            GraphExpander graphExpander,
            MissingContextDetector missingContextDetector,
            SecondRetriever secondRetriever,
            ArchitectureSummarizer architectureSummarizer,
            IndexedContextAssembler indexedContextAssembler
    ) {
        this.projectIndexService = projectIndexService;
        this.indexBuildService = indexBuildService;
        this.symbolIndexRepository = symbolIndexRepository;
        this.intentExtractionService = intentExtractionService;
        this.deterministicRetriever = deterministicRetriever;
        this.graphExpander = graphExpander;
        this.missingContextDetector = missingContextDetector;
        this.secondRetriever = secondRetriever;
        this.architectureSummarizer = architectureSummarizer;
        this.indexedContextAssembler = indexedContextAssembler;
    }

    public String build(Project project, String userRequest, AIModel aiModel) {
        if (project == null) {
            return "## Relevant Files\n\nNo project selected. Context generation skipped.";
        }
        ensureIndicesFresh(project);
        Intent intent = intentExtractionService.extract(project, userRequest, aiModel);
        ContextResult initial = deterministicRetriever.retrieve(project, intent);
        ContextResult expanded = graphExpander.expand(project, initial);
        MissingContext missing = missingContextDetector.detect(project, userRequest, intent, expanded, aiModel);
        List<RetrievedFile> mergedFiles = mergeFiles(expanded.files(), secondRetriever.retrieve(project, intent, missing));
        String architectureFacts = architectureSummarizer.summarize(project, userRequest, intent, mergedFiles, aiModel);
        KnowledgeResult knowledgeResult = new KnowledgeResult(architectureFacts, mergedFiles, expanded.graph());
        return indexedContextAssembler.assemble(intent, knowledgeResult);
    }

    private void ensureIndicesFresh(Project project) {
        if (projectIndexService.isStale(project)) {
            projectIndexService.refreshIndex(project);
            return;
        }
        if (symbolIndexRepository.findByProjectId(project.getId()).isEmpty()) {
            indexBuildService.rebuildProject(project);
        }
    }

    private List<RetrievedFile> mergeFiles(List<RetrievedFile> primary, List<RetrievedFile> secondary) {
        Map<String, RetrievedFile> merged = new LinkedHashMap<>();
        for (RetrievedFile file : primary) {
            merged.put(file.relativePath(), file);
        }
        for (RetrievedFile file : secondary) {
            merged.putIfAbsent(file.relativePath(), file);
        }
        return new ArrayList<>(merged.values());
    }
}
