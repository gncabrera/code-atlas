package com.code.atlas.web.service.context.indexed.engine.context.builder;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.PromptFormatService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            @Value("${codeatlas.context.offline-summary.max-files-per-chunk:10}") int maxFilesPerChunk,
            @Value("${codeatlas.context.offline-summary.max-tokens-per-file:30000}") int maxTokensPerFile,
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
            chunks.add(buildChunkBlock(projectRoot, files, files.subList(start, end), contentTokenBudget));
        }
        return chunks;
    }

    private String buildChunkBlock(
            Path projectRoot,
            List<ProjectFileIndex> allFiles,
            List<ProjectFileIndex> batch,
            int contentTokenBudget
    ) {
        Map<String, List<String>> neighborsByDirectory = buildNeighborsByDirectory(allFiles);
        List<FileBlock> blocks = new ArrayList<>();
        for (ProjectFileIndex entry : batch) {
            loadFileBlock(projectRoot, entry, neighborsByDirectory)
                    .ifPresent(blocks::add);
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

    private Optional<FileBlock> loadFileBlock(
            Path projectRoot,
            ProjectFileIndex entry,
            Map<String, List<String>> neighborsByDirectory
    ) {
        String filePath = entry.getFilePath();
        String diskContent = readFileContent(projectRoot, filePath);
        if (diskContent != null) {
            String relativeDirectory = parentDirectory(filePath);
            List<String> neighborFiles = neighborFilesFor(filePath, neighborsByDirectory);
            return Optional.of(FileBlock.withContent(filePath, relativeDirectory, neighborFiles, diskContent));
        }
        return Optional.empty();
    }

    private static Map<String, List<String>> buildNeighborsByDirectory(List<ProjectFileIndex> allFiles) {
        Map<String, List<String>> byDirectory = new HashMap<>();
        for (ProjectFileIndex entry : allFiles) {
            String filePath = entry.getFilePath();
            byDirectory
                    .computeIfAbsent(parentDirectory(filePath), ignored -> new ArrayList<>())
                    .add(fileName(filePath));
        }
        for (List<String> names : byDirectory.values()) {
            Collections.sort(names);
        }
        return byDirectory;
    }

    private static List<String> neighborFilesFor(String filePath, Map<String, List<String>> neighborsByDirectory) {
        String selfName = fileName(filePath);
        return neighborsByDirectory.getOrDefault(parentDirectory(filePath), List.of()).stream()
                .filter(name -> !name.equals(selfName))
                .toList();
    }

    private static String parentDirectory(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash < 0) {
            return "";
        }
        return filePath.substring(0, lastSlash);
    }

    private static String fileName(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash < 0 ? filePath : filePath.substring(lastSlash + 1);
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
        private final String relativeDirectory;
        private final List<String> neighborFiles;
        private String content;

        private FileBlock(String filePath, String relativeDirectory, List<String> neighborFiles, String content) {
            this.filePath = filePath;
            this.relativeDirectory = relativeDirectory;
            this.neighborFiles = neighborFiles;
            this.content = content;
        }

        static FileBlock withContent(
                String filePath,
                String relativeDirectory,
                List<String> neighborFiles,
                String content
        ) {
            return new FileBlock(filePath, relativeDirectory, List.copyOf(neighborFiles), content);
        }

        void truncateToTokens(int maxTokens, String suffix) {
            int metadataTokens = AIModelService.estimateTokens(formatBlock(""));
            int contentTokenBudget = Math.max(1, maxTokens - metadataTokens);
            int maxChars = Math.max(1, contentTokenBudget) * 4;
            if (content.length() <= maxChars) {
                return;
            }
            if (maxChars <= suffix.length()) {
                content = content.substring(0, maxChars);
                return;
            }
            content = content.substring(0, maxChars - suffix.length()) + suffix;
        }

        int estimatedTokens() {
            return AIModelService.estimateTokens(formattedBlock());
        }

        String formattedBlock() {
            return formatBlock(content);
        }

        private String formatBlock(String contentValue) {
            StringBuilder builder = new StringBuilder();
            builder.append("=== FILE START ===\n\n");
            builder.append("filePath: ").append(filePath).append("\n\n");
            builder.append("relativeDirectory: ").append(relativeDirectory).append("\n\n");
            builder.append("neighborFiles:\n");
            for (String neighbor : neighborFiles) {
                builder.append(neighbor).append('\n');
            }
            builder.append("\ncontent:\n");
            builder.append(contentValue);
            builder.append("\n\n=== FILE END ===");
            return builder.toString();
        }
    }
}
