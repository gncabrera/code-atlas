package com.code.atlas.web.service.context.indexed.offline;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.PromptFormatService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OfflineFileSummaryChunkBuilder {

    private static final String CONTENT_TRUNCATION_SUFFIX = "\n[content truncated]";
    private static final String UNAVAILABLE_NOTE = "(content unavailable — infer summary from path and symbols only)";

    private final PromptFormatService promptFormatService;
    private final int maxFilesPerChunk;
    private final int maxTokensPerFile;
    private final double promptBudgetRatio;

    public OfflineFileSummaryChunkBuilder(
            PromptFormatService promptFormatService,
            @Value("${codeatlas.context.offline-summary.max-files-per-chunk:5}") int maxFilesPerChunk,
            @Value("${codeatlas.context.offline-summary.max-tokens-per-file:2000}") int maxTokensPerFile,
            @Value("${codeatlas.context.offline-summary.prompt-budget-ratio:0.8}") double promptBudgetRatio
    ) {
        this.promptFormatService = promptFormatService;
        this.maxFilesPerChunk = Math.max(1, maxFilesPerChunk);
        this.maxTokensPerFile = Math.max(1, maxTokensPerFile);
        this.promptBudgetRatio = clampRatio(promptBudgetRatio);
    }

    public List<String> buildChunks(
            Project project,
            List<ProjectFileIndex> files,
            AIModel model,
            String summaryTemplate
    ) {
        if (files.isEmpty()) {
            return List.of();
        }
        Path projectRoot = Path.of(project.getPath()).normalize();
        int contentTokenBudget = resolveContentTokenBudget(model, summaryTemplate);
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < files.size(); start += maxFilesPerChunk) {
            int end = Math.min(start + maxFilesPerChunk, files.size());
            chunks.add(buildChunkBlock(projectRoot, files.subList(start, end), contentTokenBudget));
        }
        return chunks;
    }

    private String buildChunkBlock(Path projectRoot, List<ProjectFileIndex> batch, int contentTokenBudget) {
        List<FileBlock> blocks = new ArrayList<>();
        for (ProjectFileIndex entry : batch) {
            blocks.add(loadFileBlock(projectRoot, entry));
        }
        applyTokenBudget(blocks, contentTokenBudget);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                builder.append("\n\n");
            }
            builder.append(blocks.get(i).formattedBlock());
        }
        return builder.toString();
    }

    private FileBlock loadFileBlock(Path projectRoot, ProjectFileIndex entry) {
        String filePath = entry.getFilePath();
        String diskContent = readFileContent(projectRoot, filePath);
        if (diskContent != null) {
            return FileBlock.withContent(filePath, diskContent);
        }
        String indexedContent = normalizeIndexedContent(entry.getSearchableText());
        if (indexedContent != null) {
            return FileBlock.withContent(filePath, indexedContent);
        }
        return FileBlock.withSymbolsOnly(filePath, entry.getSymbols(), UNAVAILABLE_NOTE);
    }

    private String normalizeIndexedContent(String searchableText) {
        if (searchableText == null || searchableText.isBlank()) {
            return null;
        }
        return searchableText.trim();
    }

    private String readFileContent(Path projectRoot, String relativePath) {
        Path filePath = projectRoot.resolve(relativePath).normalize();
        if (!filePath.startsWith(projectRoot) || !Files.isRegularFile(filePath)) {
            return null;
        }
        try {
            return Files.readString(filePath);
        } catch (IOException ex) {
            return null;
        }
    }

    private void applyTokenBudget(List<FileBlock> blocks, int contentTokenBudget) {
        if (blocks.isEmpty()) {
            return;
        }
        int perFileCap = Math.min(maxTokensPerFile, Math.max(1, contentTokenBudget / blocks.size()));
        for (FileBlock block : blocks) {
            block.truncateToTokens(perFileCap, CONTENT_TRUNCATION_SUFFIX);
        }
        int totalTokens = blocks.stream().mapToInt(FileBlock::estimatedTokens).sum();
        if (totalTokens <= contentTokenBudget) {
            return;
        }
        int targetPerFile = Math.max(1, contentTokenBudget / blocks.size());
        for (FileBlock block : blocks) {
            block.truncateToTokens(targetPerFile, CONTENT_TRUNCATION_SUFFIX);
        }
    }

    private int resolveContentTokenBudget(AIModel model, String summaryTemplate) {
        int promptBudget = model.getTokensPerMinute() > 0
                ? (int) Math.floor(model.getTokensPerMinute() * promptBudgetRatio)
                : maxTokensPerFile * maxFilesPerChunk;
        int wrapperTokens = AIModelService.estimateTokens(
                promptFormatService.formatPrompt(summaryTemplate, Map.of("FILES", ""))
        );
        int contentBudget = promptBudget - wrapperTokens;
        if (contentBudget <= 0) {
            throw new IllegalArgumentException("File summary prompt template exceeds model tokensPerMinute limit.");
        }
        return contentBudget;
    }

    private static double clampRatio(double ratio) {
        if (ratio <= 0) {
            return 0.8;
        }
        return Math.min(ratio, 1.0);
    }

    private static final class FileBlock {
        private final String filePath;
        private String body;

        private FileBlock(String filePath, String body) {
            this.filePath = filePath;
            this.body = body;
        }

        static FileBlock withContent(String filePath, String content) {
            return new FileBlock(filePath, content);
        }

        static FileBlock withSymbolsOnly(String filePath, String symbols, String unavailableNote) {
            String safeSymbols = symbols == null || symbols.isBlank() ? "(none)" : symbols.trim();
            String body = unavailableNote + "\nsymbols: " + safeSymbols;
            return new FileBlock(filePath, body);
        }

        void truncateToTokens(int maxTokens, String suffix) {
            int maxChars = Math.max(1, maxTokens) * 4;
            if (body.length() <= maxChars) {
                return;
            }
            if (maxChars <= suffix.length()) {
                body = body.substring(0, maxChars);
                return;
            }
            body = body.substring(0, maxChars - suffix.length()) + suffix;
        }

        int estimatedTokens() {
            return AIModelService.estimateTokens(formattedBlock());
        }

        String formattedBlock() {
            return filePath + ":\n---\n" + body + "\n---";
        }
    }
}
