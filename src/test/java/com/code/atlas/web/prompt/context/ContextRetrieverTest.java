package com.code.atlas.web.prompt.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.Project;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.code.atlas.web.service.ProjectIndexService;
import com.code.atlas.web.service.context.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class ContextRetrieverTest {

    @TempDir
    Path tempDir;

    @Test
    void retrievePrioritizesEndpointControllerAndRunsWithinBudget() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/code/atlas/web/profile");
        Files.createDirectories(srcDir);
        Files.writeString(
                srcDir.resolve("ProfileController.java"),
                """
                        @RestController
                        class ProfileController {
                            @DeleteMapping("/api/profile")
                            public void deleteProfile() {
                            }
                        }
                        """
        );
        Files.writeString(
                srcDir.resolve("UserService.java"),
                """
                        class UserService {
                            public void keepUser() {}
                        }
                        """
        );

        Project project = new Project();
        project.setId(1L);
        project.setPath(tempDir.toString());

        ProjectIndexService projectIndexService = Mockito.mock(ProjectIndexService.class);
        when(projectIndexService.isStale(any(Project.class))).thenReturn(false);
        when(projectIndexService.search(any(Project.class), any(ContextQuery.class), any(Integer.class)))
                .thenReturn(List.of());

        ContextRetriever retriever = new ContextRetriever(
                projectIndexService,
                new ContextSymbolExtractor(),
                new ContextSnippetExtractor(),
                4,
                20,
                800
        );
        ContextQuery query = new ContextQueryParser().parse("""
                Feature: soft delete users
                Endpoint: DELETE /api/profile
                Need: auth/session implications
                """);

        Instant started = Instant.now();
        List<ContextCandidate> candidates = retriever.retrieve(project, query);
        long elapsedMs = Duration.between(started, Instant.now()).toMillis();

        assertFalse(candidates.isEmpty());
        assertTrue(candidates.getFirst().relativePath().endsWith("ProfileController.java"));
        assertTrue(elapsedMs < 2_000, "Context retrieval should stay fast for small projects.");
    }
}
