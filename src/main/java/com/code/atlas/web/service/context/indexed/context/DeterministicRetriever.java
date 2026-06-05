package com.code.atlas.web.service.context.indexed.context;

import com.code.atlas.web.domain.BusinessConceptIndexEntry;
import com.code.atlas.web.domain.EndpointIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.SymbolIndexEntry;
import com.code.atlas.web.repository.BusinessConceptIndexRepository;
import com.code.atlas.web.repository.EndpointIndexRepository;
import com.code.atlas.web.repository.SymbolIndexRepository;
import com.code.atlas.web.service.context.indexed.ContextResult;
import com.code.atlas.web.service.context.indexed.IndexedFileLoader;
import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeterministicRetriever {

    private final SymbolIndexRepository symbolIndexRepository;
    private final BusinessConceptIndexRepository businessConceptIndexRepository;
    private final EndpointIndexRepository endpointIndexRepository;
    private final IndexedFileLoader indexedFileLoader;
    private final int maxFiles;

    public DeterministicRetriever(
            SymbolIndexRepository symbolIndexRepository,
            BusinessConceptIndexRepository businessConceptIndexRepository,
            EndpointIndexRepository endpointIndexRepository,
            IndexedFileLoader indexedFileLoader,
            @Value("${codeatlas.context.indexed.max-files:12}") int maxFiles
    ) {
        this.symbolIndexRepository = symbolIndexRepository;
        this.businessConceptIndexRepository = businessConceptIndexRepository;
        this.endpointIndexRepository = endpointIndexRepository;
        this.indexedFileLoader = indexedFileLoader;
        this.maxFiles = Math.max(1, maxFiles);
    }

    public ContextResult retrieve(Project project, Intent intent) {
        Map<String, RetrievedFile> filesByPath = new LinkedHashMap<>();
        for (String entity : intent.entities()) {
            addSymbolMatches(project, entity, 90, "Symbol index match for entity " + entity, filesByPath);
        }
        for (String operation : intent.operations()) {
            addConceptMatches(project, operation, 70, "Business concept match for " + operation, filesByPath);
        }
        if (intent.layers().contains("controller")) {
            for (EndpointIndexEntry endpoint : endpointIndexRepository.findByProjectId(project.getId())) {
                addFile(project, endpoint.getFilePath(), 60, "Endpoint index: " + endpoint.getHttpMethod()
                        + " " + endpoint.getPath(), filesByPath);
            }
        }
        List<RetrievedFile> files = filesByPath.values().stream().limit(maxFiles).toList();
        return new ContextResult(files, List.of());
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
        if (filePath == null || filePath.isBlank() || filesByPath.containsKey(filePath)) {
            return;
        }
        filesByPath.put(filePath, indexedFileLoader.load(project, filePath, score, List.of(reason)));
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
