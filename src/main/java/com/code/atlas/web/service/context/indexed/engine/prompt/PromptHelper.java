package com.code.atlas.web.service.context.indexed.engine.prompt;

import com.code.atlas.web.domain.ProjectFileMetadataIndex;
import com.code.atlas.web.repository.ProjectFileMetadataIndexRepository;
import com.code.atlas.web.service.context.indexed.dto.FileSummaryOfflineResponse;
import com.code.atlas.web.service.context.indexed.dto.RetrievedFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PromptHelper {

    private static final String NO_SUMMARY = "(no summary available)";

    private final ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository;
    private final ObjectMapper objectMapper;

    public PromptHelper(
            ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository,
            ObjectMapper objectMapper
    ) {
        this.projectFileMetadataIndexRepository = projectFileMetadataIndexRepository;
        this.objectMapper = objectMapper;
    }

    public String formatMetadata(List<RetrievedFile> files) {
        if (files.isEmpty()) {
            return "No files retrieved.";
        }
        StringBuilder builder = new StringBuilder();
        for (RetrievedFile retrievedFile : files) {
            builder.append(formatMetadataBlock(retrievedFile));
            builder.append("\n\n");
        }
        return builder.toString().trim();
    }

    private String formatMetadataBlock(RetrievedFile retrievedFile) {
        StringBuilder builder = new StringBuilder();
        builder.append("=== FILE START ===\n\n");
        builder.append("filePath: ").append(retrievedFile.file().getFilePath()).append("\n\n");
        builder.append("metadataJson:\n");
        builder.append(resolveMetadataJson(retrievedFile.file().getId()));
        builder.append("\n\n=== FILE END ===");
        return builder.toString();
    }

    private String resolveMetadataJson(Long fileIndexId) {
        return projectFileMetadataIndexRepository.findByFileId(fileIndexId)
                .map(ProjectFileMetadataIndex::getMetadataJson)
                .filter(json -> json != null && !json.isBlank())
                .orElse("(no metadata)");
    }

    public String formatSummaries(List<RetrievedFile> files) {
        if (files.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (RetrievedFile retrievedFile : files) {
            builder.append(formatSummaryBlock(retrievedFile));
            builder.append("\n\n");
        }
        return builder.toString().trim();
    }

    private String formatSummaryBlock(RetrievedFile retrievedFile) {
        String filePath = retrievedFile.file().getFilePath();
        String displayName = Paths.get(filePath).getFileName().toString();
        String summary = resolveSummary(retrievedFile.file().getId());
        return displayName + "\n" + summary;
    }

    private String resolveSummary(Long fileIndexId) {
        String metadataJson = projectFileMetadataIndexRepository.findByFileId(fileIndexId)
                .map(ProjectFileMetadataIndex::getMetadataJson)
                .filter(json -> json != null && !json.isBlank())
                .orElse(null);
        FileSummaryOfflineResponse.Metadata metadata = parseMetadata(metadataJson);
        if (metadata == null || metadata.summary() == null || metadata.summary().isBlank()) {
            return NO_SUMMARY;
        }
        return metadata.summary().trim();
    }

    private FileSummaryOfflineResponse.Metadata parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(metadataJson, FileSummaryOfflineResponse.Metadata.class);
        } catch (IOException ex) {
            return null;
        }
    }

    public String formatFiles(List<RetrievedFile> files) {
        if (files.isEmpty()) {
            return "No files retrieved.";
        }
        StringBuilder builder = new StringBuilder();
        for (RetrievedFile retrievedFile : files) {
            builder.append(formatFileBlock(retrievedFile));
            builder.append("\n\n");
        }
        return builder.toString().trim();
    }

    private String formatFileBlock(RetrievedFile retrievedFile) {
        StringBuilder builder = new StringBuilder();
        builder.append("=== FILE START ===\n\n");
        builder.append("filePath: ").append(retrievedFile.file().getFilePath()).append("\n\n");
        builder.append("language: ").append(retrievedFile.language()).append("\n\n");
        builder.append("type: ").append(retrievedFile.type()).append("\n\n");
        builder.append("score: ").append(retrievedFile.score()).append("\n\n");
        builder.append("reasons:\n");
        for (String reason : retrievedFile.reasons()) {
            builder.append(reason).append('\n');
        }
        builder.append("\nsymbols:\n");
        for (String symbol : retrievedFile.symbols()) {
            builder.append(symbol).append('\n');
        }
        builder.append("\nsnippet:\n");
        builder.append(retrievedFile.snippet().isBlank() ? "(no snippet)" : retrievedFile.snippet()).append("\n\n");
        builder.append("\n\n=== FILE END ===");
        return builder.toString();
    }


}
