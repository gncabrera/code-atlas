package com.code.atlas.web.service.context.indexed.engine.prompt;

import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.KnowledgeResult;
import org.springframework.stereotype.Service;

@Service
public class PromptEngine {
    private final IndexedContextAssembler indexedContextAssembler;

    public PromptEngine(IndexedContextAssembler indexedContextAssembler) {
        this.indexedContextAssembler = indexedContextAssembler;
    }

    public String assemble(Intent intent, KnowledgeResult knowledgeResult) {
        return indexedContextAssembler.assemble(intent, knowledgeResult);
    }
}
