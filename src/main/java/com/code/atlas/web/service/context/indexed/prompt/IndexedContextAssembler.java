package com.code.atlas.web.service.context.indexed.prompt;

import com.code.atlas.web.service.context.indexed.GraphEdgeView;
import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.KnowledgeResult;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IndexedContextAssembler {

    private final int maxTotalChars;

    public IndexedContextAssembler(@Value("${codeatlas.context.indexed.max-total-chars:7000}") int maxTotalChars) {
        this.maxTotalChars = Math.max(1000, maxTotalChars);
    }

    public String assemble(Intent intent, KnowledgeResult knowledgeResult) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "# User Request Context", "Action: " + intent.action()
                + "\nEntities: " + intent.entities()
                + "\nOperations: " + intent.operations()
                + "\nLayers: " + intent.layers());
        appendSection(builder, "# Architecture Facts", knowledgeResult.architectureFacts());
        appendSection(builder, "# Dependency Graph", formatGraph(knowledgeResult.graph()));
        appendSection(builder, "# Relevant Files", formatFileList(knowledgeResult.files()));
        appendSection(builder, "# Code Snippets", formatSnippets(knowledgeResult.files()));
        //return limitSize(builder.toString().trim());
        return builder.toString().trim();
    }

    private void appendSection(StringBuilder builder, String title, String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        builder.append(title).append("\n\n").append(body.trim()).append("\n\n");
    }

    private String formatGraph(List<GraphEdgeView> graph) {
        if (graph.isEmpty()) {
            return "No dependency edges retrieved.";
        }
        StringBuilder builder = new StringBuilder();
        for (GraphEdgeView edge : graph) {
            builder.append("- ").append(edge.source())
                    .append(" -[").append(edge.relation()).append("]-> ")
                    .append(edge.target()).append('\n');
        }
        return builder.toString().trim();
    }

    private String formatFileList(List<RetrievedFile> files) {
        if (files.isEmpty()) {
            return "No files retrieved.";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (RetrievedFile file : files) {
            builder.append(index++).append(". ").append(file.relativePath())
                    .append(" (score=").append(file.score()).append(")\n");
            for (String reason : file.reasons()) {
                builder.append("   - ").append(reason).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private String formatSnippets(List<RetrievedFile> files) {
        if (files.isEmpty()) {
            return "No snippets available.";
        }
        StringBuilder builder = new StringBuilder();
        for (RetrievedFile file : files) {
            builder.append("## ").append(file.relativePath()).append("\n```")
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
