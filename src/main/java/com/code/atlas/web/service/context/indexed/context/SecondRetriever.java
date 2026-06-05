package com.code.atlas.web.service.context.indexed.context;

import com.code.atlas.web.domain.DatabaseIndexEntry;
import com.code.atlas.web.domain.FrontendIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.repository.DatabaseIndexRepository;
import com.code.atlas.web.repository.FrontendIndexRepository;
import com.code.atlas.web.repository.SymbolIndexRepository;
import com.code.atlas.web.service.context.indexed.IndexedFileLoader;
import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.MissingContext;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecondRetriever {

    private final DatabaseIndexRepository databaseIndexRepository;
    private final FrontendIndexRepository frontendIndexRepository;
    private final SymbolIndexRepository symbolIndexRepository;
    private final IndexedFileLoader indexedFileLoader;
    private final int maxFiles;

    public SecondRetriever(
            DatabaseIndexRepository databaseIndexRepository,
            FrontendIndexRepository frontendIndexRepository,
            SymbolIndexRepository symbolIndexRepository,
            IndexedFileLoader indexedFileLoader,
            @Value("${codeatlas.context.indexed.max-files:12}") int maxFiles
    ) {
        this.databaseIndexRepository = databaseIndexRepository;
        this.frontendIndexRepository = frontendIndexRepository;
        this.symbolIndexRepository = symbolIndexRepository;
        this.indexedFileLoader = indexedFileLoader;
        this.maxFiles = Math.max(1, maxFiles);
    }

    public List<RetrievedFile> retrieve(Project project, Intent intent, MissingContext missing) {
        if (missing.missing().isEmpty()) {
            return List.of();
        }
        Map<String, RetrievedFile> filesByPath = new LinkedHashMap<>();
        for (String category : missing.missing()) {
            String normalized = category.toLowerCase(Locale.ROOT);
            if (normalized.contains("migration") || normalized.contains("database") || normalized.contains("entity")) {
                addDatabaseMatches(project, intent, filesByPath);
            }
            if (normalized.contains("frontend")) {
                addFrontendMatches(project, filesByPath);
            }
            if (normalized.contains("repository") || normalized.contains("service") || normalized.contains("controller")) {
                addEntitySymbolMatches(project, intent, filesByPath);
            }
        }
        return filesByPath.values().stream().limit(maxFiles).toList();
    }

    private void addDatabaseMatches(Project project, Intent intent, Map<String, RetrievedFile> filesByPath) {
        for (DatabaseIndexEntry entry : databaseIndexRepository.findByProjectId(project.getId())) {
            addFile(project, entry.getFilePath(), 80, "Database index match", filesByPath);
        }
        for (String entity : intent.entities()) {
            for (DatabaseIndexEntry entry : databaseIndexRepository.findByProjectIdAndTableNameContainingIgnoreCase(
                    project.getId(),
                    entity.toLowerCase(Locale.ROOT)
            )) {
                addFile(project, entry.getFilePath(), 85, "Database index for " + entity, filesByPath);
            }
        }
    }

    private void addFrontendMatches(Project project, Map<String, RetrievedFile> filesByPath) {
        for (FrontendIndexEntry entry : frontendIndexRepository.findByProjectId(project.getId())) {
            addFile(project, entry.getFilePath(), 75, "Frontend index: " + entry.getComponent(), filesByPath);
        }
    }

    private void addEntitySymbolMatches(Project project, Intent intent, Map<String, RetrievedFile> filesByPath) {
        for (String entity : intent.entities()) {
            symbolIndexRepository.findByProjectIdAndSymbolIgnoreCase(project.getId(), entity)
                    .forEach(entry -> addFile(project, entry.getFilePath(), 70, "Symbol match for " + entity, filesByPath));
            symbolIndexRepository.findByProjectIdAndSymbolIgnoreCase(project.getId(), entity + "Service")
                    .forEach(entry -> addFile(project, entry.getFilePath(), 70, "Service match for " + entity, filesByPath));
            symbolIndexRepository.findByProjectIdAndSymbolIgnoreCase(project.getId(), entity + "Repository")
                    .forEach(entry -> addFile(project, entry.getFilePath(), 70, "Repository match for " + entity, filesByPath));
            symbolIndexRepository.findByProjectIdAndSymbolIgnoreCase(project.getId(), entity + "Controller")
                    .forEach(entry -> addFile(project, entry.getFilePath(), 70, "Controller match for " + entity, filesByPath));
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
}
