package com.code.atlas.web.service.context.indexed.step.s02_retrieval;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.context.indexed.dto.ContextResult;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeterministicRetriever {

    private final int maxFiles;

    public DeterministicRetriever(
            @Value("${codeatlas.context.indexed.max-files:16}") int maxFiles
    ) {
        this.maxFiles = Math.max(1, maxFiles);
    }

    public ContextResult retrieve(Project project, Intent intent) {
        return new ContextResult(List.of());
    }

}
