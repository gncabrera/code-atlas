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
                "RETRIEVED_FILES", formatRetrievedFiles(files)
        ));
        return aiModelService.sendToModel(project, aiModel, prompt, NOTES, "Indexed context: architecture summary").reponse();
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

    private String formatRetrievedFiles(List<RetrievedFile> files) {
        if (files.isEmpty()) {
            return "No files retrieved.";
        }
        StringBuilder builder = new StringBuilder();
        for (RetrievedFile file : files) {
            builder.append("## ").append(file.relativePath())
                    .append(" (score=").append(file.score()).append(")\n");
            for (String reason : file.reasons()) {
                builder.append("- ").append(reason).append('\n');
            }
            if (!file.symbols().isEmpty()) {
                builder.append("symbols: ").append(String.join(", ", file.symbols())).append('\n');
            }
            builder.append("```").append(file.language()).append('\n');
            builder.append(file.snippet().isBlank() ? "// No snippet" : file.snippet()).append("\n```\n\n");
        }
        return builder.toString().trim();
    }
}
