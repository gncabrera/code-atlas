package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.CodeReviewMetadataDto;
import com.code.atlas.web.service.dto.CodeReviewResponseDto;
import com.code.atlas.web.service.dto.ModelResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CodeReviewService {

    private static final String CODE_REVIEW_TEMPLATE_PATH = "prompts/code-review.md";
    private static final String CODE_REVIEW_NOTES = "Code Review";
    private static final String AGENTS_FILE_KEY = "AGENTS_FILE";
    private static final String DESIGN_FILE_KEY = "DESIGN_FILE";
    private static final String FILES_KEY = "FILES";
    private static final String DIFF_KEY = "DIFF";
    private static final String TRUNCATION_SUFFIX = "\n\n[diff truncated]";

    private final ProjectService projectService;
    private final AIModelService aiModelService;
    private final GitProcessRunner gitProcessRunner;
    private final PromptFormatService promptFormatService;
    private final ObjectMapper objectMapper;
    private final String codeReviewTemplate;

    public CodeReviewService(
            ProjectService projectService,
            AIModelService aiModelService,
            GitProcessRunner gitProcessRunner,
            PromptFormatService promptFormatService,
            ObjectMapper objectMapper
    ) {
        this.projectService = projectService;
        this.aiModelService = aiModelService;
        this.gitProcessRunner = gitProcessRunner;
        this.promptFormatService = promptFormatService;
        this.objectMapper = objectMapper;
        this.codeReviewTemplate = loadCodeReviewTemplate();
    }

    public CodeReviewMetadataDto getMetadata(Long projectId) {
        List<String> branches = List.of();
        if (projectId != null) {
            Path projectRoot = resolveProjectRoot(projectService.getProjectEntity(projectId));
            assertGitRepository(projectRoot);
            branches = gitProcessRunner.listBranches(projectRoot);
        }
        return new CodeReviewMetadataDto(
                projectService.getAllProjects(),
                aiModelService.getEnabledModels(),
                branches
        );
    }

    public CodeReviewResponseDto runCodeReview(Long projectId, Long modelId, String branchA, String branchB) {
        String normalizedBranchA = branchA.trim();
        String normalizedBranchB = branchB.trim();
        if (normalizedBranchA.equals(normalizedBranchB)) {
            throw new IllegalArgumentException("Base and compare branches must be different.");
        }

        Project project = projectService.getProjectEntity(projectId);
        AIModel model = aiModelService.getModelEntity(modelId);
        Path projectRoot = resolveProjectRoot(project);
        assertGitRepository(projectRoot);

        String agentsFile = projectService.resolveAgentsFileContent(project);
        String designFile = projectService.resolveDesignFileContent(project);
        String files = String.join("\n", projectService.getProjectFiles(project));
        String diff = gitProcessRunner.diffBetweenBranches(projectRoot, normalizedBranchA, normalizedBranchB);
        if (diff.isBlank()) {
            throw new IllegalArgumentException("No differences found between the selected branches.");
        }

        String truncatedDiff = truncateDiffForModel(agentsFile, designFile, files, diff, model.getTokensPerMinute());
        String prompt = getFormatPrompt(agentsFile, designFile, files, truncatedDiff);

        ModelResponseDto response = aiModelService.sendToModel(project, model, prompt, CODE_REVIEW_NOTES);
        return parseReviewResponse(response.reponse());
    }

    String truncateDiffForModel(String agentsFile, String designFile, String files, String diff, int tokensPerMinute) {
        if (tokensPerMinute <= 0) {
            return diff;
        }

        int wrapperTokens = AIModelService.estimateTokens(
                getFormatPrompt(agentsFile, designFile, files, "")
        );
        int availableTokens = tokensPerMinute - wrapperTokens;
        if (availableTokens <= 0) {
            throw new IllegalArgumentException("Code review prompt template exceeds model tokensPerMinute limit.");
        }

        int maxDiffChars = availableTokens * 4;
        if (diff.length() <= maxDiffChars) {
            return diff;
        }

        int suffixLength = TRUNCATION_SUFFIX.length();
        if (maxDiffChars <= suffixLength) {
            return diff.substring(0, maxDiffChars);
        }

        return diff.substring(0, maxDiffChars - suffixLength) + TRUNCATION_SUFFIX;
    }

    private String getFormatPrompt(String agentsFile, String designFile, String files, String diff) {
        return promptFormatService.formatPrompt(codeReviewTemplate, Map.of(
                AGENTS_FILE_KEY, agentsFile,
                DESIGN_FILE_KEY, designFile,
                FILES_KEY, files,
                DIFF_KEY, diff
        ));
    }

    CodeReviewResponseDto parseReviewResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("AI model returned an empty code review response.");
        }
        try {
            String json = extractJson(rawResponse.trim());
            return objectMapper.readValue(json, CodeReviewResponseDto.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed parsing code review JSON: " + ex.getMessage(), ex);
        }
    }

    private String extractJson(String raw) {
        String stripped = stripCodeFences(raw);
        int start = stripped.indexOf('{');
        int end = stripped.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI response did not contain valid JSON.");
        }
        return stripped.substring(start, end + 1);
    }

    private String stripCodeFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }

    private Path resolveProjectRoot(Project project) {
        Path projectRoot = Paths.get(project.getPath()).normalize();
        if (!Files.exists(projectRoot)) {
            throw new IllegalArgumentException("Project path does not exist: " + projectRoot);
        }
        if (!Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Project path is not a directory: " + projectRoot);
        }
        return projectRoot;
    }

    private void assertGitRepository(Path projectRoot) {
        String result = gitProcessRunner.run(projectRoot, List.of("git", "rev-parse", "--is-inside-work-tree"));
        if (!"true".equalsIgnoreCase(result.trim())) {
            throw new IllegalArgumentException("Project path is not a git repository: " + projectRoot);
        }
    }

    private String loadCodeReviewTemplate() {
        try (InputStream inputStream = CodeReviewService.class.getClassLoader().getResourceAsStream(CODE_REVIEW_TEMPLATE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Code review template not found in classpath: " + CODE_REVIEW_TEMPLATE_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed reading code review template: " + CODE_REVIEW_TEMPLATE_PATH, ex);
        }
    }
}
