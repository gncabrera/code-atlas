package com.code.atlas.web.service.context.indexed.engine.knowledge;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.context.indexed.dto.ContextResult;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.RetrievedFile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowlegeEngine {
    private final ArchitectureSummarizer architectureSummarizer;
    private final MissingContextDetector missingContextDetector;

    public KnowlegeEngine(ArchitectureSummarizer architectureSummarizer, MissingContextDetector missingContextDetector) {
        this.architectureSummarizer = architectureSummarizer;
        this.missingContextDetector = missingContextDetector;
    }

    public String summarize(Project project, String userRequest, Intent intent, List<RetrievedFile> files, AIModel aiModel) {
        return architectureSummarizer.summarize(project, userRequest, intent, files, aiModel);
    }

    public Intent detectMissingContext(Project project, String userRequest, Intent intent,
                                       ContextResult contextResult, AIModel aiModel) {
        return missingContextDetector.detect(project, userRequest, intent, contextResult, aiModel);
    }
}
