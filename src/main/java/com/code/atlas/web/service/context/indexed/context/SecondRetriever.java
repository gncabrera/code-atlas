package com.code.atlas.web.service.context.indexed.context;

import com.code.atlas.web.domain.DatabaseIndexEntry;
import com.code.atlas.web.domain.FrontendIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.repository.DatabaseIndexRepository;
import com.code.atlas.web.repository.FrontendIndexRepository;
import com.code.atlas.web.repository.SymbolIndexRepository;
import com.code.atlas.web.service.context.indexed.IndexedFileLoader;
import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.MissingContext;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import java.util.Comparator;
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
    private final IndexedPathFileRetriever indexedPathFileRetriever;
    private final int maxFiles;

    public SecondRetriever(
            DatabaseIndexRepository databaseIndexRepository,
            FrontendIndexRepository frontendIndexRepository,
            SymbolIndexRepository symbolIndexRepository,
            IndexedFileLoader indexedFileLoader,
            IndexedPathFileRetriever indexedPathFileRetriever,
            @Value("${codeatlas.context.indexed.max-files:16}") int maxFiles
    ) {
        this.databaseIndexRepository = databaseIndexRepository;
        this.frontendIndexRepository = frontendIndexRepository;
        this.symbolIndexRepository = symbolIndexRepository;
        this.indexedFileLoader = indexedFileLoader;
        this.indexedPathFileRetriever = indexedPathFileRetriever;
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
        return filesByPath.values().stream()
                .sorted(Comparator.comparingInt(RetrievedFile::score).reversed())
                .limit(maxFiles)
                .toList();
    }

    private void addDatabaseMatches(Project project, Intent intent, Map<String, RetrievedFile> filesByPath) {
        int sizeBefore = filesByPath.size();
        List<DatabaseIndexEntry> databaseEntries = databaseIndexRepository.findByProjectId(project.getId());
        for (DatabaseIndexEntry entry : databaseEntries) {
            addFile(project, entry.getFilePath(), 80, "Database index match", filesByPath);
            if (!entry.getMigration().isBlank() && !entry.getMigration().equals(entry.getFilePath())) {
                addFile(project, entry.getMigration(), 82, "Linked migration from database index", filesByPath);
            }
        }
        for (String entity : intent.entities()) {
            for (DatabaseIndexEntry entry : databaseIndexRepository.findByProjectIdAndTableNameContainingIgnoreCase(
                    project.getId(),
                    entity.toLowerCase(Locale.ROOT)
            )) {
                addFile(project, entry.getFilePath(), 85, "Database index for " + entity, filesByPath);
            }
        }
        if (databaseEntries.isEmpty() || filesByPath.size() == sizeBefore) {
            addMigrationPathFallback(project, filesByPath);
        }
    }

    private void addMigrationPathFallback(Project project, Map<String, RetrievedFile> filesByPath) {
        for (ProjectFileIndex entry : indexedPathFileRetriever.findMigrationFiles(project)) {
            addFile(project, entry.getFilePath(), 78, "Migration path fallback", filesByPath);
        }
    }

    private void addFrontendMatches(Project project, Map<String, RetrievedFile> filesByPath) {
        int sizeBefore = filesByPath.size();
        List<FrontendIndexEntry> frontendEntries = frontendIndexRepository.findByProjectId(project.getId());
        for (FrontendIndexEntry entry : frontendEntries) {
            addFile(project, entry.getFilePath(), 75, "Frontend index: " + entry.getComponent(), filesByPath);
            if (!entry.getService().isBlank() && !entry.getService().equals(entry.getFilePath())) {
                addFile(project, entry.getService(), 74, "Linked frontend script", filesByPath);
            }
        }
        if (frontendEntries.isEmpty() || filesByPath.size() == sizeBefore) {
            addFrontendPathFallback(project, filesByPath);
        }
    }

    private void addFrontendPathFallback(Project project, Map<String, RetrievedFile> filesByPath) {
        for (ProjectFileIndex entry : indexedPathFileRetriever.findFrontendFiles(project)) {
            addFile(project, entry.getFilePath(), 72, "Frontend path fallback", filesByPath);
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
