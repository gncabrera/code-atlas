package com.code.atlas.web.service.context.indexed.engine.intent;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import org.springframework.stereotype.Service;

@Service
public class IntentEngine {
    private final IntentExtractionService intentExtractionService;

    public IntentEngine(IntentExtractionService intentExtractionService) {
        this.intentExtractionService = intentExtractionService;
    }
    public Intent extract(Project project, String userRequest, AIModel aiModel) {
        return intentExtractionService.extract(project, userRequest, aiModel);
    }

}
