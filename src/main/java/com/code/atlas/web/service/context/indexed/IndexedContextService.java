package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.ProjectIndexService;
import com.code.atlas.web.service.context.indexed.dto.*;
import com.code.atlas.web.service.context.indexed.engine.context.ContextEngine;
import com.code.atlas.web.service.context.indexed.engine.intent.IntentEngine;
import com.code.atlas.web.service.context.indexed.engine.knowledge.KnowlegeEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.code.atlas.web.service.context.indexed.engine.prompt.PromptEngine;
import org.springframework.stereotype.Service;

@Service
public class IndexedContextService {

    private static final String PHASE = "build";
    private static final int TOTAL_STEPS = 8;

    private final ProjectIndexService projectIndexService;
    private final ContextPipelineLogger pipelineLogger;

    private final ContextEngine contextEngine;
    private final IntentEngine intentEngine;
    private final KnowlegeEngine knowlegeEngine;
    private final PromptEngine promptEngine;

    public IndexedContextService(
            ProjectIndexService projectIndexService,
            ContextPipelineLogger pipelineLogger,
            ContextEngine contextEngine,
            IntentEngine intentEngine,
            KnowlegeEngine knowlegeEngine,
            PromptEngine promptEngine
    ) {
        this.projectIndexService = projectIndexService;
        this.pipelineLogger = pipelineLogger;
        this.contextEngine = contextEngine;
        this.intentEngine = intentEngine;
        this.knowlegeEngine = knowlegeEngine;
        this.promptEngine = promptEngine;
    }

    public String build(Project project, String userRequest, AIModel aiModel) {
        if (project == null) {
            return "## Relevant Files\n\nNo project selected. Context generation skipped.";
        }
        pipelineLogger.message(project, PHASE, "Starting indexed context build");
        long buildStarted = System.nanoTime();
        try {
            runVoidStep(project, 1, "Ensure indices are fresh", () -> ensureIndicesFresh(project));
            Intent intent = runStep(project, 2, "Extract intent", () -> intentEngine.extract(project, userRequest, aiModel));
            ContextResult contextResult = runStep(project, 3, "Deterministic retrieval", () -> contextEngine.retrieve(project, intent));
            Intent missingIntent = runStep(project, 4, "Detect missing context",
                    () -> knowlegeEngine.detectMissingContext(project, userRequest, intent, contextResult, aiModel));
            ContextResult missingContext = runStep(project, 5, "Second deterministic retrieval",
                    () -> contextEngine.retrieve(project, missingIntent));
            List<RetrievedFile> mergedFiles = runStep(project, 6, "Merge second retrieval files",
                    () -> mergeFiles(contextResult.files(), missingContext.files()));
            String architectureFacts = runStep(project, 7, "Summarize architecture",
                    () -> knowlegeEngine.summarize(project, userRequest, intent, mergedFiles, aiModel));
            KnowledgeResult knowledgeResult = new KnowledgeResult(architectureFacts, mergedFiles);
            String assembled = runStep(project, 8, "Assemble context",
                    () -> promptEngine.assemble(intent, knowledgeResult));
            pipelineLogger.message(project, PHASE, "Indexed context build finished (" + ContextPipelineLogger.elapsedMs(buildStarted) + " ms)");
            return assembled;
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, 0, TOTAL_STEPS, "Indexed context build", ContextPipelineLogger.elapsedMs(buildStarted), ex.getMessage());
            throw ex;
        }
    }

    private void ensureIndicesFresh(Project project) {
        if (projectIndexService.isStale(project)) {
            projectIndexService.refreshIndex(project, PHASE);
        }
    }

    private void runVoidStep(Project project, int step, String label, Runnable action) {
        long started = System.nanoTime();
        pipelineLogger.stepStart(project, PHASE, step, TOTAL_STEPS, label);
        try {
            action.run();
            pipelineLogger.stepComplete(project, PHASE, step, TOTAL_STEPS, label, ContextPipelineLogger.elapsedMs(started));
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, step, TOTAL_STEPS, label, ContextPipelineLogger.elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    private <T> T runStep(Project project, int step, String label, StepAction<T> action) {
        long started = System.nanoTime();
        pipelineLogger.stepStart(project, PHASE, step, TOTAL_STEPS, label);
        try {
            T result = action.run();
            pipelineLogger.stepComplete(project, PHASE, step, TOTAL_STEPS, label, ContextPipelineLogger.elapsedMs(started));
            return result;
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, step, TOTAL_STEPS, label, ContextPipelineLogger.elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    private List<RetrievedFile> mergeFiles(List<RetrievedFile> primary, List<RetrievedFile> secondary) {
        Map<String, RetrievedFile> merged = new LinkedHashMap<>();
        for (RetrievedFile file : primary) {
            merged.put(file.file().getFilePath(), file);
        }
        for (RetrievedFile file : secondary) {
            merged.putIfAbsent(file.file().getFilePath(), file);
        }
        return new ArrayList<>(merged.values());
    }

    @FunctionalInterface
    private interface StepAction<T> {
        T run();
    }
}
