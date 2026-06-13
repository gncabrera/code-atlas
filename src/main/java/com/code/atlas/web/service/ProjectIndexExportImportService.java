package com.code.atlas.web.service;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.ProjectFileMetadataIndex;
import com.code.atlas.web.repository.ProjectFileIndexRepository;
import com.code.atlas.web.repository.ProjectFileMetadataIndexRepository;
import com.code.atlas.web.service.dto.FlatProjectIndexDto;
import com.code.atlas.web.service.dto.CachedPreflight;
import com.code.atlas.web.service.dto.ImportPreflightDto;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProjectIndexExportImportService {

    private static final int IMPORT_BATCH_SIZE = 50;
    private static final int MAX_WARNING_SAMPLES = 10;

    private final ProjectService projectService;
    private final ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository;
    private final ProjectFileIndexRepository projectFileIndexRepository;
    private final JsonFactory jsonFactory;
    private final ObjectMapper objectMapper;
    private final long importMaxBytes;
    private final Duration preflightCacheTtl;
    private final ConcurrentHashMap<String, CachedPreflight> preflightCache = new ConcurrentHashMap<>();

    public ProjectIndexExportImportService(
            ProjectService projectService,
            ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository,
            ProjectFileIndexRepository projectFileIndexRepository,
            ObjectMapper objectMapper,
            @Value("${codeatlas.index.import-max-bytes:52428800}") long importMaxBytes,
            @Value("${codeatlas.index.preflight-cache-minutes:15}") long preflightCacheMinutes
    ) {
        this.projectService = projectService;
        this.projectFileMetadataIndexRepository = projectFileMetadataIndexRepository;
        this.projectFileIndexRepository = projectFileIndexRepository;
        this.jsonFactory = objectMapper.getFactory();
        this.objectMapper = objectMapper;
        this.importMaxBytes = Math.max(1, importMaxBytes);
        this.preflightCacheTtl = Duration.ofMinutes(Math.max(1, preflightCacheMinutes));
    }


    public void writeExportStream(Long projectId, OutputStream out) throws IOException {
        projectService.getProjectEntity(projectId);
        Map<Long, ProjectFileMetadataIndex> metadataByFileId = projectFileMetadataIndexRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(entry -> entry.getFile().getId(), Function.identity(), (left, right) -> left));
        List<ProjectFileIndex> fileEntries = projectFileIndexRepository.findByProjectId(projectId).stream()
                .sorted(Comparator.comparing(ProjectFileIndex::getFilePath))
                .toList();

        try (JsonGenerator generator = jsonFactory.createGenerator(out)) {
            generator.writeStartArray();
            for (int i = 0; i < fileEntries.size(); i++) {
                ProjectFileIndex fileEntry = fileEntries.get(i);
                ProjectFileMetadataIndex metadataEntry = metadataByFileId.get(fileEntry.getId());
                FlatProjectIndexDto dto = new FlatProjectIndexDto(
                        fileEntry.getFilePath(),
                        fileEntry.getFileExtension(),
                        fileEntry.getLastModifiedEpoch(),
                        fileEntry.getContentHash(),
                        fileEntry.getTokenCount(),
                        metadataEntry == null ? null : metadataEntry.getMetadataJson(),
                        metadataEntry == null ? null : metadataEntry.getContentHash()
                );
                generator.writeObject(dto);
            }
            generator.writeEndArray();
            generator.flush();
        }
    }

    public ImportPreflightDto analyzeImport(Long projectId, InputStream jsonStream) throws IOException {
        Project project = projectService.getProjectEntity(projectId);
        evictExpiredPreflights();
        byte[] payload = readBounded(jsonStream);
        FlatProjectIndexDto[] records = objectMapper.readValue(payload, FlatProjectIndexDto[].class);
        if (records == null || records.length == 0) {
            throw new IllegalArgumentException("Import file must contain at least one index record.");
        }

        Map<String, ProjectFileIndex> existingByPath = projectFileIndexRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(ProjectFileIndex::getFilePath, Function.identity(), (left, right) -> left));
        Path projectRoot = Path.of(project.getPath()).normalize();
        boolean projectRootExists = Files.exists(projectRoot);

        int overwriteCount = 0;
        int newPathCount = 0;
        int missingOnDiskCount = 0;
        List<String> warnings = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();
        List<FlatProjectIndexDto> normalizedRecords = new ArrayList<>();

        for (FlatProjectIndexDto record : records) {
            String normalizedPath = validateImportPath(record.filePath());
            if (!seenPaths.add(normalizedPath)) {
                throw new IllegalArgumentException("Duplicate file path in import: " + normalizedPath);
            }
            validateContentHash(record.contentHash());
            if (record.metadataJson() != null && !record.metadataJson().isBlank()) {
                validateMetadataJson(record.metadataJson());
            }
            normalizedRecords.add(new FlatProjectIndexDto(
                    normalizedPath,
                    record.fileExtension(),
                    record.lastModifiedEpoch(),
                    record.contentHash(),
                    record.tokenCount(),
                    record.metadataJson(),
                    record.metadataContentHash()
            ));
            if (existingByPath.containsKey(normalizedPath)) {
                overwriteCount++;
            } else {
                newPathCount++;
            }
            if (projectRootExists && !Files.exists(projectRoot.resolve(normalizedPath))) {
                missingOnDiskCount++;
                if (warnings.size() < MAX_WARNING_SAMPLES) {
                    warnings.add("Path not found on disk: " + normalizedPath);
                }
            }
        }
        if (missingOnDiskCount > MAX_WARNING_SAMPLES) {
            warnings.add("And " + (missingOnDiskCount - MAX_WARNING_SAMPLES) + " more paths missing on disk.");
        }

        String preflightId = UUID.randomUUID().toString();
        preflightCache.put(preflightId, new CachedPreflight(
                projectId,
                List.copyOf(normalizedRecords),
                LocalDateTime.now().plus(preflightCacheTtl)
        ));
        return new ImportPreflightDto(
                preflightId,
                normalizedRecords.size(),
                overwriteCount,
                newPathCount,
                missingOnDiskCount,
                List.copyOf(warnings)
        );
    }

    @Transactional
    public void confirmImport(Long projectId, String preflightId) {
        Project project = projectService.getProjectEntity(projectId);
        evictExpiredPreflights();
        CachedPreflight cached = preflightCache.get(preflightId);
        if (cached == null) {
            throw new IllegalArgumentException("Preflight session expired or not found. Upload the file again.");
        }
        if (!cached.projectId().equals(projectId)) {
            throw new IllegalArgumentException("Preflight session does not match the selected project.");
        }

        List<FlatProjectIndexDto> records = cached.records();
        int processed = 0;
        for (FlatProjectIndexDto record : records) {
            String normalizedPath = validateImportPath(record.filePath());
            ProjectFileIndex fileEntry = projectFileIndexRepository.findByProjectIdAndFilePath(projectId, normalizedPath)
                    .orElseGet(ProjectFileIndex::new);
            fileEntry.setProject(project);
            fileEntry.setFilePath(normalizedPath);
            fileEntry.setFileExtension(record.fileExtension());
            fileEntry.setLastModifiedEpoch(record.lastModifiedEpoch());
            fileEntry.setContentHash(record.contentHash());
            fileEntry.setTokenCount(record.tokenCount());
            fileEntry.setUpdatedAt(LocalDateTime.now());
            ProjectFileIndex savedFileEntry = projectFileIndexRepository.save(fileEntry);

            if (record.metadataJson() != null && !record.metadataJson().isBlank()) {
                validateMetadataJson(record.metadataJson());
                ProjectFileIndex managedFile = projectFileIndexRepository.findById(savedFileEntry.getId())
                        .orElseThrow(() -> new IllegalStateException("Indexed file not found for id: " + savedFileEntry.getId()));
                ProjectFileMetadataIndex metadataEntry = projectFileMetadataIndexRepository.findByFileId(managedFile.getId())
                        .orElseGet(() -> {
                            ProjectFileMetadataIndex newEntry = new ProjectFileMetadataIndex();
                            newEntry.setProject(project);
                            newEntry.setFile(managedFile);
                            return newEntry;
                        });
                metadataEntry.setMetadataJson(record.metadataJson());
                metadataEntry.setContentHash(
                        record.metadataContentHash() != null && !record.metadataContentHash().isBlank()
                                ? record.metadataContentHash()
                                : managedFile.getContentHash()
                );
                metadataEntry.setUpdatedAt(LocalDateTime.now());
                projectFileMetadataIndexRepository.save(metadataEntry);
            }

            processed++;
            if (processed % IMPORT_BATCH_SIZE == 0) {
                projectFileIndexRepository.flush();
                projectFileMetadataIndexRepository.flush();
            }
        }
        preflightCache.remove(preflightId);
    }
    private void validateMetadataJson(String metadataJson) {
        try {
            JsonNode node = objectMapper.readTree(metadataJson);
            if (!node.isObject() && !node.isArray()) {
                throw new IllegalArgumentException("Metadata JSON must be a JSON object or array.");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Metadata JSON is not valid JSON.");
        }
    }

    private void evictExpiredPreflights() {
        LocalDateTime now = LocalDateTime.now();
        preflightCache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private byte[] readBounded(InputStream inputStream) throws IOException {
        byte[] buffer = inputStream.readAllBytes();
        if (buffer.length > importMaxBytes) {
            throw new IllegalArgumentException("Import file exceeds maximum allowed size of " + importMaxBytes + " bytes.");
        }
        return buffer;
    }

    private String validateImportPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("File path is required.");
        }
        if (rawPath.contains("\0")) {
            throw new IllegalArgumentException("Invalid file path.");
        }
        String normalized = rawPath.replace('\\', '/').trim();
        if (normalized.startsWith("/") || normalized.contains("..")) {
            throw new IllegalArgumentException("Invalid file path: " + rawPath);
        }
        return normalized;
    }

    private void validateContentHash(String contentHash) {
        if (contentHash == null || contentHash.length() != 64 || !contentHash.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("Content hash must be a 64-character hex string.");
        }
    }
}
