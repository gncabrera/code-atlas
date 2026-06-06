package com.code.atlas.web.service.context.indexed.knowledge;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.PatternIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.JsonResponseExtractor;
import com.code.atlas.web.service.PromptFormatService;
import com.code.atlas.web.service.context.indexed.ContextResult;
import com.code.atlas.web.service.context.indexed.IndexedPromptLoader;
import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.MissingContext;
import com.code.atlas.web.service.context.indexed.MissingContextResponse;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import com.code.atlas.web.repository.PatternIndexRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MissingContextDetector {

    private static final String TEMPLATE_PATH = "prompts/context/missing-context.md";
    private static final String NOTES = "Indexed context: missing context detection";

    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final PatternIndexRepository patternIndexRepository;
    private final ObjectMapper objectMapper;
    private final String template;

    public MissingContextDetector(
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            PatternIndexRepository patternIndexRepository,
            ObjectMapper objectMapper
    ) {
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.patternIndexRepository = patternIndexRepository;
        this.objectMapper = objectMapper;
        this.template = IndexedPromptLoader.load(TEMPLATE_PATH);
    }

    public MissingContext detect(
            Project project,
            String userRequest,
            Intent intent,
            ContextResult contextResult,
            AIModel aiModel
    ) {
        String prompt = promptFormatService.formatPrompt(template, Map.of(
                "USER_REQUEST", userRequest,
                "INTENT", formatIntent(intent),
                "RETRIEVED_FILES", formatFiles(contextResult.files()),
                "PATTERN_HINTS", formatPatternHints(project)
        ));
        String raw = aiModelService.sendToModel(project, aiModel, prompt, NOTES, "Indexed context: missing context detection").reponse();
        MissingContextResponse response = JsonResponseExtractor.parseResponse(
                raw,
                MissingContextResponse.class,
                objectMapper
        );
        return new MissingContext(MissingContextCategoryNormalizer.normalize(response.missing()));
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

    private String formatPatternHints(Project project) {
        return patternIndexRepository.findByProjectId(project.getId()).stream()
                .map(PatternIndexEntry::getPattern)
                .collect(Collectors.joining(", "));
    }
}
