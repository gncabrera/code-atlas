package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.context.indexed.engine.context.builder.FileSummariesIndexService;
import jakarta.transaction.Transactional;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class IndexBuilderService {

    private static final String PHASE = "offline";
    private static final int TOTAL_STEPS = 4;

    private final ContextPipelineLogger pipelineLogger;
    private final IndexBuilderService self;
    private final FileSummariesIndexService fileSummariesIndexService;

    public IndexBuilderService(
            ContextPipelineLogger pipelineLogger,
            @Lazy IndexBuilderService self,
            FileSummariesIndexService fileSummariesIndexService
    ) {
        this.pipelineLogger = pipelineLogger;
        this.self = self;
        this.fileSummariesIndexService = fileSummariesIndexService;
    }

    public void regenerate(Project project, AIModel aiModel) {
        pipelineLogger.message(project, PHASE, "Starting offline index regeneration (full)");
        long started = System.nanoTime();
        try {
            runStep(project, 1, "Purge offline indices", () -> self.purgeOfflineIndices(project.getId()));
            runStep(project, 2, "Generate file metadata", () -> fileSummariesIndexService.regenerateSummaries(project, aiModel, false));
            pipelineLogger.message(project, PHASE, "Offline index regeneration finished (" + ContextPipelineLogger.elapsedMs(started) + " ms)");
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, 0, TOTAL_STEPS, "Offline index regeneration", ContextPipelineLogger.elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    public void regenerateIncremental(Project project, AIModel aiModel) {
        pipelineLogger.message(project, PHASE, "Starting offline incremental index regeneration");
        long started = System.nanoTime();
        try {
            runStep(project, 1, "Generate file summaries (incremental)", () -> fileSummariesIndexService.regenerateSummaries(project, aiModel, true));
            pipelineLogger.message(project, PHASE, "Offline incremental index regeneration finished (" + ContextPipelineLogger.elapsedMs(started) + " ms)");
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, 0, TOTAL_STEPS, "Offline incremental index regeneration", ContextPipelineLogger.elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    private void runStep(Project project, int step, String label, Runnable action) {
        long stepStarted = System.nanoTime();
        pipelineLogger.stepStart(project, PHASE, step, TOTAL_STEPS, label);
        try {
            action.run();
            pipelineLogger.stepComplete(project, PHASE, step, TOTAL_STEPS, label, ContextPipelineLogger.elapsedMs(stepStarted));
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, step, TOTAL_STEPS, label, ContextPipelineLogger.elapsedMs(stepStarted), ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public void purgeOfflineIndices(Long projectId) {
        fileSummariesIndexService.purgeIndices(projectId);
    }

}
