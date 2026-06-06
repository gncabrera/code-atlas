package com.code.atlas.web.service.context.indexed.context;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.repository.ProjectFileIndexRepository;
import com.code.atlas.web.service.context.indexed.Intent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class IndexedPathFileRetriever {

    private final ProjectFileIndexRepository projectFileIndexRepository;

    public IndexedPathFileRetriever(ProjectFileIndexRepository projectFileIndexRepository) {
        this.projectFileIndexRepository = projectFileIndexRepository;
    }

    public List<ProjectFileIndex> findMigrationFiles(Project project) {
        return projectFileIndexRepository.findByProjectId(project.getId()).stream()
                .filter(entry -> isMigrationPath(entry.getFilePath()))
                .sorted(Comparator.comparing(ProjectFileIndex::getFilePath))
                .toList();
    }

    public List<ProjectFileIndex> findFrontendFiles(Project project) {
        return projectFileIndexRepository.findByProjectId(project.getId()).stream()
                .filter(entry -> isFrontendPath(entry.getFilePath()))
                .sorted(Comparator.comparing(ProjectFileIndex::getFilePath))
                .toList();
    }

    public List<ScoredProjectFile> scoreForIntent(Project project, Intent intent) {
        List<ScoredProjectFile> scored = new ArrayList<>();
        for (ProjectFileIndex entry : projectFileIndexRepository.findByProjectId(project.getId())) {
            int score = scoreEntry(entry, intent);
            if (score > 0) {
                scored.add(new ScoredProjectFile(entry, score));
            }
        }
        return scored.stream()
                .sorted(Comparator.comparingInt(ScoredProjectFile::score).reversed())
                .toList();
    }

    private int scoreEntry(ProjectFileIndex entry, Intent intent) {
        String pathLower = entry.getFilePath().toLowerCase(Locale.ROOT);
        String searchableLower = entry.getSearchableText().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String entity : intent.entities()) {
            String entityLower = entity.toLowerCase(Locale.ROOT);
            if (pathLower.contains(entityLower) || searchableLower.contains(entityLower)) {
                score += 12;
            }
        }
        for (String operation : intent.operations()) {
            String operationLower = operation.toLowerCase(Locale.ROOT);
            if (pathLower.contains(operationLower) || searchableLower.contains(operationLower)) {
                score += 8;
            }
        }
        if (intent.layers().stream().anyMatch(layer -> "migration".equalsIgnoreCase(layer)) && isMigrationPath(entry.getFilePath())) {
            score += 20;
        }
        if ((intent.frontendImpact() || intent.layers().stream().anyMatch(layer -> "frontend".equalsIgnoreCase(layer)))
                && isFrontendPath(entry.getFilePath())) {
            score += 18;
        }
        return score;
    }

    static boolean isMigrationPath(String filePath) {
        String lower = filePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        return lower.endsWith(".sql")
                && (lower.contains("/migration/") || lower.contains("/migrations/") || lower.contains("flyway") || lower.contains("liquibase"));
    }

    static boolean isFrontendPath(String filePath) {
        String lower = filePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        if (lower.contains("/vendor/")) {
            return false;
        }
        return lower.contains("/templates/") && lower.endsWith(".html")
                || lower.contains("/static/js/") && lower.endsWith(".js");
    }

    public record ScoredProjectFile(ProjectFileIndex entry, int score) {
    }
}
