package com.code.atlas.web.service.context.indexed.context;

import com.code.atlas.web.domain.BusinessConceptIndexEntry;
import com.code.atlas.web.domain.EndpointIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.SymbolIndexEntry;
import com.code.atlas.web.repository.BusinessConceptIndexRepository;
import com.code.atlas.web.repository.EndpointIndexRepository;
import com.code.atlas.web.repository.SymbolIndexRepository;
import com.code.atlas.web.service.context.indexed.ContextResult;
import com.code.atlas.web.service.context.indexed.IndexedFileLoader;
import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeterministicRetriever {

    private final SymbolIndexRepository symbolIndexRepository;
    private final BusinessConceptIndexRepository businessConceptIndexRepository;
    private final EndpointIndexRepository endpointIndexRepository;
    private final IndexedFileLoader indexedFileLoader;
    private final IndexedPathFileRetriever indexedPathFileRetriever;
    private final int maxFiles;

    public DeterministicRetriever(
            SymbolIndexRepository symbolIndexRepository,
            BusinessConceptIndexRepository businessConceptIndexRepository,
            EndpointIndexRepository endpointIndexRepository,
            IndexedFileLoader indexedFileLoader,
            IndexedPathFileRetriever indexedPathFileRetriever,
            @Value("${codeatlas.context.indexed.max-files:16}") int maxFiles
    ) {
        this.symbolIndexRepository = symbolIndexRepository;
        this.businessConceptIndexRepository = businessConceptIndexRepository;
        this.endpointIndexRepository = endpointIndexRepository;
        this.indexedFileLoader = indexedFileLoader;
        this.indexedPathFileRetriever = indexedPathFileRetriever;
        this.maxFiles = Math.max(1, maxFiles);
    }

    public ContextResult retrieve(Project project, Intent intent) {
        Map<String, RetrievedFile> filesByPath = new LinkedHashMap<>();
        addLayerPathMatches(project, intent, filesByPath);
        for (String entity : intent.entities()) {
            addSymbolMatches(project, entity, 90, "Symbol index match for entity " + entity, filesByPath);
        }
        for (String operation : intent.operations()) {
            addConceptMatches(project, operation, 70, "Business concept match for " + operation, filesByPath);
        }
        if (intent.layers().stream().anyMatch(layer -> "controller".equalsIgnoreCase(layer))) {
            for (EndpointIndexEntry endpoint : endpointIndexRepository.findByProjectId(project.getId())) {
                addFile(project, endpoint.getFilePath(), 60, "Endpoint index: " + endpoint.getHttpMethod()
                        + " " + endpoint.getPath(), filesByPath);
            }
        }
        List<RetrievedFile> files = filesByPath.values().stream()
                .sorted(Comparator.comparingInt(RetrievedFile::score).reversed())
                .limit(maxFiles)
                .toList();
        return new ContextResult(files, List.of());
    }

    private void addLayerPathMatches(Project project, Intent intent, Map<String, RetrievedFile> filesByPath) {
        if (intent.layers().stream().anyMatch(layer -> "migration".equalsIgnoreCase(layer))) {
            for (ProjectFileIndex entry : indexedPathFileRetriever.findMigrationFiles(project)) {
                addFile(project, entry.getFilePath(), 88, "Migration layer path match", filesByPath);
            }
        }
        if (intent.frontendImpact() || intent.layers().stream().anyMatch(layer -> "frontend".equalsIgnoreCase(layer))) {
            for (ProjectFileIndex entry : indexedPathFileRetriever.findFrontendFiles(project)) {
                addFile(project, entry.getFilePath(), 86, "Frontend layer path match", filesByPath);
            }
        }
        for (IndexedPathFileRetriever.ScoredProjectFile scored : indexedPathFileRetriever.scoreForIntent(project, intent)) {
            addFile(
                    project,
                    scored.entry().getFilePath(),
                    scored.score(),
                    "Path and searchable-text match",
                    filesByPath
            );
        }
    }

    private void addSymbolMatches(
            Project project,
            String symbol,
            int score,
            String reason,
            Map<String, RetrievedFile> filesByPath
    ) {
        for (SymbolIndexEntry entry : symbolIndexRepository.findByProjectIdAndSymbolIgnoreCase(project.getId(), symbol)) {
            addFile(project, entry.getFilePath(), score, reason, filesByPath);
        }
    }

    private void addConceptMatches(
            Project project,
            String concept,
            int score,
            String reason,
            Map<String, RetrievedFile> filesByPath
    ) {
        businessConceptIndexRepository.findByProjectIdAndConceptIgnoreCase(project.getId(), concept.toLowerCase(Locale.ROOT))
                .ifPresent(entry -> addConceptFiles(project, entry.getFiles(), score, reason, filesByPath));
        for (BusinessConceptIndexEntry entry : businessConceptIndexRepository.findByProjectId(project.getId())) {
            if (entry.getConcept().toLowerCase(Locale.ROOT).contains(concept.toLowerCase(Locale.ROOT))) {
                addConceptFiles(project, entry.getFiles(), score - 10, reason, filesByPath);
            }
        }
    }

    private void addConceptFiles(
            Project project,
            String filesCsv,
            int score,
            String reason,
            Map<String, RetrievedFile> filesByPath
    ) {
        for (String symbol : splitCsv(filesCsv)) {
            addSymbolMatches(project, symbol, score, reason, filesByPath);
        }
    }

    private void addFile(
            Project project,
            String filePath,
            int score,
            String reason,
            Map<String, RetrievedFile> filesByPath
    ) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        RetrievedFile existing = filesByPath.get(filePath);
        if (existing != null) {
            if (score > existing.score()) {
                filesByPath.put(filePath, indexedFileLoader.load(project, filePath, score, mergeReasons(existing.reasons(), reason)));
            }
            return;
        }
        filesByPath.put(filePath, indexedFileLoader.load(project, filePath, score, List.of(reason)));
    }

    private List<String> mergeReasons(List<String> existingReasons, String reason) {
        List<String> merged = new ArrayList<>(existingReasons);
        if (!merged.contains(reason)) {
            merged.add(reason);
        }
        return merged;
    }

    static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                values.add(trimmed);
            }
        }
        return values;
    }
}
