package com.code.atlas.web.service;

import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.context.deterministic.ContextFileSupport;
import com.code.atlas.web.service.context.deterministic.ContextQuery;
import com.code.atlas.web.service.context.deterministic.ContextSymbolExtractor;
import com.code.atlas.web.service.context.indexed.ContextPipelineLogger;
import com.code.atlas.web.repository.ProjectFileIndexRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProjectIndexService {

    private final ProjectFileIndexRepository projectFileIndexRepository;
    private final ContextPipelineLogger pipelineLogger;
    private final Duration maxAge;

    public ProjectIndexService(
            ProjectFileIndexRepository projectFileIndexRepository,
            ContextPipelineLogger pipelineLogger,
            @Value("${codeatlas.context.index-max-age-minutes:30}") long maxAgeMinutes
    ) {
        this.projectFileIndexRepository = projectFileIndexRepository;
        this.pipelineLogger = pipelineLogger;
        this.maxAge = Duration.ofMinutes(Math.max(1, maxAgeMinutes));
    }

    @Transactional
    public void refreshIndex(Project project) {
        refreshIndex(project, "index");
    }

    @Transactional
    public void refreshIndex(Project project, String phase) {
        long started = System.nanoTime();
        pipelineLogger.stepStart(project, phase, 1, 2, "Scan project files and refresh file index");
        try {
            Path projectRoot = Path.of(project.getPath()).normalize();
            if (!Files.exists(projectRoot)) {
                projectFileIndexRepository.deleteByProjectId(project.getId());
                pipelineLogger.stepComplete(project, phase, 1, 2, "Scan project files and refresh file index", elapsedMs(started));
                return;
            }
            List<Path> files = collectRelevantFiles(projectRoot);
            List<String> activeRelativePaths = new ArrayList<>();
            Map<String, ProjectFileIndex> existingByPath = projectFileIndexRepository.findByProjectId(project.getId()).stream()
                    .collect(Collectors.toMap(ProjectFileIndex::getFilePath, Function.identity(), (left, right) -> left));

            for (Path filePath : files) {
                String relativePath = projectRoot.relativize(filePath).toString().replace('\\', '/');
                activeRelativePaths.add(relativePath);
                ProjectFileIndex existing = existingByPath.get(relativePath);
                upsertIndexEntry(project, filePath, relativePath, existing);
            }
            if (!activeRelativePaths.isEmpty()) {
                projectFileIndexRepository.deleteByProjectIdAndFilePathNotIn(project.getId(), activeRelativePaths);
            } else {
                projectFileIndexRepository.deleteByProjectId(project.getId());
            }
            pipelineLogger.stepComplete(project, phase, 1, 2, "Scan project files and refresh file index", elapsedMs(started));
            pipelineLogger.message(project, phase, "Indexed " + activeRelativePaths.size() + " project files");
        } catch (RuntimeException ex) {
            pipelineLogger.stepFailed(project, phase, 1, 2, "Refresh file index", elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    public List<ProjectFileIndex> search(Project project, ContextQuery query, int limit) {
        List<ProjectFileIndex> entries = projectFileIndexRepository.findByProjectId(project.getId());
        if (entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .limit(limit)
                .toList();
    }

    public boolean isStale(Project project) {
        List<ProjectFileIndex> entries = projectFileIndexRepository.findByProjectId(project.getId());
        if (entries.isEmpty()) {
            return true;
        }
        LocalDateTime newestUpdate = entries.stream()
                .map(ProjectFileIndex::getUpdatedAt)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
        return newestUpdate.isBefore(LocalDateTime.now().minus(maxAge));
    }

    private void upsertIndexEntry(Project project, Path filePath, String relativePath, ProjectFileIndex existing) {
        try {
            long lastModified = Files.getLastModifiedTime(filePath).toMillis();
            String content = Files.readString(filePath);
            String contentHash = hash(content);
            if (existing != null && existing.getLastModifiedEpoch() == lastModified
                    && contentHash.equals(existing.getContentHash())) {
                existing.setUpdatedAt(LocalDateTime.now());
                projectFileIndexRepository.save(existing);
                return;
            }
            String extension = ContextFileSupport.extensionOf(filePath.getFileName().toString());

            ProjectFileIndex entity = existing == null ? new ProjectFileIndex() : existing;
            entity.setProject(project);
            entity.setFilePath(relativePath);
            entity.setFileExtension(extension);
            entity.setLastModifiedEpoch(lastModified);
            entity.setContentHash(contentHash);
            entity.setTokenCount((content.length() + 3) / 4);
            entity.setUpdatedAt(LocalDateTime.now());
            projectFileIndexRepository.save(entity);
        } catch (IOException ex) {
            // Skip unreadable files and keep index refresh resilient.
        }
    }

    private List<Path> collectRelevantFiles(Path projectRoot) {
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> ContextFileSupport.isRelevantFile(projectRoot.relativize(path)))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available.");
        }
    }
}
