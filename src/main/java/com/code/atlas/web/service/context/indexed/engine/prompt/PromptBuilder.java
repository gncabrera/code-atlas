package com.code.atlas.web.service.context.indexed.engine.prompt;

import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.KnowledgeResult;
import com.code.atlas.web.service.context.indexed.dto.RetrievedFile;

import java.util.List;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilder {

    private final int maxTotalChars;
    private final PromptHelper promptHelper;

    public PromptBuilder(
            @Value("${codeatlas.context.indexed.max-total-chars:7000}") int maxTotalChars,
            PromptHelper promptHelper
    ) {
        this.maxTotalChars = Math.max(1000, maxTotalChars);
        this.promptHelper = promptHelper;
    }

    public String assemble(Intent intent, KnowledgeResult knowledgeResult) {
        StringBuilder builder = new StringBuilder();
        List<RetrievedFile> files = knowledgeResult.files();
        appendSection(builder, "# Request Analysis", formatRequestAnalysis(intent));
        appendSection(builder, "# Architecture Facts", knowledgeResult.architectureFacts());
        appendSection(builder, "# Relevant Files", formatRelevantFiles(files));
        appendSection(builder, "# File Summaries", promptHelper.formatSummaries(files));
        appendSection(builder, "# Code Snippets", formatSnippets(files));
        return builder.toString().trim();
    }

    private void appendSection(StringBuilder builder, String title, String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        builder.append(title).append("\n\n").append(body.trim()).append("\n\n");
    }

    private String formatRequestAnalysis(Intent intent) {
        StringBuilder builder = new StringBuilder();
        builder.append("Action: ").append(intent.action());
        if (!intent.symbols().isEmpty()) {
            builder.append("\nPrimary symbols: ").append(String.join(", ", intent.symbols()));
        }
        if (!intent.concepts().isEmpty()) {
            builder.append("\nPrimary concepts: ").append(String.join(", ", intent.concepts()));
        }
        if (!intent.capabilities().isEmpty()) {
            builder.append("\nCapabilities: ").append(String.join(", ", intent.capabilities()));
        }
        return builder.toString().trim();
    }

    private String formatRelevantFiles(List<RetrievedFile> files) {
        if (files.isEmpty()) {
            return "No files retrieved.";
        }
        StringJoiner joiner = new StringJoiner("\n\n");
        for (RetrievedFile file : files) {
            joiner.add(formatRelevantFile(file));
        }
        return joiner.toString();
    }

    private String formatRelevantFile(RetrievedFile file) {
        StringBuilder builder = new StringBuilder();
        builder.append(file.file().getFilePath());
        if (!file.type().isBlank()) {
            builder.append("\n- type: ").append(file.type());
        }
        builder.append("\n- score: ").append(file.score());
        if (!file.reasons().isEmpty()) {
            builder.append("\n- matched:");
            for (String reason : file.reasons()) {
                builder.append("\n  - ").append(reason);
            }
        }
        return builder.toString();
    }

    private String formatSnippets(List<RetrievedFile> files) {
        if (files.isEmpty()) {
            return "No snippets available.";
        }
        StringBuilder builder = new StringBuilder();
        for (RetrievedFile file : files) {
            builder.append("## ").append(file.file().getFilePath()).append("\n```")
                    .append(file.language()).append('\n');
            builder.append(file.snippet().isBlank() ? "// No snippet" : file.snippet()).append("\n```\n\n");
        }
        return builder.toString().trim();
    }

    private String limitSize(String content) {
        if (content.length() <= maxTotalChars) {
            return content;
        }
        return content.substring(0, maxTotalChars - 40) + "\n\n... [context truncated]";
    }
}
