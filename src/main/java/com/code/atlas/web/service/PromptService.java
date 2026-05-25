package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.AIModelApiKey;
import com.code.atlas.web.domain.PromptHistory;
import com.code.atlas.web.domain.PromptMode;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.BuildPreviewRequestDto;
import com.code.atlas.web.service.dto.BuildPreviewResponseDto;
import com.code.atlas.web.service.dto.SendPromptRequestDto;
import com.code.atlas.web.service.dto.SendPromptResponseDto;
import com.code.atlas.web.repository.PromptHistoryRepository;
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

    private final PromptTemplateService promptTemplateService;
    private final ProjectService projectService;
    private final PromptContextService promptContextService;
    private final AIModelService aiModelService;
    private final int timeoutSeconds;
    private final PromptHistoryService promptHistoryService;

    public PromptService(
            PromptTemplateService promptTemplateService,
            ProjectService projectService,
            PromptContextService promptContextService,
            AIModelService aiModelService,
            @Value("${codeatlas.gemini.timeout-seconds:60}") int timeoutSeconds,
            PromptHistoryService promptHistoryService
    ) {
        this.promptTemplateService = promptTemplateService;
        this.projectService = projectService;
        this.promptContextService = promptContextService;
        this.aiModelService = aiModelService;
        this.promptHistoryService = promptHistoryService;
        this.timeoutSeconds = timeoutSeconds;
    }

    public BuildPreviewResponseDto buildPreview(BuildPreviewRequestDto requestDto) {
        PromptMode mode = PromptMode.fromNullableValue(requestDto.promptMode());
        Project project = resolveProject(requestDto.projectId());
        String template = promptTemplateService.loadTemplate(mode);
        String context = promptContextService.buildContext(project, requestDto.userRequest());
        String agentsFileContent = resolveAgentsFileContent(project, requestDto.shouldSendAgentsFile());
        String generatedPrompt = template
                .replace("{{ USER_REQUEST }}", requestDto.userRequest().trim())
                .replace("{{ CONTEXT }}", context)
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
        if (model.getTokensPerMinute() > 0 && estimatedTokens > model.getTokensPerMinute()) {
            throw new IllegalArgumentException(
                    "Estimated tokens exceed model tokensPerMinute limit."
            );
        }

        Project project = resolveProject(requestDto.projectId());
        String notes = "shouldSendAgentsFile: " + requestDto.shouldSendAgentsFile() + ". PromptMode: " + PromptMode.fromNullableValue(requestDto.promptMode()).name();
        PromptHistory history = promptHistoryService.create(project, model, exactPrompt, notes);

        try {
            String apiKeyValue = resolveApiKeyValue(model);
            HttpOptions httpOptions = HttpOptions.builder()
                    .timeout(timeoutSeconds * 1000)
                    .build();
            Client client = Client.builder()
                    .apiKey(apiKeyValue)
                    .httpOptions(httpOptions)
                    .build();
            GenerateContentResponse response = client.models.generateContent(model.getName(), exactPrompt, null);
            String outputText = response.text();
            promptHistoryService.success(history, outputText);
            return new SendPromptResponseDto(outputText, estimatedTokens);
        } catch (Exception ex) {
            promptHistoryService.error(history, ex);

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

    public static int estimateTokens(String input) {
        int characters = input == null ? 0 : input.length();
        return (characters + 3) / 4;
    }

    private String resolveApiKeyValue(AIModel model) {
        AIModelApiKey apiKey = model.getAiModelApiKey();
        if (apiKey == null) {
            throw new IllegalArgumentException("AI model has no API key assigned.");
        }
        if (!apiKey.isActive()) {
            throw new IllegalArgumentException("Assigned API key is inactive.");
        }
        String value = apiKey.getApiKey();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Assigned API key has no value.");
        }
        return value.trim();
    }
}
