package com.code.atlas.web.prompt;

import com.code.atlas.web.aimodel.AIModel;
import com.code.atlas.web.aimodel.AIModelService;
import com.code.atlas.web.project.Project;
import com.code.atlas.web.project.ProjectService;
import com.code.atlas.web.prompt.dto.BuildPreviewRequestDto;
import com.code.atlas.web.prompt.dto.BuildPreviewResponseDto;
import com.code.atlas.web.prompt.dto.SendPromptRequestDto;
import com.code.atlas.web.prompt.dto.SendPromptResponseDto;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private static final String CONTEXT_PLACEHOLDER = "Context: WIP";

    private final PromptTemplateService promptTemplateService;
    private final ProjectService projectService;
    private final AIModelService aiModelService;
    private final PromptHistoryRepository promptHistoryRepository;
    private final int timeoutSeconds;

    public PromptService(
            PromptTemplateService promptTemplateService,
            ProjectService projectService,
            AIModelService aiModelService,
            PromptHistoryRepository promptHistoryRepository,
            @Value("${codeatlas.gemini.timeout-seconds:60}") int timeoutSeconds
    ) {
        this.promptTemplateService = promptTemplateService;
        this.projectService = projectService;
        this.aiModelService = aiModelService;
        this.promptHistoryRepository = promptHistoryRepository;
        this.timeoutSeconds = timeoutSeconds;
    }

    public BuildPreviewResponseDto buildPreview(BuildPreviewRequestDto requestDto) {
        PromptMode mode = PromptMode.fromNullableValue(requestDto.promptMode());
        Project project = resolveProject(requestDto.projectId());
        String template = promptTemplateService.loadTemplate(mode);
        String agentsFileContent = resolveAgentsFileContent(project, requestDto.shouldSendAgentsFile());
        String generatedPrompt = template
                .replace("{{ USER_REQUEST }}", requestDto.userRequest().trim())
                .replace("{{ CONTEXT }}", CONTEXT_PLACEHOLDER)
                .replace("{{ AGENTS_FILE }}", agentsFileContent);
        return new BuildPreviewResponseDto(generatedPrompt, estimateTokens(generatedPrompt));
    }

    @Transactional
    public SendPromptResponseDto sendToModel(SendPromptRequestDto requestDto) {
        AIModel model = aiModelService.getModelEntity(requestDto.aiModelId());
        if (!model.isEnabled()) {
            throw new IllegalArgumentException("Selected AI model is disabled.");
        }
        String exactPrompt = requestDto.aiModelPrompt();
        int estimatedTokens = estimateTokens(exactPrompt);
        if (estimatedTokens > model.getTokensPerMinute()) {
            throw new IllegalArgumentException(
                    "Estimated tokens exceed model tokensPerMinute limit."
            );
        }

        PromptHistory history = new PromptHistory();
        history.setProject(resolveProject(requestDto.projectId()));
        history.setAiModel(model);
        history.setMode(PromptMode.fromNullableValue(requestDto.promptMode()).name());
        history.setShouldSendAgentsFile(requestDto.shouldSendAgentsFile());
        history.setEstimatedTokens(estimatedTokens);
        history.setRequestPrompt(exactPrompt);
        history.setStatus("PENDING");
        promptHistoryRepository.save(history);

        try {
            HttpOptions httpOptions = HttpOptions.builder()
                    .timeout(timeoutSeconds * 1000)
                    .build();
            Client client = Client.builder()
                    .apiKey(model.getApiKey())
                    .httpOptions(httpOptions)
                    .build();
            GenerateContentResponse response = client.models.generateContent(model.getName(), exactPrompt, null);
            String outputText = response.text();
            history.setResponsePrompt(outputText);
            history.setStatus("SUCCESS");
            promptHistoryRepository.save(history);
            return new SendPromptResponseDto(outputText, estimatedTokens);
        } catch (Exception ex) {
            ex.printStackTrace();
            history.setStatus("ERROR");
            history.setErrorMessage(ex.getMessage());
            promptHistoryRepository.save(history);
            throw new IllegalArgumentException("Failed calling AI model: " + ex.getMessage());
        }
    }

    private Project resolveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectService.getProjectEntity(projectId);
    }

    private String resolveAgentsFileContent(Project project, boolean shouldSendAgentsFile) {
        if (!shouldSendAgentsFile) {
            return "";
        }
        if (project == null) {
            return "";
        }
        if (!project.isUseAgentsFile()) {
            return "";
        }
        Path agentsPath = Path.of(project.getPath(), "AGENTS.md").normalize();
        if (!Files.exists(agentsPath)) {
            return "No AGENTS.md found";
        }
        try {
            return "AGENTS.md\n\n" + Files.readString(agentsPath);
        } catch (IOException ex) {
            return "No AGENTS.md found";
        }
    }

    private int estimateTokens(String input) {
        int characters = input == null ? 0 : input.length();
        return (characters + 3) / 4;
    }
}
