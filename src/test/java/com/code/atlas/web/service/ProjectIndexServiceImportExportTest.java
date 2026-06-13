package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.ProjectFileMetadataIndex;
import com.code.atlas.web.repository.ProjectFileIndexRepository;
import com.code.atlas.web.repository.ProjectFileMetadataIndexRepository;
import com.code.atlas.web.service.context.indexed.ContextPipelineLogger;
import com.code.atlas.web.service.dto.FlatProjectIndexDto;
import com.code.atlas.web.service.dto.ImportPreflightDto;
import com.code.atlas.web.service.dto.IndexIntegrityStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectIndexServiceImportExportTest {

    private static final String HASH = "a".repeat(64);

    @Mock
    private ProjectFileIndexRepository projectFileIndexRepository;

    @Mock
    private ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private ContextPipelineLogger pipelineLogger;

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;
    private ProjectIndexService projectIndexService;
    private Project project;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        projectIndexService = new ProjectIndexService(
                projectFileIndexRepository,
                projectFileMetadataIndexRepository,
                projectService,
                objectMapper,
                pipelineLogger,
                30L,
                52428800L,
                15L
        );
        project = new Project();
        project.setId(1L);
        project.setName("Demo");
        project.setPath(tempDir.toString());
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/Main.java"), "class Main {}");
    }

    @Test
    void preflightCategorizesNewAndOverwritePaths() throws IOException {
        ProjectFileIndex existing = fileIndex("src/Main.java");
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(projectFileIndexRepository.findByProjectId(1L)).thenReturn(List.of(existing));

        String json = objectMapper.writeValueAsString(new FlatProjectIndexDto[]{
                flatDto("src/Main.java"),
                flatDto("src/New.java")
        });

        ImportPreflightDto preflight = projectIndexService.analyzeImport(
                1L,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(2, preflight.totalRecords());
        assertEquals(1, preflight.overwriteCount());
        assertEquals(1, preflight.newPathCount());
        assertEquals(1, preflight.missingOnDiskCount());
        assertTrue(preflight.warnings().stream().anyMatch(w -> w.contains("src/New.java")));
    }

    @Test
    void preflightRejectsInvalidPathTraversal() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(projectFileIndexRepository.findByProjectId(1L)).thenReturn(List.of());

        String json = """
                [{"filePath":"../secret.txt","fileExtension":".txt","lastModifiedEpoch":1,"contentHash":"%s","tokenCount":1}]
                """.formatted(HASH);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> projectIndexService.analyzeImport(1L, new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
        );

        assertEquals("Invalid file path: ../secret.txt", ex.getMessage());
    }

    @Test
    void analyzeImportRejectsMalformedMetadata() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(projectFileIndexRepository.findByProjectId(1L)).thenReturn(List.of());

        String json = """
                [{"filePath":"src/Bad.java","fileExtension":".java","lastModifiedEpoch":1,"contentHash":"%s","tokenCount":1,"metadataJson":"not-json"}]
                """.formatted(HASH);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> projectIndexService.analyzeImport(1L, new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
        );

        assertEquals("Metadata JSON is not valid JSON.", ex.getMessage());
    }

    @Test
    void confirmImportRejectsMissingPreflightSession() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> projectIndexService.confirmImport(1L, "missing-id")
        );

        assertEquals("Preflight session expired or not found. Upload the file again.", ex.getMessage());
    }

    @Test
    void clearDeletesBothTablesAndValidatesConfirmation() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> projectIndexService.clearIndex(1L, "WRONG", "Demo")
        );
        assertEquals("Confirmation text must be CLEAR or the exact project name.", ex.getMessage());

        projectIndexService.clearIndex(1L, "CLEAR", "Demo");
        verify(projectFileMetadataIndexRepository).deleteByProjectId(1L);
        verify(projectFileIndexRepository).deleteByProjectId(1L);
    }

    @Test
    void exportStreamWritesValidJsonArray() throws IOException {
        ProjectFileIndex fileEntry = fileIndex("src/Main.java");
        ProjectFileMetadataIndex metadataEntry = new ProjectFileMetadataIndex();
        metadataEntry.setFile(fileEntry);
        metadataEntry.setMetadataJson("{\"summary\":\"demo\"}");
        metadataEntry.setContentHash(HASH);

        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(projectFileIndexRepository.findByProjectId(1L)).thenReturn(List.of(fileEntry));
        when(projectFileMetadataIndexRepository.findByProjectId(1L)).thenReturn(List.of(metadataEntry));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        projectIndexService.writeExportStream(1L, out);

        FlatProjectIndexDto[] exported = objectMapper.readValue(out.toByteArray(), FlatProjectIndexDto[].class);
        assertEquals(1, exported.length);
        assertEquals("src/Main.java", exported[0].filePath());
        assertEquals("{\"summary\":\"demo\"}", exported[0].metadataJson());
    }

    @Test
    void getIndexStatusReturnsFreshWhenRecent() {
        ProjectFileIndex fileEntry = fileIndex("src/Main.java");
        fileEntry.setUpdatedAt(LocalDateTime.now());
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(projectFileIndexRepository.findByProjectId(1L)).thenReturn(List.of(fileEntry));
        when(projectFileMetadataIndexRepository.findByProjectId(1L)).thenReturn(List.of());

        assertEquals(IndexIntegrityStatus.FRESH, projectIndexService.getIndexStatus(1L).integrity());
    }

    private FlatProjectIndexDto flatDto(String path) {
        return new FlatProjectIndexDto(path, ".java", 1L, HASH, 4, null, null);
    }

    private ProjectFileIndex fileIndex(String path) {
        ProjectFileIndex entry = new ProjectFileIndex();
        entry.setId(99L);
        entry.setProject(project);
        entry.setFilePath(path);
        entry.setFileExtension(".java");
        entry.setLastModifiedEpoch(1L);
        entry.setContentHash(HASH);
        entry.setTokenCount(4);
        entry.setUpdatedAt(LocalDateTime.now());
        return entry;
    }
}
