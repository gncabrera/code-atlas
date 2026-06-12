package com.code.atlas.web.service.context.indexed.engine.knowledge;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.*;
import com.code.atlas.web.service.context.indexed.dto.ContextResult;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.MissingContextResponse;
import com.code.atlas.web.service.context.indexed.dto.RetrievedFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MissingContextDetector {

    private static final String NOTES = "Indexed context: missing context detection";

    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final ObjectMapper objectMapper;
    private final String template;

    public MissingContextDetector(
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            ObjectMapper objectMapper
    ) {
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.objectMapper = objectMapper;
        this.template = PromptTemplateService.load(PromptTemplate.CONTEXT_MISSING_CONTEXT);
    }

    public Intent detect(
            Project project,
            String userRequest,
            Intent intent,
            ContextResult contextResult,
            AIModel aiModel
    ) {
        // TODO: Agregar el summary / modificar MissingContext > Intent
        String prompt = promptFormatService.formatPrompt(template, Map.of(
                "USER_REQUEST", userRequest,
                "INTENT", formatIntent(intent),
                "RETRIEVED_FILES", formatFiles(contextResult.files())
        ));
        String raw = aiModelService.sendToModel(project, aiModel, prompt, NOTES, "Indexed context: missing context detection").reponse();
        MissingContextResponse response = JsonResponseExtractor.parseResponse(
                raw,
                MissingContextResponse.class,
                objectMapper
        );
        return null;
    }

    private String formatIntent(Intent intent) {
        return "action=" + intent.action()
                + ", entities=" + intent.entities()
                + ", operations=" + intent.operations()
                + ", layers=" + intent.layers()
                + ", frontendImpact=" + intent.frontendImpact();
    }

    private String formatFiles(List<RetrievedFile> files) {
        return files.stream().map(RetrievedFile::relativePath).collect(Collectors.joining("\n"));
    }
}
