package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.CommitHelperMetadataDto;
import com.code.atlas.web.service.dto.ModelResponseDto;
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
public class CommitHelperService {

    private static final String COMMIT_TEMPLATE_PATH = "prompts/commit-message.md";
    private static final String DIFF_PARAMETER_KEY = "DIFF";
    private static final String TRUNCATION_SUFFIX = "\n\n[diff truncated]";
    private static final String COMMIT_HELPER_NOTES = "Commit Helper";

    private final ProjectService projectService;
    private final AIModelService aiModelService;
    private final GitProcessRunner gitProcessRunner;
    private final PromptFormatService promptFormatService;
    private final String commitTemplate;

    public CommitHelperService(
            ProjectService projectService,
            AIModelService aiModelService,
            GitProcessRunner gitProcessRunner,
            PromptFormatService promptFormatService
    ) {
        this.projectService = projectService;
        this.aiModelService = aiModelService;
        this.gitProcessRunner = gitProcessRunner;
        this.promptFormatService = promptFormatService;
        this.commitTemplate = loadCommitTemplate();
    }

    public CommitHelperMetadataDto getMetadata(Long projectId) {
        String currentBranch = projectId != null ? resolveCurrentBranch(projectId) : null;
        return new CommitHelperMetadataDto(
                projectService.getAllProjects(),
                aiModelService.getEnabledModels(),
                currentBranch
        );
    }

    String resolveCurrentBranch(Long projectId) {
        Path projectRoot = resolveProjectRoot(projectService.getProjectEntity(projectId));
        assertGitRepository(projectRoot);
        return gitProcessRunner.run(projectRoot, List.of("git", "rev-parse", "--abbrev-ref", "HEAD")).trim();
    }

    public String generateCommitMessage(Long projectId, Long aiModelId) {
        Project project = projectService.getProjectEntity(projectId);
        AIModel model = aiModelService.getModelEntity(aiModelId);
        Path projectRoot = resolveProjectRoot(project);

        assertGitRepository(projectRoot);
        String diff = gitProcessRunner.collectWorkingTreeDiff(projectRoot);
        if (diff.isBlank()) {
            throw new IllegalArgumentException("No uncommitted changes found for project.");
        }

        String truncatedDiff = truncateDiffForModel(diff, model.getTokensPerMinute());
        String prompt = promptFormatService.formatPrompt(commitTemplate, Map.of(DIFF_PARAMETER_KEY, truncatedDiff));
        ModelResponseDto response = aiModelService.sendToModel(project, model, prompt, COMMIT_HELPER_NOTES);
        return response.reponse().trim();
    }

    public void executeCommit(Long projectId, String message) {
        message = cleanMessage(message);
        Path projectRoot = resolveProjectRoot(projectService.getProjectEntity(projectId));
        assertGitRepository(projectRoot);
        gitProcessRunner.run(projectRoot, List.of("git", "add", "-A"));
        gitProcessRunner.run(projectRoot, List.of("git", "commit", "-m", message.trim()));
    }

    public void executeCommitAndPush(Long projectId, String message) {
        message = cleanMessage(message);
        Path projectRoot = resolveProjectRoot(projectService.getProjectEntity(projectId));
        assertGitRepository(projectRoot);
        gitProcessRunner.run(projectRoot, List.of("git", "add", "-A"));
        gitProcessRunner.run(projectRoot, List.of("git", "commit", "-m", message.trim()));
        gitProcessRunner.pushCurrentBranch(projectRoot);
    }

    private String cleanMessage(String message) {
        return message.replaceAll("\"", "'");
    }

    String truncateDiffForModel(String diff, int tokensPerMinute) {
        if (tokensPerMinute <= 0) {
            return diff;
        }

        int wrapperTokens = AIModelService.estimateTokens(
                promptFormatService.formatPrompt(commitTemplate, Map.of(DIFF_PARAMETER_KEY, ""))
        );
        int availableTokens = tokensPerMinute - wrapperTokens;
        if (availableTokens <= 0) {
            throw new IllegalArgumentException("Commit prompt template exceeds model tokensPerMinute limit.");
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

    private String loadCommitTemplate() {
        try (InputStream inputStream = CommitHelperService.class.getClassLoader().getResourceAsStream(COMMIT_TEMPLATE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Commit template not found in classpath: " + COMMIT_TEMPLATE_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed reading commit template: " + COMMIT_TEMPLATE_PATH, ex);
        }
    }
}
