package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.CommitHelperMetadataDto;
import com.code.atlas.web.service.dto.ModelResponseDto;
import com.code.atlas.web.service.dto.ProjectResponseDto;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommitHelperServiceTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private AIModelService aiModelService;

    @Mock
    private GitProcessRunner gitProcessRunner;

    @InjectMocks
    private CommitHelperService commitHelperService;

    @Captor
    private ArgumentCaptor<List<String>> commandCaptor;

    @TempDir
    Path tempDir;

    private Project project;
    private AIModel model;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setPath(tempDir.toString());
        project.setName("Test Project");

        model = new AIModel();
        model.setId(2L);
        model.setName("gemini-test");
        model.setEnabled(true);
        model.setTokensPerMinute(10_000);
    }

    @Test
    void getMetadata_withProjectId_returnsCurrentBranch() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(projectService.getAllProjects()).thenReturn(List.of(
                new ProjectResponseDto(1L, tempDir.toString(), "Test Project", null, false)
        ));
        when(aiModelService.getEnabledModels()).thenReturn(List.of());
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true", "feature/autocommit");

        CommitHelperMetadataDto metadata = commitHelperService.getMetadata(1L);

        assertEquals("feature/autocommit", metadata.currentBranch());
        verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "rev-parse", "--abbrev-ref", "HEAD")));
    }

    @Test
    void getMetadata_withoutProjectId_omitsCurrentBranch() {
        when(projectService.getAllProjects()).thenReturn(List.of());
        when(aiModelService.getEnabledModels()).thenReturn(List.of());

        CommitHelperMetadataDto metadata = commitHelperService.getMetadata(null);

        assertNull(metadata.currentBranch());
    }

    @Test
    void truncateDiff_respectsTokenBudget() {
        String largeDiff = "x".repeat(100_000);
        int tokenLimit = 2_000;
        String truncated = commitHelperService.truncateDiffForModel(largeDiff, tokenLimit);

        assertTrue(truncated.length() < largeDiff.length());
        assertTrue(truncated.endsWith("[diff truncated]"));
        assertTrue(AIModelService.estimateTokens(truncated) <= tokenLimit);
    }

    @Test
    void generateCommitMessage_callsGitDiffAndAiModel() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(aiModelService.getModelEntity(2L)).thenReturn(model);
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true", "");
        when(gitProcessRunner.runAllowDiffExit(any(Path.class), any())).thenReturn("diff line");
        when(aiModelService.sendToModel(eq(project), eq(model), any(), eq("Commit Helper")))
                .thenReturn(new ModelResponseDto("feat(api): add commit helper", 42));

        String message = commitHelperService.generateCommitMessage(1L, 2L);

        assertEquals("feat(api): add commit helper", message);
        verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "rev-parse", "--is-inside-work-tree")));
        verify(gitProcessRunner).runAllowDiffExit(any(Path.class), eq(List.of("git", "diff", "HEAD")));
        verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "ls-files", "--others", "--exclude-standard")));
        verify(aiModelService).sendToModel(eq(project), eq(model), any(), eq("Commit Helper"));
    }

    @Test
    void generateCommitMessage_rejectsEmptyDiff() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(aiModelService.getModelEntity(2L)).thenReturn(model);
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true", "");
        when(gitProcessRunner.runAllowDiffExit(any(Path.class), any())).thenReturn("   ");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commitHelperService.generateCommitMessage(1L, 2L)
        );

        assertEquals("No uncommitted changes found for project.", ex.getMessage());
    }

    @Test
    void collectWorkingTreeDiff_includesUntrackedFiles() {
        when(gitProcessRunner.runAllowDiffExit(any(Path.class), eq(List.of("git", "diff", "HEAD"))))
                .thenReturn("tracked diff");
        when(gitProcessRunner.run(any(Path.class), eq(List.of("git", "ls-files", "--others", "--exclude-standard"))))
                .thenReturn("new-file.txt");
        when(gitProcessRunner.runAllowDiffExit(
                any(Path.class),
                argThat(command -> command.size() == 5
                        && "git".equals(command.get(0))
                        && "diff".equals(command.get(1))
                        && "--no-index".equals(command.get(2))
                        && "new-file.txt".equals(command.get(4)))
        )).thenReturn("untracked diff");

        String diff = gitProcessRunner.collectWorkingTreeDiff(tempDir);

        assertEquals("tracked diff\nuntracked diff", diff);
    }

    @Test
    void executeCommit_runsAddAndCommit() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true", "", "");

        commitHelperService.executeCommit(1L, "feat: test commit");

        var inOrder = inOrder(gitProcessRunner);
        inOrder.verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "rev-parse", "--is-inside-work-tree")));
        inOrder.verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "add", "-A")));
        inOrder.verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "commit", "-m", "feat: test commit")));
    }

    @Test
    void executeCommitAndPush_runsPushAfterCommit() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true", "", "", "");

        commitHelperService.executeCommitAndPush(1L, "feat: test commit");

        var inOrder = inOrder(gitProcessRunner);
        inOrder.verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "rev-parse", "--is-inside-work-tree")));
        inOrder.verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "add", "-A")));
        inOrder.verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "commit", "-m", "feat: test commit")));
        inOrder.verify(gitProcessRunner).run(any(Path.class), eq(List.of("git", "push")));
    }
}
