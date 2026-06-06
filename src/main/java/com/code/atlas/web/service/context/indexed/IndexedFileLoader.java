package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.context.deterministic.ContextFileSupport;
import com.code.atlas.web.service.context.deterministic.ContextSymbolExtractor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IndexedFileLoader {

    private final ContextSymbolExtractor contextSymbolExtractor;
    private final int maxSnippetLines;
    private final int maxSnippetChars;

    public IndexedFileLoader(
            ContextSymbolExtractor contextSymbolExtractor,
            @Value("${codeatlas.context.max-snippet-lines:20}") int maxSnippetLines,
            @Value("${codeatlas.context.max-snippet-chars:1200}") int maxSnippetChars
    ) {
        this.contextSymbolExtractor = contextSymbolExtractor;
        this.maxSnippetLines = Math.max(6, maxSnippetLines);
        this.maxSnippetChars = Math.max(400, maxSnippetChars);
    }

    public RetrievedFile load(Project project, String relativePath, int score, List<String> reasons) {
        Path projectRoot = Path.of(project.getPath()).normalize();
        Path filePath = projectRoot.resolve(relativePath).normalize();
        if (!Files.isRegularFile(filePath)) {
            return new RetrievedFile(relativePath, "text", inferType(relativePath), score, reasons, List.of(), "");
        }
        try {
            String content = Files.readString(filePath);
            String extension = ContextFileSupport.extensionOf(filePath.getFileName().toString());
            List<String> symbols = contextSymbolExtractor.extractSymbols(content, extension, 8);
            String snippet = extractLeadingSnippet(content.lines().toList());
            return new RetrievedFile(
                    relativePath,
                    ContextFileSupport.languageByExtension(extension),
                    inferType(relativePath),
                    score,
                    reasons,
                    symbols,
                    snippet
            );
        } catch (IOException ex) {
            return new RetrievedFile(relativePath, "text", inferType(relativePath), score, reasons, List.of(), "");
        }
    }

    private String inferType(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (lower.contains("controller")) {
            return "controller";
        }
        if (lower.contains("service")) {
            return "service";
        }
        if (lower.contains("repository")) {
            return "repository";
        }
        if (lower.contains("migration") || lower.endsWith(".sql")) {
            return "migration";
        }
        if (lower.contains("entity") || lower.contains("/domain/")) {
            return "entity";
        }
        return "file";
    }

    private String extractLeadingSnippet(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder snippetBuilder = new StringBuilder();
        int endExclusive = Math.min(lines.size(), maxSnippetLines);
        for (int index = 0; index < endExclusive; index++) {
            String line = lines.get(index);
            if (snippetBuilder.length() + line.length() + 1 > maxSnippetChars) {
                snippetBuilder.append("\n// ... truncated ...");
                break;
            }
            snippetBuilder.append(line).append('\n');
        }
        return snippetBuilder.toString().trim();
    }
}
