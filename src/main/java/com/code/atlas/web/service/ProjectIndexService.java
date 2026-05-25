package com.code.atlas.web.service;

import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.context.ContextFileSupport;
import com.code.atlas.web.service.context.ContextQuery;
import com.code.atlas.web.service.context.ContextSymbolExtractor;
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
    private final ContextSymbolExtractor contextSymbolExtractor;
    private final Duration maxAge;

    public ProjectIndexService(
            ProjectFileIndexRepository projectFileIndexRepository,
            ContextSymbolExtractor contextSymbolExtractor,
            @Value("${codeatlas.context.index-max-age-minutes:30}") long maxAgeMinutes
    ) {
        this.projectFileIndexRepository = projectFileIndexRepository;
        this.contextSymbolExtractor = contextSymbolExtractor;
        this.maxAge = Duration.ofMinutes(Math.max(1, maxAgeMinutes));
    }

    @Transactional
    public void refreshIndex(Project project) {
        Path projectRoot = Path.of(project.getPath()).normalize();
        if (!Files.exists(projectRoot)) {
            projectFileIndexRepository.deleteByProjectId(project.getId());
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
    }

    public List<ProjectFileIndex> search(Project project, ContextQuery query, int limit) {
        List<ProjectFileIndex> entries = projectFileIndexRepository.findByProjectId(project.getId());
        if (entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .sorted(Comparator.comparingInt((ProjectFileIndex entry) -> scoreIndexEntry(entry, query)).reversed())
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
            List<String> symbols = contextSymbolExtractor.extractSymbols(content, extension, 12);
            List<String> endpointHints = contextSymbolExtractor.extractEndpointHints(content, 8);
            String searchableText = buildSearchableText(relativePath, content, symbols, endpointHints);

            ProjectFileIndex entity = existing == null ? new ProjectFileIndex() : existing;
            entity.setProject(project);
            entity.setFilePath(relativePath);
            entity.setFileExtension(extension);
            entity.setLastModifiedEpoch(lastModified);
            entity.setContentHash(contentHash);
            entity.setTokenCount((content.length() + 3) / 4);
            entity.setSymbols(String.join(",", symbols));
            entity.setEndpointHints(String.join(",", endpointHints));
            entity.setSearchableText(searchableText);
            entity.setUpdatedAt(LocalDateTime.now());
            projectFileIndexRepository.save(entity);
        } catch (IOException ex) {
            // Skip unreadable files and keep index refresh resilient.
        }
    }

    private int scoreIndexEntry(ProjectFileIndex entry, ContextQuery query) {
        String searchableText = entry.getSearchableText().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : query.keywords()) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            if (entry.getFilePath().toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                score += 8;
            }
            if (searchableText.contains(normalizedKeyword)) {
                score += 5;
            }
        }
        if (query.hasEndpoint()) {
            String endpoint = query.endpointMethod() + " " + query.endpointPath();
            if (entry.getEndpointHints().toLowerCase(Locale.ROOT).contains(endpoint.toLowerCase(Locale.ROOT))) {
                score += 40;
            } else if (searchableText.contains(query.endpointPath().toLowerCase(Locale.ROOT))) {
                score += 20;
            }
        }
        for (String area : query.focusAreas()) {
            if (entry.getFilePath().toLowerCase(Locale.ROOT).contains(area.toLowerCase(Locale.ROOT))) {
                score += 15;
            }
        }
        return score;
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

    private String buildSearchableText(
            String relativePath,
            String content,
            List<String> symbols,
            List<String> endpointHints
    ) {
        int limit = Math.min(content.length(), 4000);
        String contentSample = content.substring(0, limit);
        Set<String> tokens = new LinkedHashSet<>();
        tokens.add(relativePath);
        tokens.add(contentSample);
        tokens.add(String.join(" ", symbols));
        tokens.add(String.join(" ", endpointHints));
        return String.join(" ", tokens).toLowerCase(Locale.ROOT);
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
