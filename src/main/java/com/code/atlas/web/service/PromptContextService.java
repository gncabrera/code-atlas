package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.context.indexed.ContextPipelineLogger;
import com.code.atlas.web.service.context.indexed.IndexedContextService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromptContextService {

    private final IndexedContextService indexedContextService;
    private final ContextPipelineLogger pipelineLogger;

    public PromptContextService(
            IndexedContextService indexedContextService,
            ContextPipelineLogger pipelineLogger
    ) {
        this.indexedContextService = indexedContextService;
        this.pipelineLogger = pipelineLogger;
    }

    public String buildIndexedContext(Project project, String userRequest, AIModel aiModel) {
        if (aiModel == null) {
            throw new IllegalArgumentException("AI model is required for indexed context generation.");
        }
        try {
            return indexedContextService.build(project, userRequest, aiModel);
        } catch (Exception ex) {
            pipelineLogger.stepFailed(project, "build", 0, 0, "Indexed context build",
                    0L, "Falling back to deterministic context: " + ex.getMessage());
            return buildDeterministicContext(project, userRequest);
        }
    }

    public String buildDeterministicContext(Project project, String userRequest) {
        if (project == null) {
            return "## Relevant Files\n\nNo project selected. Context generation skipped.";
        }
        return "Deterministic Context not implemented yet";
    }

}
