package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.FileSummaryIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.repository.FileSummaryIndexRepository;
import com.code.atlas.web.repository.ProjectFileIndexRepository;
import com.code.atlas.web.service.*;
import com.code.atlas.web.service.context.indexed.builder.OfflineFileSummaryChunkBuilder;
import com.code.atlas.web.service.context.indexed.dto.FileSummaryOfflineResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final EntityManager entityManager;
    private final ContextPipelineLogger pipelineLogger;
    private final String summaryTemplate;
    private final FileSummaryIndexRepository fileSummaryIndexRepository;
    private final OfflineFileSummaryChunkBuilder fileSummaryChunkBuilder;

    public IndexBuilderService(
            ProjectFileIndexRepository projectFileIndexRepository,
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            ObjectMapper objectMapper,
            EntityManager entityManager,
            ContextPipelineLogger pipelineLogger, FileSummaryIndexRepository fileSummaryIndexRepository, OfflineFileSummaryChunkBuilder fileSummaryChunkBuilder
    ) {
        this.projectFileIndexRepository = projectFileIndexRepository;
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.pipelineLogger = pipelineLogger;
        this.fileSummaryIndexRepository = fileSummaryIndexRepository;
        this.fileSummaryChunkBuilder = fileSummaryChunkBuilder;
        this.summaryTemplate = PromptTemplateService.load(PromptTemplate.CONTEXT_FILE_SUMMARY);
    }

    @Transactional
    public void regenerate(Project project, AIModel aiModel) {
        pipelineLogger.message(project, PHASE, "Starting offline index regeneration (full)");
        long started = System.nanoTime();
        try {
            runStep(project, 1, "Purge offline indices", () -> {
                purgeOfflineIndices(project.getId());
                entityManager.flush();
            });
            runStep(project, 2, "Generate file summaries", () -> regenerateSummaries(project, aiModel, false));
            pipelineLogger.message(project, PHASE, "Offline index regeneration finished (" + elapsedMs(started) + " ms)");
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, PHASE, 0, TOTAL_STEPS, "Offline index regeneration", elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    @Transactional
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

    private void purgeOfflineIndices(Long projectId) {
    }

    private void regenerateSummaries(Project project, AIModel aiModel, boolean incremental) {
        List<ProjectFileIndex> allFiles = projectFileIndexRepository.findByProjectId(project.getId());
        if (allFiles.isEmpty()) {
            pipelineLogger.message(project, PHASE, "No indexed files — skipped file summaries");
            return;
        }
        List<ProjectFileIndex> filesToSummarize;
        if (incremental) {
            List<String> activePaths = allFiles.stream().map(ProjectFileIndex::getFilePath).toList();
            purgeStaleFileSummaries(project.getId(), activePaths);
            entityManager.flush();
            Map<String, FileSummaryIndexEntry> existingSummaries = fileSummaryIndexRepository.findByProjectId(project.getId()).stream()
                    .collect(Collectors.toMap(FileSummaryIndexEntry::getFilePath, Function.identity(), (left, right) -> left));
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
            Map<String, FileSummaryIndexEntry> existingSummariesByPath
    ) {
        List<ProjectFileIndex> selected = new ArrayList<>();
        for (ProjectFileIndex file : allFiles) {
            FileSummaryIndexEntry existing = existingSummariesByPath.get(file.getFilePath());
            if (existing == null || !file.getContentHash().equals(existing.getContentHash())) {
                selected.add(file);
            }
        }
        return selected;
    }

    private void purgeStaleFileSummaries(Long projectId, List<String> activePaths) {
        if (activePaths.isEmpty()) {
            fileSummaryIndexRepository.deleteByProjectId(projectId);
            return;
        }
        fileSummaryIndexRepository.deleteByProjectIdAndFilePathNotIn(projectId, activePaths);
    }

    private void summarizeAndPersistFiles(Project project, AIModel aiModel, List<ProjectFileIndex> filesToSummarize) {
        if (filesToSummarize.isEmpty()) {
            pipelineLogger.message(project, PHASE, "No file summaries needed");
            return;
        }
        Map<String, String> contentHashByPath = filesToSummarize.stream()
                .collect(Collectors.toMap(ProjectFileIndex::getFilePath, ProjectFileIndex::getContentHash, (left, right) -> left));
        List<String> chunks = fileSummaryChunkBuilder.buildChunks(project, filesToSummarize, aiModel, summaryTemplate);
        pipelineLogger.message(project, PHASE, "Summarizing " + filesToSummarize.size() + " files in " + chunks.size() + " chunk(s)");
        List<FileSummaryOfflineResponse.SummaryItem> summaries = new ArrayList<>();
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            summaries.addAll(getFileSummaryOffline(
                    project,
                    aiModel,
                    chunks.get(chunkIndex),
                    chunkIndex + 1,
                    chunks.size()
            ));
        }
        persistSummaryItems(project, summaries, contentHashByPath);
    }

    private void persistSummaryItems(
            Project project,
            List<FileSummaryOfflineResponse.SummaryItem> summaries,
            Map<String, String> contentHashByPath
    ) {
        if (summaries.isEmpty()) {
            pipelineLogger.message(project, PHASE, "File summaries response empty — skipped persistence");
            return;
        }
        Set<String> seenFiles = new HashSet<>();
        int saved = 0;
        for (FileSummaryOfflineResponse.SummaryItem item : summaries) {
            if (item.file() == null || item.file().isBlank()) {
                continue;
            }
            String fileKey = item.file().trim();
            if (!seenFiles.add(fileKey)) {
                continue;
            }
            String contentHash = contentHashByPath.getOrDefault(fileKey, "");
            saveSummary(project, fileKey, item.summary(), contentHash);
            saved++;
        }
        pipelineLogger.message(project, PHASE, "Persisted " + saved + " file summaries");
    }

    private void saveSummary(Project project, String filePath, String summary, String contentHash) {
        FileSummaryIndexEntry entry = fileSummaryIndexRepository.findByProjectIdAndFilePath(project.getId(), filePath)
                .orElseGet(() -> {
                    FileSummaryIndexEntry newEntry = new FileSummaryIndexEntry();
                    newEntry.setProject(project);
                    newEntry.setFilePath(filePath);
                    return newEntry;
                });
        entry.setSummary(summary == null ? "" : summary);
        entry.setContentHash(contentHash == null ? "" : contentHash);
        entry.setUpdatedAt(LocalDateTime.now());
        fileSummaryIndexRepository.save(entry);
    }

    private List<FileSummaryOfflineResponse.SummaryItem> getFileSummaryOffline(
            Project project,
            AIModel aiModel,
            String filesBlock,
            int chunkIndex,
            int chunkCount
    ) {
        String prompt = promptFormatService.formatPrompt(summaryTemplate, Map.of("FILES", filesBlock));
        String logLabel = "Offline index: file summaries chunk " + chunkIndex + "/" + chunkCount;
        String raw = aiModelService.sendToModel(project, aiModel, prompt, NOTES, logLabel).reponse();
        FileSummaryOfflineResponse response = JsonResponseExtractor.parseResponse(
                raw,
                FileSummaryOfflineResponse.class,
                objectMapper
        );
        if (response.summaries() == null) {
            return List.of();
        }
        return response.summaries();
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}
