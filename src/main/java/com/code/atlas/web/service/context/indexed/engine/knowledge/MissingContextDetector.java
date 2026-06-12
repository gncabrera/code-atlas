package com.code.atlas.web.service.context.indexed.engine.knowledge;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.PromptFormatService;
import com.code.atlas.web.service.PromptTemplate;
import com.code.atlas.web.service.PromptTemplateService;
import com.code.atlas.web.service.ProjectService;
import com.code.atlas.web.service.context.indexed.dto.ContextResult;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.engine.intent.IntentEngine;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.code.atlas.web.service.context.indexed.engine.prompt.PromptHelper;
import org.springframework.stereotype.Service;

@Service
public class MissingContextDetector {

    private static final String NOTES = "Indexed context: missing context detection";

    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final String template;
    private final ProjectService projectService;
    private final PromptHelper promptHelper;

    public MissingContextDetector(
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            ProjectService projectService, PromptHelper promptHelper
    ) {
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.projectService = projectService;
        this.promptHelper = promptHelper;
        this.template = PromptTemplateService.load(PromptTemplate.CONTEXT_MISSING_CONTEXT);
    }

    public Intent detect(
            Project project,
            String userRequest,
            Intent intent,
            ContextResult contextResult,
            AIModel aiModel
    ) {
        String agentsFileContent = projectService.resolveAgentsFileContent(project);
        String designFileContent = projectService.resolveDesignFileContent(project);

        String prompt = promptFormatService.formatPrompt(template, Map.of(
                "USER_REQUEST", userRequest,
                "INTENT", IntentEngine.formatIntent(intent),
                "RETRIEVED_FILES", promptHelper.formatFiles(contextResult.files()),
                "FILE_METADATA", promptHelper.formatMetadata(contextResult.files()),
                "AGENTS_FILE", agentsFileContent,
                "DESIGN_FILE", designFileContent
        ));
        Intent missingIntent = aiModelService.sendToModel(Intent.class, project, aiModel, prompt, NOTES, "Indexed context: missing context detection");

        return diffIntent(intent, missingIntent);
    }

    private Intent diffIntent(Intent original, Intent missing) {
        return new Intent(
                original.action(),
                diffList(missing.symbols(), original.symbols()),
                diffList(missing.concepts(), original.concepts()),
                diffList(missing.capabilities(), original.capabilities()),
                diffList(missing.architecturalRoles(), original.architecturalRoles()),
                diffList(missing.changeImpactAreas(), original.changeImpactAreas()),
                missing.frontendImpact() && !original.frontendImpact(),
                missing.confidence()
        );
    }

    private static List<String> diffList(List<String> missing, List<String> original) {
        Set<String> originalValues = new LinkedHashSet<>();
        for (String value : original) {
            if (value != null && !value.isBlank()) {
                originalValues.add(value.trim());
            }
        }
        List<String> diff = new ArrayList<>();
        for (String value : missing) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.trim();
            if (!originalValues.contains(trimmed)) {
                diff.add(trimmed);
            }
        }
        return diff;
    }
}
