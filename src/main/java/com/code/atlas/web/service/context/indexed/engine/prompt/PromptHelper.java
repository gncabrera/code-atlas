package com.code.atlas.web.service.context.indexed.engine.prompt;

import com.code.atlas.web.domain.ProjectFileMetadataIndex;
import com.code.atlas.web.repository.ProjectFileMetadataIndexRepository;
import com.code.atlas.web.service.context.indexed.dto.RetrievedFile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptHelper {

    private final ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository;

    public PromptHelper(ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository) {
        this.projectFileMetadataIndexRepository = projectFileMetadataIndexRepository;
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
}
