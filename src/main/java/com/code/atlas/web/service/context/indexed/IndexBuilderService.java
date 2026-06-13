package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.ProjectService;
import com.code.atlas.web.service.context.indexed.engine.context.builder.FileSummariesIndexService;
import com.code.atlas.web.service.dto.OfflineIndexJobMode;
import com.code.atlas.web.service.dto.OfflineIndexJobResponseDto;
import com.code.atlas.web.service.dto.OfflineIndexJobStatus;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class IndexBuilderService {

    private static final String PHASE = "offline";
    private static final int TOTAL_STEPS = 4;

    private final ContextPipelineLogger pipelineLogger;
    private final IndexBuilderService self;
    private final FileSummariesIndexService fileSummariesIndexService;
    private final ProjectService projectService;
    private final AIModelService aiModelService;
    private final ConcurrentHashMap<Long, OfflineIndexJobResponseDto> jobsByProjectId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Object> projectLocks = new ConcurrentHashMap<>();

    public IndexBuilderService(
            ContextPipelineLogger pipelineLogger,
            @Lazy IndexBuilderService self,
            FileSummariesIndexService fileSummariesIndexService,
            ProjectService projectService,
            AIModelService aiModelService
    ) {
        this.pipelineLogger = pipelineLogger;
        this.self = self;
        this.fileSummariesIndexService = fileSummariesIndexService;
        this.projectService = projectService;
        this.aiModelService = aiModelService;
    }

    public OfflineIndexJobResponseDto startRegenerateAsync(Long projectId, Long aiModelId) {
        return startJob(projectId, aiModelId, OfflineIndexJobMode.FULL);
    }

    public OfflineIndexJobResponseDto startRegenerateIncrementalAsync(Long projectId, Long aiModelId) {
        return startJob(projectId, aiModelId, OfflineIndexJobMode.INCREMENTAL);
    }

    public OfflineIndexJobResponseDto getJobStatus(Long projectId) {
        return jobsByProjectId.get(projectId);
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

    @Async("offlineIndexTaskExecutor")
    public void executeRegenerateAsync(Long projectId, Long aiModelId, OfflineIndexJobMode mode) {
        try {
            Project project = projectService.getProjectEntity(projectId);
            AIModel aiModel = aiModelService.getModelEntity(aiModelId);
            if (mode == OfflineIndexJobMode.FULL) {
                regenerate(project, aiModel);
            } else {
                regenerateIncremental(project, aiModel);
            }
            markJobCompleted(projectId);
        } catch (RuntimeException ex) {
            markJobFailed(projectId, ex.getMessage());
        }
    }

    private OfflineIndexJobResponseDto startJob(Long projectId, Long aiModelId, OfflineIndexJobMode mode) {
        synchronized (lockFor(projectId)) {
            OfflineIndexJobResponseDto existing = jobsByProjectId.get(projectId);
            if (existing != null && existing.status() == OfflineIndexJobStatus.RUNNING) {
                throw new IllegalArgumentException("Offline index job already running for this project.");
            }
            OfflineIndexJobResponseDto job = new OfflineIndexJobResponseDto(
                    projectId,
                    mode,
                    OfflineIndexJobStatus.RUNNING,
                    LocalDateTime.now(),
                    null,
                    null
            );
            jobsByProjectId.put(projectId, job);
            self.executeRegenerateAsync(projectId, aiModelId, mode);
            return job;
        }
    }

    private void markJobCompleted(Long projectId) {
        synchronized (lockFor(projectId)) {
            OfflineIndexJobResponseDto current = jobsByProjectId.get(projectId);
            if (current == null) {
                return;
            }
            jobsByProjectId.put(projectId, new OfflineIndexJobResponseDto(
                    current.projectId(),
                    current.mode(),
                    OfflineIndexJobStatus.COMPLETED,
                    current.startedAt(),
                    LocalDateTime.now(),
                    null
            ));
        }
    }

    private void markJobFailed(Long projectId, String errorMessage) {
        synchronized (lockFor(projectId)) {
            OfflineIndexJobResponseDto current = jobsByProjectId.get(projectId);
            if (current == null) {
                return;
            }
            jobsByProjectId.put(projectId, new OfflineIndexJobResponseDto(
                    current.projectId(),
                    current.mode(),
                    OfflineIndexJobStatus.FAILED,
                    current.startedAt(),
                    LocalDateTime.now(),
                    errorMessage
            ));
        }
    }

    private Object lockFor(Long projectId) {
        return projectLocks.computeIfAbsent(projectId, ignored -> new Object());
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
