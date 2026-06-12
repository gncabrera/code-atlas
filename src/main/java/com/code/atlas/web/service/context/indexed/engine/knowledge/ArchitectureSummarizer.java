package com.code.atlas.web.service.context.indexed.engine.knowledge;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.*;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.RetrievedFile;
import java.util.List;
import java.util.Map;

import com.code.atlas.web.service.context.indexed.engine.intent.IntentEngine;
import com.code.atlas.web.service.context.indexed.engine.prompt.PromptHelper;
import org.springframework.stereotype.Service;

@Service
public class ArchitectureSummarizer {

    private static final String NOTES = "Indexed context: architecture summary";

    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final String template;
    private final PromptHelper promptHelper;
    private final ProjectService projectService;

    public ArchitectureSummarizer(
            AIModelService aiModelService,
            PromptFormatService promptFormatService, PromptHelper promptHelper, ProjectService projectService
    ) {
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.promptHelper = promptHelper;
        this.projectService = projectService;
        this.template = PromptTemplateService.load(PromptTemplate.CONTEXT_ARCHITECTURE_SUMMARY);
    }

    public String summarize(Project project, String userRequest, Intent intent, List<RetrievedFile> files, AIModel aiModel) {
        String agentsFileContent = projectService.resolveAgentsFileContent(project);
        String designFileContent = projectService.resolveDesignFileContent(project);
        String prompt = promptFormatService.formatPrompt(template, Map.of(
                "USER_REQUEST", userRequest,
                "INTENT", IntentEngine.formatIntent(intent),
                "RETRIEVED_FILES", promptHelper.formatFiles(files),
                "FILE_METADATA", promptHelper.formatMetadata(files),
                "AGENTS_FILE", agentsFileContent,
                "DESIGN_FILE", designFileContent
        ));
        return aiModelService.sendToModel(project, aiModel, prompt, NOTES, "Indexed context: architecture summary").reponse();
    }

}
