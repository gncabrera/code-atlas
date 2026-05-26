package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.PromptOptimizerMode;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private final PromptOptimizerModeService promptOptimizerModeService;
    private final ProjectService projectService;
    private final PromptContextService promptContextService;
    private final AIModelService aiModelService;

    public PromptService(
            PromptOptimizerModeService promptOptimizerModeService,
            ProjectService projectService,
            PromptContextService promptContextService,
            AIModelService aiModelService
    ) {
        this.promptOptimizerModeService = promptOptimizerModeService;
        this.projectService = projectService;
        this.promptContextService = promptContextService;
        this.aiModelService = aiModelService;
    }

    public BuildPreviewResponseDto buildPreview(BuildPreviewRequestDto requestDto) {
        PromptOptimizerMode mode = promptOptimizerModeService.getModeEntity(requestDto.promptModeId());
        if (mode.isHidden()) {
            throw new IllegalArgumentException("Selected prompt mode is not available.");
        }
        Project project = resolveProject(requestDto.projectId());
        String template = mode.getPrompt();
        String context = promptContextService.buildContext(project, requestDto.userRequest());
        String agentsFileContent = requestDto.shouldSendAgentsFile() ? projectService.resolveAgentsFileContent(project) : "";
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
        String modeLabel = resolveModeLabel(requestDto.promptModeId());
        String notes = "shouldSendAgentsFile: " + requestDto.shouldSendAgentsFile() + ". PromptMode: " + modeLabel;
        ModelResponseDto modelResponseDto = aiModelService.sendToModel(project, model, exactPrompt, notes);
        return new SendPromptResponseDto(modelResponseDto.reponse(), modelResponseDto.estimatedTokens());
    }

    private String resolveModeLabel(Long promptModeId) {
        if (promptModeId == null) {
            return "UNKNOWN";
        }
        PromptOptimizerMode mode = promptOptimizerModeService.getModeEntity(promptModeId);
        return mode.getCode();
    }

    private Project resolveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectService.getProjectEntity(projectId);
    }
}
