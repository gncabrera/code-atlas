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
import org.springframework.stereotype.Service;

@Service
public class CommitHelperService {

    private static final String COMMIT_TEMPLATE_PATH = "prompts/commit-message.md";
    private static final String DIFF_PLACEHOLDER = "{{DIFF}}";
    private static final String TRUNCATION_SUFFIX = "\n\n[diff truncated]";
    private static final String COMMIT_HELPER_NOTES = "Commit Helper";

    private final ProjectService projectService;
    private final AIModelService aiModelService;
    private final GitProcessRunner gitProcessRunner;
    private final String commitTemplate;

    public CommitHelperService(
            ProjectService projectService,
            AIModelService aiModelService,
            GitProcessRunner gitProcessRunner
    ) {
        this.projectService = projectService;
        this.aiModelService = aiModelService;
        this.gitProcessRunner = gitProcessRunner;
        this.commitTemplate = loadCommitTemplate();
    }

    public CommitHelperMetadataDto getMetadata() {
        return new CommitHelperMetadataDto(
                projectService.getAllProjects(),
                aiModelService.getEnabledModels()
        );
    }

    public String generateCommitMessage(Long projectId, Long aiModelId) {
        Project project = projectService.getProjectEntity(projectId);
        AIModel model = aiModelService.getModelEntity(aiModelId);
        Path projectRoot = resolveProjectRoot(project);

        assertGitRepository(projectRoot);
        String diff = collectWorkingTreeDiff(projectRoot);
        if (diff.isBlank()) {
            throw new IllegalArgumentException("No uncommitted changes found for project.");
        }

        String truncatedDiff = truncateDiffForModel(diff, model.getTokensPerMinute());
        String prompt = commitTemplate.replace(DIFF_PLACEHOLDER, truncatedDiff);
        ModelResponseDto response = aiModelService.sendToModel(project, model, prompt, COMMIT_HELPER_NOTES);
        return response.reponse().trim();
    }

    public void executeCommit(Long projectId, String message) {
        Path projectRoot = resolveProjectRoot(projectService.getProjectEntity(projectId));
        assertGitRepository(projectRoot);
        gitProcessRunner.run(projectRoot, List.of("git", "add", "-A"));
        gitProcessRunner.run(projectRoot, List.of("git", "commit", "-m", message.trim()));
    }

    public void executeCommitAndPush(Long projectId, String message) {
        Path projectRoot = resolveProjectRoot(projectService.getProjectEntity(projectId));
        assertGitRepository(projectRoot);
        gitProcessRunner.run(projectRoot, List.of("git", "add", "-A"));
        gitProcessRunner.run(projectRoot, List.of("git", "commit", "-m", message.trim()));
        gitProcessRunner.run(projectRoot, List.of("git", "push"));
    }

    String truncateDiffForModel(String diff, int tokensPerMinute) {
        if (tokensPerMinute <= 0) {
            return diff;
        }

        int wrapperTokens = AIModelService.estimateTokens(commitTemplate.replace(DIFF_PLACEHOLDER, ""));
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

    String collectWorkingTreeDiff(Path projectRoot) {
        StringBuilder diff = new StringBuilder();
        appendDiffSection(diff, gitProcessRunner.runAllowDiffExit(projectRoot, List.of("git", "diff", "HEAD")));

        String untrackedFiles = gitProcessRunner.run(
                projectRoot,
                List.of("git", "ls-files", "--others", "--exclude-standard")
        );
        if (!untrackedFiles.isBlank()) {
            String nullDevice = nullDevicePath();
            for (String file : untrackedFiles.split("\n")) {
                String relativePath = file.trim();
                if (relativePath.isEmpty()) {
                    continue;
                }
                appendDiffSection(
                        diff,
                        gitProcessRunner.runAllowDiffExit(
                                projectRoot,
                                List.of("git", "diff", "--no-index", nullDevice, relativePath)
                        )
                );
            }
        }

        return diff.toString().trim();
    }

    private void appendDiffSection(StringBuilder diff, String section) {
        if (section == null || section.isBlank()) {
            return;
        }
        if (!diff.isEmpty()) {
            diff.append("\n");
        }
        diff.append(section.trim());
    }

    private String nullDevicePath() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win") ? "NUL" : "/dev/null";
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
