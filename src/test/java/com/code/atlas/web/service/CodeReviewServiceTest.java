package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.CodeReviewMetadataDto;
import com.code.atlas.web.service.dto.CodeReviewRequestDto;
import com.code.atlas.web.service.dto.CodeReviewResponseDto;
import com.code.atlas.web.service.dto.ModelResponseDto;
import com.code.atlas.web.service.dto.ProjectResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodeReviewServiceTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private AIModelService aiModelService;

    @Mock
    private GitProcessRunner gitProcessRunner;

    @Spy
    private PromptFormatService promptFormatService = new PromptFormatService();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CodeReviewService codeReviewService;

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
    void getMetadata_withProjectId_returnsBranches() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(projectService.getAllProjects()).thenReturn(List.of(
                new ProjectResponseDto(1L, tempDir.toString(), "Test Project", null, false, true)
        ));
        when(aiModelService.getEnabledModels()).thenReturn(List.of());
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true");
        when(gitProcessRunner.listBranches(any(Path.class))).thenReturn(List.of("main", "origin/main"));

        CodeReviewMetadataDto metadata = codeReviewService.getMetadata(1L);

        assertEquals(List.of("main", "origin/main"), metadata.branches());
        verify(gitProcessRunner).listBranches(any(Path.class));
    }

    @Test
    void getMetadata_withoutProjectId_omitsBranches() {
        when(projectService.getAllProjects()).thenReturn(List.of());
        when(aiModelService.getEnabledModels()).thenReturn(List.of());

        CodeReviewMetadataDto metadata = codeReviewService.getMetadata(null);

        assertTrue(metadata.branches().isEmpty());
    }

    @Test
    void runBranchCodeReview_rejectsSameBranch() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> codeReviewService.runBranchCodeReview(1L, 2L, "main", "main")
        );

        assertEquals("Base and compare branches must be different.", ex.getMessage());
    }

    @Test
    void truncateDiff_respectsTokenBudget() {
        String largeDiff = "x".repeat(100_000);
        String truncated = codeReviewService.truncateDiffForModel("", "", "", largeDiff, 2_000);

        assertTrue(truncated.length() < largeDiff.length());
        assertTrue(truncated.endsWith("[diff truncated]"));
        assertTrue(AIModelService.estimateTokens(truncated) <= 2_000);
    }

    @Test
    void parseReviewResponse_rejectsEmptyResponse() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> codeReviewService.parseReviewResponse("   ")
        );

        assertEquals("AI model returned an empty code review response.", ex.getMessage());
    }

    @Test
    void parseReviewResponse_stripsCodeFenceAndParsesJson() {
        String raw = """
                ```json
                {
                  "summary": {
                    "score": 8,
                    "risk": "LOW",
                    "mainConcerns": ["Missing tests"]
                  },
                  "findings": []
                }
                ```
                """;

        CodeReviewResponseDto response = codeReviewService.parseReviewResponse(raw);

        assertEquals(8, response.summary().score());
        assertEquals("LOW", response.summary().risk());
        assertEquals(List.of("Missing tests"), response.summary().mainConcerns());
        assertTrue(response.findings().isEmpty());
    }

    @Test
    void runBranchCodeReview_callsGitDiffAndAiModel() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(aiModelService.getModelEntity(2L)).thenReturn(model);
        when(projectService.resolveAgentsFileContent(project)).thenReturn("agents");
        when(projectService.resolveDesignFileContent(project)).thenReturn("");
        when(projectService.getProjectFiles(project)).thenReturn(List.of("src/Main.java"));
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true");
        when(gitProcessRunner.diffBetweenBranches(any(Path.class), eq("main"), eq("feature/x")))
                .thenReturn("diff content");
        when(aiModelService.sendToModel(eq(project), eq(model), any(), eq("Code Review")))
                .thenReturn(new ModelResponseDto("""
                        {"summary":{"score":7,"risk":"MEDIUM","mainConcerns":[]},"findings":[]}
                        """, 10));

        CodeReviewResponseDto result = codeReviewService.runBranchCodeReview(1L, 2L, "main", "feature/x");

        assertEquals(7, result.summary().score());
        assertEquals("MEDIUM", result.summary().risk());
        verify(gitProcessRunner).diffBetweenBranches(any(Path.class), eq("main"), eq("feature/x"));
        verify(aiModelService).sendToModel(eq(project), eq(model), any(), eq("Code Review"));
    }

    @Test
    void runCodeReview_currentChangesOnly_usesWorkingTreeDiff() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(aiModelService.getModelEntity(2L)).thenReturn(model);
        when(projectService.resolveAgentsFileContent(project)).thenReturn("agents");
        when(projectService.resolveDesignFileContent(project)).thenReturn("");
        when(projectService.getProjectFiles(project)).thenReturn(List.of("src/Main.java"));
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true");
        when(gitProcessRunner.collectWorkingTreeDiff(any(Path.class))).thenReturn("uncommitted diff");
        when(aiModelService.sendToModel(eq(project), eq(model), any(), eq("Code Review")))
                .thenReturn(new ModelResponseDto("""
                        {"summary":{"score":9,"risk":"LOW","mainConcerns":[]},"findings":[]}
                        """, 10));

        CodeReviewRequestDto request = new CodeReviewRequestDto(1L, 2L, null, null, true);
        CodeReviewResponseDto result = codeReviewService.runCodeReview(request);

        assertEquals(9, result.summary().score());
        assertEquals("LOW", result.summary().risk());
        verify(gitProcessRunner).collectWorkingTreeDiff(any(Path.class));
        verify(aiModelService).sendToModel(eq(project), eq(model), any(), eq("Code Review"));
    }

    @Test
    void runCodeReview_currentChangesOnly_rejectsEmptyWorkingTreeDiff() {
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(gitProcessRunner.run(any(Path.class), any())).thenReturn("true");
        when(gitProcessRunner.collectWorkingTreeDiff(any(Path.class))).thenReturn("");

        CodeReviewRequestDto request = new CodeReviewRequestDto(1L, 2L, null, null, true);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> codeReviewService.runCodeReview(request)
        );

        assertEquals("No uncommitted changes detected to review.", ex.getMessage());
    }
}
