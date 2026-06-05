package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.PromptOptimizerMode;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.BuildPreviewRequestDto;
import com.code.atlas.web.service.dto.BuildPreviewResponseDto;
import com.code.atlas.web.service.dto.ModelResponseDto;
import com.code.atlas.web.service.dto.SendPromptRequestDto;
import com.code.atlas.web.service.dto.SendPromptResponseDto;
import jakarta.transaction.Transactional;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private final PromptOptimizerModeService promptOptimizerModeService;
    private final ProjectService projectService;
    private final PromptContextService promptContextService;
    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final String contextStrategy;

    public PromptService(
            PromptOptimizerModeService promptOptimizerModeService,
            ProjectService projectService,
            PromptContextService promptContextService,
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            @Value("${codeatlas.context.strategy:deterministic}") String contextStrategy
    ) {
        this.promptOptimizerModeService = promptOptimizerModeService;
        this.projectService = projectService;
        this.promptContextService = promptContextService;
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.contextStrategy = contextStrategy == null ? "deterministic" : contextStrategy.trim().toLowerCase();
    }

    public BuildPreviewResponseDto buildPreview(BuildPreviewRequestDto requestDto) {
        PromptOptimizerMode mode = promptOptimizerModeService.getModeEntity(requestDto.promptModeId());
        if (mode.isHidden()) {
            throw new IllegalArgumentException("Selected prompt mode is not available.");
        }
        Project project = resolveProject(requestDto.projectId());
        String template = mode.getPrompt();
        String context = resolveContext(project, requestDto);
        String agentsFileContent = requestDto.shouldSendAgentsFile() ? projectService.resolveAgentsFileContent(project) : "";
        String designFileContent = requestDto.shouldSendDesignFile() ? projectService.resolveDesignFileContent(project) : "";
        Map<String, String> parameters = Map.of(
                "USER_REQUEST", requestDto.userRequest(),
                "CONTEXT", context,
                "AGENTS_FILE", agentsFileContent,
                "DESIGN_FILE", designFileContent
        );
        String generatedPrompt = promptFormatService.formatPrompt(template, parameters);
        return new BuildPreviewResponseDto(generatedPrompt, AIModelService.estimateTokens(generatedPrompt));
    }

    @Transactional
    public SendPromptResponseDto sendToModel(SendPromptRequestDto requestDto) {
        AIModel model = aiModelService.getModelEntity(requestDto.aiModelId());
        String exactPrompt = requestDto.aiModelPrompt();
        Project project = resolveProject(requestDto.projectId());
        String modeLabel = resolveModeLabel(requestDto.promptModeId());
        String notes = "shouldSendAgentsFile: " + requestDto.shouldSendAgentsFile()
                + ". shouldSendDesignFile: " + requestDto.shouldSendDesignFile()
                + ". PromptMode: " + modeLabel;
        ModelResponseDto modelResponseDto = aiModelService.sendToModel(project, model, exactPrompt, notes);
        return new SendPromptResponseDto(modelResponseDto.reponse(), modelResponseDto.estimatedTokens());
    }

    private String resolveContext(Project project, BuildPreviewRequestDto requestDto) {
        if ("indexed".equals(contextStrategy)) {
            if (requestDto.aiModelId() == null) {
                throw new IllegalArgumentException("AI model id is required when indexed context strategy is enabled.");
            }
            AIModel aiModel = aiModelService.getModelEntity(requestDto.aiModelId());
            return promptContextService.buildIndexedContext(project, requestDto.userRequest(), aiModel);
        }
        return promptContextService.buildDeterministicContext(project, requestDto.userRequest());
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
