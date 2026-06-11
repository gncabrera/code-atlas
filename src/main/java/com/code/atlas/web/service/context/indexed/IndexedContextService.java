package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.ProjectIndexService;
import com.code.atlas.web.service.context.indexed.dto.*;
import com.code.atlas.web.service.context.indexed.step.s02_retrieval.DeterministicRetriever;
import com.code.atlas.web.service.context.indexed.step.s01_intent.IntentExtractionService;
import com.code.atlas.web.service.context.indexed.step.s04_architecture.ArchitectureSummarizer;
import com.code.atlas.web.service.context.indexed.step.s03_missing_context.MissingContextDetector;
import com.code.atlas.web.service.context.indexed.step.s05_assembler.IndexedContextAssembler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IndexedContextService {

    private static final String PHASE = "build";
    private static final int TOTAL_STEPS = 8;

    private final ProjectIndexService projectIndexService;
    private final IntentExtractionService intentExtractionService;
    private final DeterministicRetriever deterministicRetriever;
    private final MissingContextDetector missingContextDetector;
    private final ArchitectureSummarizer architectureSummarizer;
    private final IndexedContextAssembler indexedContextAssembler;
    private final ContextPipelineLogger pipelineLogger;

    public IndexedContextService(
            ProjectIndexService projectIndexService,
            IntentExtractionService intentExtractionService,
            DeterministicRetriever deterministicRetriever,
            MissingContextDetector missingContextDetector,
            ArchitectureSummarizer architectureSummarizer,
            IndexedContextAssembler indexedContextAssembler,
            ContextPipelineLogger pipelineLogger
    ) {
        this.projectIndexService = projectIndexService;
        this.intentExtractionService = intentExtractionService;
        this.deterministicRetriever = deterministicRetriever;
        this.missingContextDetector = missingContextDetector;
        this.architectureSummarizer = architectureSummarizer;
        this.indexedContextAssembler = indexedContextAssembler;
        this.pipelineLogger = pipelineLogger;
    }

    public String build(Project project, String userRequest, AIModel aiModel) {
        if (project == null) {
            return "## Relevant Files\n\nNo project selected. Context generation skipped.";
        }
        pipelineLogger.message(project, PHASE, "Starting indexed context build");
        long buildStarted = System.nanoTime();
        try {
            runVoidStep(project, 1, "Ensure indices are fresh", () -> ensureIndicesFresh(project));
            Intent intent = runStep(project, 2, "Extract intent", () -> intentExtractionService.extract(project, userRequest, aiModel));
            ContextResult contextResult = runStep(project, 3, "Deterministic retrieval", () -> deterministicRetriever.retrieve(project, intent));
            Intent missingIntent = runStep(project, 5, "Detect missingIntent context",
                    () -> missingContextDetector.detect(project, userRequest, intent, contextResult, aiModel));
            ContextResult missingContext = deterministicRetriever.retrieve(project, missingIntent);
            List<RetrievedFile> mergedFiles = runStep(project, 6, "Second retrieval and merge",
                    () -> mergeFiles(contextResult.files(), missingContext.files()));
            String architectureFacts = runStep(project, 7, "Summarize architecture",
                    () -> architectureSummarizer.summarize(project, userRequest, intent, mergedFiles, aiModel));
            KnowledgeResult knowledgeResult = new KnowledgeResult(architectureFacts, mergedFiles);
            String assembled = runStep(project, 8, "Assemble context",
                    () -> indexedContextAssembler.assemble(intent, knowledgeResult));
            pipelineLogger.message(project, PHASE, "Indexed context build finished (" + elapsedMs(buildStarted) + " ms)");
            return assembled;
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, 0, TOTAL_STEPS, "Indexed context build", elapsedMs(buildStarted), ex.getMessage());
            throw ex;
        }
    }

    private void ensureIndicesFresh(Project project) {
        if (projectIndexService.isStale(project)) {
            projectIndexService.refreshIndex(project, PHASE);
            return;
        }
        // TODO: Ensure metadata is fresh
    }

    private void runVoidStep(Project project, int step, String label, Runnable action) {
        long started = System.nanoTime();
        pipelineLogger.stepStart(project, PHASE, step, TOTAL_STEPS, label);
        try {
            action.run();
            pipelineLogger.stepComplete(project, PHASE, step, TOTAL_STEPS, label, elapsedMs(started));
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, step, TOTAL_STEPS, label, elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    private <T> T runStep(Project project, int step, String label, StepAction<T> action) {
        long started = System.nanoTime();
        pipelineLogger.stepStart(project, PHASE, step, TOTAL_STEPS, label);
        try {
            T result = action.run();
            pipelineLogger.stepComplete(project, PHASE, step, TOTAL_STEPS, label, elapsedMs(started));
            return result;
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, step, TOTAL_STEPS, label, elapsedMs(started), ex.getMessage());
            throw ex;
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

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    @FunctionalInterface
    private interface StepAction<T> {
        T run();
    }
}
