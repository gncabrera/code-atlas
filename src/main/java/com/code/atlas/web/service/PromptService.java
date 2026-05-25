package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.AIModelApiKey;
import com.code.atlas.web.domain.PromptHistory;
import com.code.atlas.web.domain.PromptMode;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.*;
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
    private final PromptHistoryService promptHistoryService;

    public PromptService(
            PromptTemplateService promptTemplateService,
            ProjectService projectService,
            PromptContextService promptContextService,
            AIModelService aiModelService,
            PromptHistoryService promptHistoryService
    ) {
        this.promptTemplateService = promptTemplateService;
        this.projectService = projectService;
        this.promptContextService = promptContextService;
        this.aiModelService = aiModelService;
        this.promptHistoryService = promptHistoryService;
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
        return new BuildPreviewResponseDto(generatedPrompt, AIModelService.estimateTokens(generatedPrompt));
    }

    @Transactional
    public SendPromptResponseDto sendToModel(SendPromptRequestDto requestDto) {
        AIModel model = aiModelService.getModelEntity(requestDto.aiModelId());
        String exactPrompt = requestDto.aiModelPrompt();
        Project project = resolveProject(requestDto.projectId());
        String notes = "shouldSendAgentsFile: " + requestDto.shouldSendAgentsFile() + ". PromptMode: " + PromptMode.fromNullableValue(requestDto.promptMode()).name();
        ModelResponseDto modelResponseDto = aiModelService.sendToModel(project, model, exactPrompt, notes);
        return new SendPromptResponseDto(modelResponseDto.reponse(), modelResponseDto.estimatedTokens());
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


}
