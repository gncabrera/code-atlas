package com.code.atlas.web.service.context.indexed.engine.intent;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.*;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.IntentExtractionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IntentExtractionService {

    private static final String NOTES = "Indexed context: intent extraction";

    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final ObjectMapper objectMapper;
    private final String template;

    public IntentExtractionService(
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            ObjectMapper objectMapper
    ) {
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.objectMapper = objectMapper;
        this.template = PromptTemplateService.load(PromptTemplate.CONTEXT_INTENT_EXTRACTION);
    }

    public Intent extract(Project project, String userRequest, AIModel aiModel) {
        String prompt = promptFormatService.formatPrompt(template, Map.of("USER_REQUEST", userRequest));
        String raw = aiModelService.sendToModel(project, aiModel, prompt, NOTES, "Indexed context: intent extraction").reponse();
        IntentExtractionResponse response = JsonResponseExtractor.parseResponse(
                raw,
                IntentExtractionResponse.class,
                objectMapper
        );
        return new Intent(
                response.action(),
                response.entities(),
                response.operations(),
                response.layers(),
                response.frontendImpact()
        );
    }
}
