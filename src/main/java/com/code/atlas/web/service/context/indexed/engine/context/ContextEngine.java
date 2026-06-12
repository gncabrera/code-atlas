package com.code.atlas.web.service.context.indexed.engine.context;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileMetadataIndex;
import com.code.atlas.web.service.context.indexed.dto.ContextResult;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.RetrievedFile;
import com.code.atlas.web.service.context.indexed.engine.context.retriever.DeterministicRetriever;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextEngine {
    private final DeterministicRetriever deterministicRetriever;

    public ContextEngine(DeterministicRetriever deterministicRetriever) {
        this.deterministicRetriever = deterministicRetriever;
    }

    public ContextResult retrieve(Project project, Intent intent) {
        return deterministicRetriever.retrieve(project, intent);
    }


}
