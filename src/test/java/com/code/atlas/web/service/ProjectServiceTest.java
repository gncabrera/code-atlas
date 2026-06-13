package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.repository.ProjectProjectTypeRepository;
import com.code.atlas.web.repository.ProjectRepository;
import com.code.atlas.web.repository.ProjectTypeRepository;
import com.code.atlas.web.service.dto.ProjectRequestDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectProjectTypeRepository projectProjectTypeRepository;

    @Mock
    private ProjectTypeRepository projectTypeRepository;

    @Mock
    private GitProcessRunner gitProcessRunner;

    @InjectMocks
    private ProjectService projectService;

    @TempDir
    Path tempDir;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setPath(tempDir.toString());
    }

    @Test
    void resolveDesignFileContent_whenDisabled_returnsEmpty() {
        project.setUseDesignFile(false);

        assertEquals("", projectService.resolveDesignFileContent(project));
    }

    @Test
    void resolveDesignFileContent_whenMissing_returnsPlaceholder() {
        project.setUseDesignFile(true);

        assertEquals("No DESIGN.md found", projectService.resolveDesignFileContent(project));
    }

    @Test
    void resolveDesignFileContent_whenPresent_returnsPrefixedContent() throws IOException {
        project.setUseDesignFile(true);
        String body = "Use Bootstrap 5 only.";
        Files.writeString(tempDir.resolve("DESIGN.md"), body);

        String content = projectService.resolveDesignFileContent(project);

        assertTrue(content.startsWith("DESIGN.md\n\n"));
        assertTrue(content.contains(body));
    }

    @Test
    void createProject_withoutProjectTypes_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRequestDto(tempDir.toString(), "Demo", "desc", false, true, List.of())
        );

        assertEquals("At least one project type is required.", ex.getMessage());
    }
}
