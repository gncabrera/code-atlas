package com.code.atlas.web.service.context.indexed.offline;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.BusinessConceptIndexEntry;
import com.code.atlas.web.domain.FileSummaryIndexEntry;
import com.code.atlas.web.domain.PatternIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.repository.BusinessConceptIndexRepository;
import com.code.atlas.web.repository.FileSummaryIndexRepository;
import com.code.atlas.web.repository.PatternIndexRepository;
import com.code.atlas.web.repository.ProjectFileIndexRepository;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.JsonResponseExtractor;
import com.code.atlas.web.service.PromptFormatService;
import com.code.atlas.web.service.context.indexed.IndexedPromptLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class OfflineIndexService {

    private static final String BUSINESS_TEMPLATE = "prompts/context/offline/business-concept.md";
    private static final String PATTERN_TEMPLATE = "prompts/context/offline/pattern.md";
    private static final String SUMMARY_TEMPLATE = "prompts/context/offline/file-summary.md";
    private static final String NOTES = "Indexed context: offline index generation";
    private static final int SUMMARY_CHUNK_SIZE = 50;

    private final ProjectFileIndexRepository projectFileIndexRepository;
    private final BusinessConceptIndexRepository businessConceptIndexRepository;
    private final PatternIndexRepository patternIndexRepository;
    private final FileSummaryIndexRepository fileSummaryIndexRepository;
    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final String businessTemplate;
    private final String patternTemplate;
    private final String summaryTemplate;

    public OfflineIndexService(
            ProjectFileIndexRepository projectFileIndexRepository,
            BusinessConceptIndexRepository businessConceptIndexRepository,
            PatternIndexRepository patternIndexRepository,
            FileSummaryIndexRepository fileSummaryIndexRepository,
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            ObjectMapper objectMapper,
            EntityManager entityManager
    ) {
        this.projectFileIndexRepository = projectFileIndexRepository;
        this.businessConceptIndexRepository = businessConceptIndexRepository;
        this.patternIndexRepository = patternIndexRepository;
        this.fileSummaryIndexRepository = fileSummaryIndexRepository;
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.businessTemplate = IndexedPromptLoader.load(BUSINESS_TEMPLATE);
        this.patternTemplate = IndexedPromptLoader.load(PATTERN_TEMPLATE);
        this.summaryTemplate = IndexedPromptLoader.load(SUMMARY_TEMPLATE);
    }

    @Transactional
    public void regenerate(Project project, AIModel aiModel) {
        purgeOfflineIndices(project.getId());
        entityManager.flush();
        String inventory = buildInventory(project);
        regenerateBusinessConcepts(project, aiModel, inventory);
        regeneratePatterns(project, aiModel, inventory);
        regenerateSummaries(project, aiModel);
    }

    private void purgeOfflineIndices(Long projectId) {
        businessConceptIndexRepository.deleteByProjectId(projectId);
        patternIndexRepository.deleteByProjectId(projectId);
        fileSummaryIndexRepository.deleteByProjectId(projectId);
    }

    private void regenerateBusinessConcepts(Project project, AIModel aiModel, String inventory) {
        String prompt = promptFormatService.formatPrompt(businessTemplate, Map.of("FILE_INVENTORY", inventory));
        String raw = aiModelService.sendToModel(project, aiModel, prompt, NOTES).reponse();
        BusinessConceptOfflineResponse response = JsonResponseExtractor.parseResponse(
                raw,
                BusinessConceptOfflineResponse.class,
                objectMapper
        );
        if (response.concepts() == null) {
            return;
        }
        Set<String> seenConcepts = new HashSet<>();
        for (BusinessConceptOfflineResponse.BusinessConceptItem item : response.concepts()) {
            if (item.concept() == null || item.concept().isBlank()) {
                continue;
            }
            String conceptKey = item.concept().trim().toLowerCase(Locale.ROOT);
            if (!seenConcepts.add(conceptKey)) {
                continue;
            }
            BusinessConceptIndexEntry entry = new BusinessConceptIndexEntry();
            entry.setProject(project);
            entry.setConcept(item.concept().trim());
            entry.setFiles(joinCsv(item.files()));
            entry.setUpdatedAt(LocalDateTime.now());
            businessConceptIndexRepository.save(entry);
        }
    }

    private void regeneratePatterns(Project project, AIModel aiModel, String inventory) {
        String prompt = promptFormatService.formatPrompt(patternTemplate, Map.of("FILE_INVENTORY", inventory));
        String raw = aiModelService.sendToModel(project, aiModel, prompt, NOTES).reponse();
        PatternOfflineResponse response = JsonResponseExtractor.parseResponse(
                raw,
                PatternOfflineResponse.class,
                objectMapper
        );
        if (response.patterns() == null) {
            return;
        }
        Set<String> seenPatterns = new HashSet<>();
        for (PatternOfflineResponse.PatternItem item : response.patterns()) {
            if (item.pattern() == null || item.pattern().isBlank()) {
                continue;
            }
            String patternKey = item.pattern().trim().toLowerCase(Locale.ROOT);
            if (!seenPatterns.add(patternKey)) {
                continue;
            }
            PatternIndexEntry entry = new PatternIndexEntry();
            entry.setProject(project);
            entry.setPattern(item.pattern().trim());
            entry.setFiles(joinCsv(item.files()));
            entry.setUpdatedAt(LocalDateTime.now());
            patternIndexRepository.save(entry);
        }
    }

    private void regenerateSummaries(Project project, AIModel aiModel) {
        List<ProjectFileIndex> files = projectFileIndexRepository.findByProjectId(project.getId());
        if (files.isEmpty()) {
            return;
        }
        List<FileSummaryOfflineResponse.SummaryItem> summaries = new ArrayList<>();
        for (int start = 0; start < files.size(); start += SUMMARY_CHUNK_SIZE) {
            int end = Math.min(start + SUMMARY_CHUNK_SIZE, files.size());
            String filesBlock = files.subList(start, end).stream()
                    .map(ProjectFileIndex::getFilePath)
                    .collect(Collectors.joining("\n"));
            summaries.addAll(getFileSummaryOffline(project, aiModel, filesBlock));
        }

        if (summaries.isEmpty()) {
            return;
        }
        Set<String> seenFiles = new HashSet<>();
        for (FileSummaryOfflineResponse.SummaryItem item : summaries) {
            if (item.file() == null || item.file().isBlank()) {
                continue;
            }
            String fileKey = item.file().trim();
            if (!seenFiles.add(fileKey)) {
                continue;
            }
            FileSummaryIndexEntry entry = new FileSummaryIndexEntry();
            entry.setProject(project);
            entry.setFilePath(fileKey);
            entry.setSummary(item.summary());
            entry.setUpdatedAt(LocalDateTime.now());
            fileSummaryIndexRepository.save(entry);
        }
    }

    private List<FileSummaryOfflineResponse.SummaryItem> getFileSummaryOffline(Project project, AIModel aiModel, String filesBlock) {
        String prompt = promptFormatService.formatPrompt(summaryTemplate, Map.of("FILES", filesBlock));
        String raw = aiModelService.sendToModel(project, aiModel, prompt, NOTES).reponse();
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

    private String buildInventory(Project project) {
        return projectFileIndexRepository.findByProjectId(project.getId()).stream()
                .map(entry -> entry.getFilePath() + ": " + entry.getSymbols())
                .limit(80)
                .collect(Collectors.joining("\n"));
    }

    private String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }
}
