package com.code.atlas.web.service.context.indexed.knowledge;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.FileSummaryIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.repository.FileSummaryIndexRepository;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.PromptFormatService;
import com.code.atlas.web.service.context.indexed.IndexedPromptLoader;
import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ArchitectureSummarizer {

    private static final String TEMPLATE_PATH = "prompts/context/architecture-summary.md";
    private static final String NOTES = "Indexed context: architecture summary";

    private final AIModelService aiModelService;
    private final PromptFormatService promptFormatService;
    private final FileSummaryIndexRepository fileSummaryIndexRepository;
    private final String template;

    public ArchitectureSummarizer(
            AIModelService aiModelService,
            PromptFormatService promptFormatService,
            FileSummaryIndexRepository fileSummaryIndexRepository
    ) {
        this.aiModelService = aiModelService;
        this.promptFormatService = promptFormatService;
        this.fileSummaryIndexRepository = fileSummaryIndexRepository;
        this.template = IndexedPromptLoader.load(TEMPLATE_PATH);
    }

    public String summarize(Project project, String userRequest, Intent intent, List<RetrievedFile> files, AIModel aiModel) {
        String prompt = promptFormatService.formatPrompt(template, Map.of(
                "USER_REQUEST", userRequest,
                "INTENT", formatIntent(intent),
                "FILE_SUMMARIES", formatSummaries(project, files),
                "RETRIEVED_FILES", files.stream().map(RetrievedFile::relativePath).collect(Collectors.joining("\n"))
        ));
        return aiModelService.sendToModel(project, aiModel, prompt, NOTES).reponse();
    }

    private String formatIntent(Intent intent) {
        return "action=" + intent.action()
                + ", entities=" + intent.entities()
                + ", operations=" + intent.operations()
                + ", layers=" + intent.layers();
    }

    private String formatSummaries(Project project, List<RetrievedFile> files) {
        StringBuilder builder = new StringBuilder();
        for (RetrievedFile file : files) {
            String summary = fileSummaryIndexRepository.findByProjectIdAndFilePath(project.getId(), file.relativePath())
                    .map(FileSummaryIndexEntry::getSummary)
                    .orElse("(no summary)");
            builder.append(file.relativePath()).append(": ").append(summary).append('\n');
        }
        return builder.toString().trim();
    }
}
