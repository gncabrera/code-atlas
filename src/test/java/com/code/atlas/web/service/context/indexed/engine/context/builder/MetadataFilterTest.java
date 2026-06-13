package com.code.atlas.web.service.context.indexed.engine.context.builder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.ProjectProjectType;
import com.code.atlas.web.domain.ProjectType;
import com.code.atlas.web.repository.ProjectProjectTypeRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataFilterTest {

    @Mock
    private ProjectProjectTypeRepository projectProjectTypeRepository;

    private MetadataFilter metadataFilter;

    @TempDir
    Path tempDir;

    private Project project;

    @BeforeEach
    void setUp() {
        metadataFilter = new MetadataFilter(projectProjectTypeRepository);
        project = new Project();
        project.setId(1L);
        project.setPath(tempDir.toString());
        stubProjectTypes("java,md");
    }

    @Test
    void shouldGenerateMetadata_whenExtensionAllowedAndPathClean_returnsTrue() {
        MetadataFilter.MetadataFilterContext context = metadataFilter.createContext(project);

        assertTrue(metadataFilter.shouldGenerateMetadata(context, file("src/main/Foo.java", "java")));
    }

    @Test
    void shouldGenerateMetadata_whenExtensionNotAllowed_returnsFalse() {
        MetadataFilter.MetadataFilterContext context = metadataFilter.createContext(project);

        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("src/main/app.ts", "ts")));
    }

    @Test
    void shouldGenerateMetadata_unionsExtensionsAcrossProjectTypes() {
        ProjectType backend = type(1L, "Backend", "java", null);
        ProjectType docs = type(2L, "Docs", "md,rst", null);
        stubAssignments(
                assignment(backend),
                assignment(docs)
        );

        MetadataFilter.MetadataFilterContext context = metadataFilter.createContext(project);

        assertTrue(metadataFilter.shouldGenerateMetadata(context, file("README.md", "md")));
        assertTrue(metadataFilter.shouldGenerateMetadata(context, file("guide.rst", "rst")));
        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("app.ts", "ts")));
    }

    @Test
    void shouldGenerateMetadata_whenExcludedSegmentAnywhere_returnsFalse() {
        MetadataFilter.MetadataFilterContext context = metadataFilter.createContext(project);

        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("frontend/node_modules/pkg/App.java", "java")));
        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("module/target/classes/App.java", "java")));
    }

    @Test
    void shouldGenerateMetadata_whenGitIgnored_returnsFalse() throws IOException {
        Files.writeString(tempDir.resolve(".gitignore"), """
                *.log
                generated/
                !generated/keep.java
                """);

        MetadataFilter.MetadataFilterContext context = metadataFilter.createContext(project);

        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("app.log", "log")));
        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("module/generated/output.java", "java")));
        assertTrue(metadataFilter.shouldGenerateMetadata(context, file("generated/keep.java", "java")));
    }

    @Test
    void shouldGenerateMetadata_whenNoGitIgnore_usesOnlyOtherRules() throws IOException {
        MetadataFilter.MetadataFilterContext context = metadataFilter.createContext(project);

        assertTrue(metadataFilter.shouldGenerateMetadata(context, file("notes.md", "md")));
        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("dist/App.java", "java")));
    }

    @Test
    void shouldGenerateMetadata_whenAllowedFileNameMatches_returnsTrueEvenWithoutExtension() {
        ProjectType backend = type(1L, "Backend", "java", "dockerfile,pom.xml");
        stubAssignments(assignment(backend));

        MetadataFilter.MetadataFilterContext context = metadataFilter.createContext(project);

        assertTrue(metadataFilter.shouldGenerateMetadata(context, file("Dockerfile", "")));
        assertTrue(metadataFilter.shouldGenerateMetadata(context, file("module/pom.xml", "xml")));
        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("build.gradle", "gradle")));
    }

    @Test
    void shouldGenerateMetadata_whenAllowedFileInExcludedPath_returnsFalse() {
        ProjectType backend = type(1L, "Backend", "java", "dockerfile");
        stubAssignments(assignment(backend));

        MetadataFilter.MetadataFilterContext context = metadataFilter.createContext(project);

        assertFalse(metadataFilter.shouldGenerateMetadata(context, file("node_modules/Dockerfile", "")));
    }

    private void stubProjectTypes(String allowedExtensions) {
        stubAssignments(assignment(type(10L, "Default", allowedExtensions, null)));
    }

    private void stubAssignments(ProjectProjectType... assignments) {
        when(projectProjectTypeRepository.findByProjectIdOrderByProjectTypeNameAsc(project.getId()))
                .thenReturn(List.of(assignments));
    }

    private ProjectProjectType assignment(ProjectType projectType) {
        ProjectProjectType assignment = new ProjectProjectType();
        assignment.setProjectType(projectType);
        return assignment;
    }

    private ProjectType type(Long id, String name, String allowedExtensions, String allowedFiles) {
        ProjectType projectType = new ProjectType();
        projectType.setId(id);
        projectType.setName(name);
        projectType.setAllowedExtensions(allowedExtensions);
        projectType.setAllowedFiles(allowedFiles);
        return projectType;
    }

    private ProjectFileIndex file(String path, String extension) {
        ProjectFileIndex file = new ProjectFileIndex();
        file.setFilePath(path);
        file.setFileExtension(extension);
        return file;
    }
}
