package com.code.atlas.web.service.context.indexed.engine.prompt;

import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.KnowledgeResult;
import org.springframework.stereotype.Service;

@Service
public class PromptEngine {
    private final PromptBuilder promptBuilder;

    public PromptEngine(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    public String assemble(Intent intent, KnowledgeResult knowledgeResult) {
        return promptBuilder.assemble(intent, knowledgeResult);
    }
}
