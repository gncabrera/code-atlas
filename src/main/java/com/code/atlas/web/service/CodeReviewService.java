package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.CodeReviewMetadataDto;
import com.code.atlas.web.service.dto.CodeReviewRequestDto;
import com.code.atlas.web.service.dto.CodeReviewResponseDto;
import com.code.atlas.web.service.dto.ModelResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CodeReviewService {

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
    private final PromptContextService promptContextService;
    private final ObjectMapper objectMapper;
    private final String codeReviewTemplate;

    public CodeReviewService(
            ProjectService projectService,
            AIModelService aiModelService,
            GitProcessRunner gitProcessRunner,
            PromptFormatService promptFormatService,
            PromptContextService promptContextService,
            ObjectMapper objectMapper
    ) {
        this.projectService = projectService;
        this.aiModelService = aiModelService;
        this.gitProcessRunner = gitProcessRunner;
        this.promptFormatService = promptFormatService;
        this.promptContextService = promptContextService;
        this.objectMapper = objectMapper;
        this.codeReviewTemplate = PromptTemplateService.load(PromptTemplate.CODE_REVIEW);
    }

    public CodeReviewMetadataDto getMetadata(Long projectId) {
        List<String> branches = List.of();
        if (projectId != null) {
            Path projectRoot = projectService.resolveProjectRoot(projectService.getProjectEntity(projectId));
            gitProcessRunner.assertGitRepository(projectRoot);
            branches = gitProcessRunner.listBranches(projectRoot);
        }
        return new CodeReviewMetadataDto(
                projectService.getAllProjects(),
                aiModelService.getEnabledModels(),
                branches
        );
    }

    public CodeReviewResponseDto runCodeReview(CodeReviewRequestDto request) {
        if (request.currentChangesOnly()) {
            return runCurrentChangesCodeReview(request.projectId(), request.modelId());
        }
        return runBranchCodeReview(
                request.projectId(),
                request.modelId(),
                request.branchA(),
                request.branchB()
        );
    }

    public CodeReviewResponseDto runCurrentChangesCodeReview(Long projectId, Long modelId) {
        Project project = projectService.getProjectEntity(projectId);
        Path projectRoot = projectService.resolveProjectRoot(project);
        gitProcessRunner.assertGitRepository(projectRoot);

        String diff = gitProcessRunner.collectWorkingTreeDiff(projectRoot);
        if (diff.isBlank()) {
            throw new IllegalArgumentException("No uncommitted changes detected to review.");
        }

        return runCodeReview(projectId, modelId, diff);
    }

    public CodeReviewResponseDto runBranchCodeReview(Long projectId, Long modelId, String branchA, String branchB) {
        String normalizedBranchA = branchA.trim();
        String normalizedBranchB = branchB.trim();
        if (normalizedBranchA.equals(normalizedBranchB)) {
            throw new IllegalArgumentException("Base and compare branches must be different.");
        }

        Project project = projectService.getProjectEntity(projectId);
        Path projectRoot = projectService.resolveProjectRoot(project);
        gitProcessRunner.assertGitRepository(projectRoot);

        String diff = gitProcessRunner.diffBetweenBranches(projectRoot, normalizedBranchA, normalizedBranchB);
        if (diff.isBlank()) {
            throw new IllegalArgumentException("No differences found between the selected branches.");
        }

        return runCodeReview(projectId, modelId, diff);
    }

    public CodeReviewResponseDto runCodeReview(Long projectId, Long modelId, String diff) {
        Project project = projectService.getProjectEntity(projectId);
        AIModel model = aiModelService.getModelEntity(modelId);

        String agentsFile = projectService.resolveAgentsFileContent(project);
        String designFile = projectService.resolveDesignFileContent(project);
        String files = String.join("\n", projectService.getProjectFiles(project));
        if (diff.isBlank()) {
            throw new IllegalArgumentException("No changes found to review.");
        }

        String truncatedDiff = truncateDiffForModel(agentsFile, designFile, files, diff, model.getTokensPerMinute());
        String prompt = getFormatPrompt(agentsFile, designFile, files, truncatedDiff);

        ModelResponseDto response = aiModelService.sendToModel(project, model, prompt, CODE_REVIEW_NOTES);
        CodeReviewResponseDto parsed = parseReviewResponse(response.reponse());
        return enrichFindingsWithPrompts(project, parsed);
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
        return JsonResponseExtractor.parseResponse(rawResponse, CodeReviewResponseDto.class, objectMapper);
    }

    private CodeReviewResponseDto enrichFindingsWithPrompts(Project project, CodeReviewResponseDto response) {
        List<CodeReviewResponseDto.Finding> findings = response.findings();
        if (findings == null || findings.isEmpty()) {
            return response;
        }

        List<CodeReviewResponseDto.Finding> enrichedFindings = new ArrayList<>(findings.size());
        for (CodeReviewResponseDto.Finding finding : findings) {
            String context = resolveFindingContext(project, finding);
            String generatedPrompt = buildFindingPrompt(finding, context);
            enrichedFindings.add(new CodeReviewResponseDto.Finding(
                    finding.severity(),
                    finding.category(),
                    finding.title(),
                    finding.file(),
                    finding.line(),
                    finding.description(),
                    finding.impact(),
                    finding.suggestion(),
                    finding.suggestedPatch(),
                    generatedPrompt
            ));
        }
        return new CodeReviewResponseDto(response.summary(), enrichedFindings);
    }

    private String resolveFindingContext(Project project, CodeReviewResponseDto.Finding finding) {
        String file = finding.file();
        if (file == null || file.isBlank()) {
            return "";
        }
        try {
            return promptContextService.buildDeterministicContext(project, buildContextQuery(finding));
        } catch (Exception ex) {
            return "Context generation omitted: " + ex.getMessage();
        }
    }

    private String buildContextQuery(CodeReviewResponseDto.Finding finding) {
        StringBuilder query = new StringBuilder();
        appendContextLine(query, "file", finding.file());
        if (finding.line() != null) {
            appendContextLine(query, "line", String.valueOf(finding.line()));
        }
        appendContextLine(query, "title", finding.title());
        appendContextLine(query, "description", finding.description());
        appendContextLine(query, "impact", finding.impact());
        appendContextLine(query, "suggestion", finding.suggestion());
        return query.toString().trim();
    }

    private void appendContextLine(StringBuilder query, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!query.isEmpty()) {
            query.append('\n');
        }
        query.append(label).append(": ").append(value.trim());
    }

    private String buildFindingPrompt(CodeReviewResponseDto.Finding finding, String context) {
        String file = nullToEmpty(finding.file());
        String category = nullToEmpty(finding.category());
        String severity = nullToEmpty(finding.severity());
        String line = finding.line() != null ? String.valueOf(finding.line()) : "N/A";
        String title = nullToEmpty(finding.title());
        String description = nullToEmpty(finding.description());
        String impact = nullToEmpty(finding.impact());
        String suggestion = nullToEmpty(finding.suggestion());

        return """
                You are an expert developer. Fix the following issue in the file: %s

                Category: %s
                Severity: %s
                Line: %s

                Issue: %s

                Description: %s

                Impact: %s

                Suggestion: %s

                --- Deterministic Context ---
                %s

                Please provide a complete corrected version of the code or explicit instructions to fix this issue.
                """.formatted(file, category, severity, line, title, description, impact, suggestion, context).trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
