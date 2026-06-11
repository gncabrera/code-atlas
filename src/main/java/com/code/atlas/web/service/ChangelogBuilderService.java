package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.ChangelogBuilderRequestDto;
import com.code.atlas.web.service.dto.ChangelogBuilderResponseDto;
import com.code.atlas.web.service.dto.ChangelogExportFormat;
import com.code.atlas.web.service.dto.GitCommitDto;
import com.code.atlas.web.service.dto.ModelResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChangelogBuilderService {

    private static final String CHANGELOG_NOTES = "Changelog Builder";
    private static final int COMMIT_LOG_LIMIT = 100;
    private static final String DIFFS_KEY = "DIFFS";
    private static final String OUTPUT_FORMAT_INSTRUCTIONS_KEY = "OUTPUT_FORMAT_INSTRUCTIONS";

    private final GitProcessRunner gitProcessRunner;
    private final ProjectService projectService;
    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final ObjectMapper objectMapper;
    private final String changelogTemplate;

    public ChangelogBuilderService(
            GitProcessRunner gitProcessRunner,
            ProjectService projectService,
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            ObjectMapper objectMapper
    ) {
        this.gitProcessRunner = gitProcessRunner;
        this.projectService = projectService;
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.objectMapper = objectMapper;
        this.changelogTemplate = PromptTemplateService.load(PromptTemplate.CHANGELOG_BUILDER);
    }

    public List<String> getBranches(Long projectId) {
        Path projectRoot = projectService.resolveProjectRoot(projectService.getProjectEntity(projectId));
        gitProcessRunner.assertGitRepository(projectRoot);
        return gitProcessRunner.listBranches(projectRoot);
    }

    public List<GitCommitDto> getCommits(Long projectId, String branch) {
        Path projectRoot = projectService.resolveProjectRoot(projectService.getProjectEntity(projectId));
        gitProcessRunner.assertGitRepository(projectRoot);

        List<String[]> rawCommits = gitProcessRunner.listCommits(projectRoot, branch, COMMIT_LOG_LIMIT);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
        DateTimeFormatter yesterdayTimeFormat = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<GitCommitDto> commits = new ArrayList<>();
        for (String[] raw : rawCommits) {
            String hash = raw[0];
            String author = raw[1];
            ZonedDateTime commitTime = ZonedDateTime.parse(raw[2]);
            LocalDate commitDate = commitTime.toLocalDate();
            String subject = raw[3];

            String formattedDate;
            if (commitDate.equals(today)) {
                formattedDate = commitTime.format(timeFormat);
            } else if (commitDate.equals(yesterday)) {
                formattedDate = "Yesterday " + commitTime.format(yesterdayTimeFormat);
            } else {
                formattedDate = commitTime.format(dateFormat);
            }

            commits.add(new GitCommitDto(hash, author, formattedDate, subject));
        }
        return commits;
    }

    public ChangelogBuilderResponseDto generateChangelog(ChangelogBuilderRequestDto request) {
        Project project = projectService.getProjectEntity(request.projectId());
        AIModel model = aiModelService.getModelEntity(request.modelId());
        Path projectRoot = projectService.resolveProjectRoot(project);
        gitProcessRunner.assertGitRepository(projectRoot);

        StringBuilder diffsBuilder = new StringBuilder();
        for (String hash : request.commitHashes()) {
            String diff = gitProcessRunner.showCommit(projectRoot, hash);
            diffsBuilder.append("Commit: ").append(hash).append("\n")
                    .append(diff).append("\n\n");
        }

        String prompt = promptFormatService.formatPrompt(changelogTemplate, Map.of(
                DIFFS_KEY, diffsBuilder.toString(),
                OUTPUT_FORMAT_INSTRUCTIONS_KEY, resolveOutputFormatInstructions(request.exportFormat())
        ));

        ModelResponseDto response = aiModelService.sendToModel(project, model, prompt, CHANGELOG_NOTES);
        return parseChangelogResponse(response.reponse());
    }

    ChangelogBuilderResponseDto parseChangelogResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("AI model returned an empty changelog response.");
        }
        return JsonResponseExtractor.parseResponse(rawResponse, ChangelogBuilderResponseDto.class, objectMapper);
    }

    private String resolveOutputFormatInstructions(ChangelogExportFormat exportFormat) {
        return switch (exportFormat) {
            case MARKDOWN -> """
                    Format the changelog field as clean Markdown.
                    Use headings (## Features, ## Bug Fixes, etc.) and bullet lists.
                    Keep tone professional and concise.""";
            case SLACK -> """
                    Format the changelog field as Slack mrkdwn.
                    Use *bold* section titles and bullet lines starting with •.
                    Avoid Markdown headings (#); use Slack-friendly formatting only.""";
            case JSON -> """
                    Format the changelog field as a JSON string containing a structured object with keys:
                    version (optional string), sections (array of {title, items[]}), and summary (optional string).
                    The changelog field value must be valid JSON when parsed.""";
            case PLAIN_TEXT -> """
                    Format the changelog field as plain text with simple section labels and hyphen bullets.
                    No Markdown, no special formatting characters.""";
        };
    }
}
