package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.ProjectFileMetadataIndex;
import com.code.atlas.web.repository.ProjectFileIndexRepository;
import com.code.atlas.web.repository.ProjectFileMetadataIndexRepository;
import com.code.atlas.web.service.*;
import com.code.atlas.web.service.context.indexed.builder.OfflineFileSummaryChunkBuilder;
import com.code.atlas.web.service.context.indexed.dto.FileSummaryOfflineResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class IndexBuilderService {

    private static final String PHASE = "offline";
    private static final int TOTAL_STEPS = 4;
    private static final String NOTES = "Indexed context: offline index generation";

    private final ProjectFileIndexRepository projectFileIndexRepository;
    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final ObjectMapper objectMapper;
    private final ContextPipelineLogger pipelineLogger;
    private final IndexBuilderService self;
    private final String summaryTemplate;
    private final OfflineFileSummaryChunkBuilder fileSummaryChunkBuilder;
    private final ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository;
    private final ProjectService projectService;

    public IndexBuilderService(
            ProjectFileIndexRepository projectFileIndexRepository,
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            ObjectMapper objectMapper,
            ContextPipelineLogger pipelineLogger,
            OfflineFileSummaryChunkBuilder fileSummaryChunkBuilder,
            ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository,
            ProjectService projectService,
            @Lazy IndexBuilderService self
    ) {
        this.projectFileIndexRepository = projectFileIndexRepository;
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.objectMapper = objectMapper;
        this.pipelineLogger = pipelineLogger;
        this.fileSummaryChunkBuilder = fileSummaryChunkBuilder;
        this.projectFileMetadataIndexRepository = projectFileMetadataIndexRepository;
        this.projectService = projectService;
        this.self = self;
        this.summaryTemplate = PromptTemplateService.load(PromptTemplate.CONTEXT_FILE_SUMMARY);
    }

    public void regenerate(Project project, AIModel aiModel) {
        pipelineLogger.message(project, PHASE, "Starting offline index regeneration (full)");
        long started = System.nanoTime();
        try {
            runStep(project, 1, "Purge offline indices", () -> self.purgeOfflineIndices(project.getId()));
            runStep(project, 2, "Generate file summaries", () -> regenerateSummaries(project, aiModel, false));
            pipelineLogger.message(project, PHASE, "Offline index regeneration finished (" + elapsedMs(started) + " ms)");
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, 0, TOTAL_STEPS, "Offline index regeneration", elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    public void regenerateIncremental(Project project, AIModel aiModel) {
        pipelineLogger.message(project, PHASE, "Starting offline incremental index regeneration");
        long started = System.nanoTime();
        try {
            runStep(project, 1, "Generate file summaries (incremental)", () -> regenerateSummaries(project, aiModel, true));
            pipelineLogger.message(project, PHASE, "Offline incremental index regeneration finished (" + elapsedMs(started) + " ms)");
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, 0, TOTAL_STEPS, "Offline incremental index regeneration", elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }


    private void runStep(Project project, int step, String label, Runnable action) {
        long stepStarted = System.nanoTime();
        pipelineLogger.stepStart(project, PHASE, step, TOTAL_STEPS, label);
        try {
            action.run();
            pipelineLogger.stepComplete(project, PHASE, step, TOTAL_STEPS, label, elapsedMs(stepStarted));
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, step, TOTAL_STEPS, label, elapsedMs(stepStarted), ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public void purgeOfflineIndices(Long projectId) {
        projectFileMetadataIndexRepository.deleteByProjectId(projectId);
    }

    private void regenerateSummaries(Project project, AIModel aiModel, boolean incremental) {
        List<ProjectFileIndex> allFiles = projectFileIndexRepository.findByProjectId(project.getId());
        if (allFiles.isEmpty()) {
            pipelineLogger.message(project, PHASE, "No indexed files — skipped file summaries");
            return;
        }
        List<ProjectFileIndex> filesToSummarize;
        if (incremental) {
            self.purgeStaleFileSummaries(project.getId(), allFiles);
            List<ProjectFileMetadataIndex> existingSummaries = projectFileMetadataIndexRepository.findByProjectId(project.getId());
            filesToSummarize = selectFilesNeedingSummary(allFiles, existingSummaries);
            int unchanged = allFiles.size() - filesToSummarize.size();
            pipelineLogger.message(project, PHASE, "Incremental file summaries: " + filesToSummarize.size()
                    + " to process, " + unchanged + " unchanged");
        } else {
            filesToSummarize = allFiles;
        }
        summarizeAndPersistFiles(project, aiModel, filesToSummarize);
    }

    static List<ProjectFileIndex> selectFilesNeedingSummary(
            List<ProjectFileIndex> allFiles,
            List<ProjectFileMetadataIndex> existingSummaries
    ) {
        List<ProjectFileIndex> selected = new ArrayList<>();
        for (ProjectFileIndex file : allFiles) {
            ProjectFileMetadataIndex existing = existingSummaries
                    .stream()
                    .filter(s -> Objects.equals(s.getFile(), file))
                    .findFirst()
                    .orElse(null);
            if(existing == null || !file.getContentHash().equals(existing.getContentHash())) {
                selected.add(file);
            }
        }
        return selected;
    }

    @Transactional
    public void purgeStaleFileSummaries(Long projectId, List<ProjectFileIndex> files) {
        if (files.isEmpty()) {
            projectFileMetadataIndexRepository.deleteByProjectId(projectId);
            return;
        }
        projectFileMetadataIndexRepository.deleteByProjectIdAndFileIdNotIn(projectId, files.stream().map(ProjectFileIndex::getId).toList());
    }

    private void summarizeAndPersistFiles(Project project, AIModel aiModel, List<ProjectFileIndex> filesToSummarize) {
        if (filesToSummarize.isEmpty()) {
            pipelineLogger.message(project, PHASE, "No file summaries needed");
            return;
        }
        List<String> chunks = fileSummaryChunkBuilder.buildChunks(project, filesToSummarize, aiModel, summaryTemplate);
        pipelineLogger.message(project, PHASE, "Summarizing " + filesToSummarize.size() + " files in " + chunks.size() + " chunk(s)");
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            int chunkNumber = chunkIndex + 1;
            List<FileSummaryOfflineResponse> chunkSummaries = getFileSummaryOffline(
                    project,
                    aiModel,
                    chunks.get(chunkIndex),
                    chunkNumber,
                    chunks.size()
            );
            self.persistSummaryItems(project, chunkSummaries, filesToSummarize, chunkNumber, chunks.size());
        }
    }

    @Transactional
    public void persistSummaryItems(
            Project project,
            List<FileSummaryOfflineResponse> summaries,
            List<ProjectFileIndex> filesToSummarize,
            int chunkIndex,
            int chunkCount
    ) {
        if (summaries.isEmpty()) {
            pipelineLogger.message(project, PHASE, "File summaries response empty — skipped persistence (chunk "
                    + chunkIndex + "/" + chunkCount + ")");
            return;
        }
        Set<String> seenFiles = new HashSet<>();
        int saved = 0;
        for (FileSummaryOfflineResponse item : summaries) {
            if (item.filePath() == null || item.filePath().isBlank() || item.metadata() == null) {
                continue;
            }
            String fileKey = item.filePath().trim();
            if (!seenFiles.add(fileKey)) {
                continue;
            }
            filesToSummarize
                    .stream()
                    .filter(f -> Objects.equals(f.getFilePath(), fileKey))
                    .findFirst()
                    .ifPresent(f -> saveSummary(item.metadata(), f));
            saved++;
        }
        pipelineLogger.message(project, PHASE, "Persisted " + saved + " file summaries (chunk "
                + chunkIndex + "/" + chunkCount + ")");
    }

    private void saveSummary(FileSummaryOfflineResponse.Metadata metadata, ProjectFileIndex fileIndex) {
        ProjectFileMetadataIndex entry = projectFileMetadataIndexRepository.findByFileId(fileIndex.getId())
                .orElseGet(() -> {
                    ProjectFileMetadataIndex newEntry = new ProjectFileMetadataIndex();
                    newEntry.setProject(fileIndex.getProject());
                    newEntry.setFile(fileIndex);
                    return newEntry;
                });
        try {
            entry.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed serializing file metadata for " + fileIndex.getFilePath(), ex);
        }
        entry.setContentHash(fileIndex.getContentHash());
        entry.setUpdatedAt(LocalDateTime.now());
        projectFileMetadataIndexRepository.save(entry);
    }

    private List<FileSummaryOfflineResponse> getFileSummaryOffline(
            Project project,
            AIModel aiModel,
            String filesBlock,
            int chunkIndex,
            int chunkCount
    ) {
        String agentsFile = projectService.resolveAgentsFileContent(project);
        String prompt = promptFormatService.formatPrompt(summaryTemplate, Map.of(
                "FILES", filesBlock,
                "PROJECT_ARCHITECTURE", project.getDescription(),
                "AGENTS_FILE", agentsFile
        ));
        String logLabel = "Offline index: file summaries chunk " + chunkIndex + "/" + chunkCount;
        String raw = aiModelService.sendToModel(project, aiModel, prompt, NOTES, logLabel).reponse();
        FileSummaryOfflineResponse[] entries = JsonResponseExtractor.parseResponse(
                raw,
                FileSummaryOfflineResponse[].class,
                objectMapper
        );
        if (entries == null || entries.length == 0) {
            return List.of();
        }
        return List.of(entries);
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}
