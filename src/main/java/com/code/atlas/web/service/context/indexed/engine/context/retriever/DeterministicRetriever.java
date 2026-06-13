package com.code.atlas.web.service.context.indexed.engine.context.retriever;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.ProjectFileMetadataIndex;
import com.code.atlas.web.helper.FileHelper;
import com.code.atlas.web.repository.ProjectFileMetadataIndexRepository;
import com.code.atlas.web.service.context.indexed.dto.ContextResult;
import com.code.atlas.web.service.context.indexed.dto.FileSummaryOfflineResponse;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.RetrievedFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeterministicRetriever {

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.50;

    private final ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository;
    private final ObjectMapper objectMapper;
    private final int maxFiles;
    private final int maxSnippetLines;
    private final int maxSnippetChars;

    public DeterministicRetriever(
            ProjectFileMetadataIndexRepository projectFileMetadataIndexRepository,
            ObjectMapper objectMapper,
            @Value("${codeatlas.context.indexed.max-files:16}") int maxFiles,
            @Value("${codeatlas.context.max-snippet-lines:200}") int maxSnippetLines,
            @Value("${codeatlas.context.max-snippet-chars:10000}") int maxSnippetChars
    ) {
        this.projectFileMetadataIndexRepository = projectFileMetadataIndexRepository;
        this.objectMapper = objectMapper;
        this.maxFiles = Math.max(1, maxFiles);
        this.maxSnippetLines = Math.max(6, maxSnippetLines);
        this.maxSnippetChars = Math.max(400, maxSnippetChars);
    }

    public ContextResult retrieve(Project project, Intent intent) {
        List<ProjectFileMetadataIndex> metadataRows = projectFileMetadataIndexRepository.findByProjectId(project.getId());
        if (metadataRows.isEmpty()) {
            return new ContextResult(List.of());
        }

        List<ScoredCandidate> scoredCandidates = new ArrayList<>();
        for (ProjectFileMetadataIndex metadataRow : metadataRows) {
            ScoredCandidate candidate = scoreCandidate(metadataRow, intent);
            if (candidate != null && candidate.score() > 0) {
                scoredCandidates.add(candidate);
            }
        }

        int returnLimit = intent.confidence() < LOW_CONFIDENCE_THRESHOLD ? maxFiles * 2 : maxFiles;
        List<ScoredCandidate> selectedCandidates = scoredCandidates.stream()
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed()
                        .thenComparing(c -> c.file.getFilePath()))
                .limit(returnLimit)
                .toList();

        Path projectRoot = Path.of(project.getPath()).normalize();
        List<RetrievedFile> files = new ArrayList<>(selectedCandidates.size());
        for (ScoredCandidate candidate : selectedCandidates) {
            files.add(toRetrievedFile(projectRoot, candidate, intent));
        }
        return new ContextResult(files);
    }

    private ScoredCandidate scoreCandidate(ProjectFileMetadataIndex metadataRow, Intent intent) {
        ProjectFileIndex fileIndex = metadataRow.getFile();
        if (fileIndex == null || fileIndex.getFilePath() == null || fileIndex.getFilePath().isBlank()) {
            return null;
        }
        FileSummaryOfflineResponse.Metadata metadata = parseMetadata(metadataRow.getMetadataJson());
        if (metadata == null) {
            return null;
        }
        MetadataMatchScorer.ScoreResult scoreResult = MetadataMatchScorer.score(intent, metadata);
        String relativePath = fileIndex.getFilePath().trim();
        String architecturalRole = metadata.architecturalRole() == null ? "" : metadata.architecturalRole().trim();
        String extension = FileHelper.extensionOf(Path.of(relativePath).getFileName().toString());
        return new ScoredCandidate(
                metadataRow.getFile(),
                FileHelper.languageByExtension(extension),
                architecturalRole,
                scoreResult.score(),
                scoreResult.reasons(),
                scoreResult.matchedSymbols(),
                buildSnippetAnchorSymbols(intent, scoreResult.matchedSymbols())
        );
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

    private List<String> buildSnippetAnchorSymbols(Intent intent, List<String> matchedSymbols) {
        LinkedHashSet<String> anchors = new LinkedHashSet<>();
        if (matchedSymbols != null) {
            for (String symbol : matchedSymbols) {
                if (symbol != null && !symbol.isBlank()) {
                    anchors.add(symbol.trim());
                }
            }
        }
        for (String symbol : intent.symbols()) {
            if (symbol != null && !symbol.isBlank()) {
                anchors.add(symbol.trim());
            }
        }
        return List.copyOf(anchors);
    }

    private RetrievedFile toRetrievedFile(Path projectRoot, ScoredCandidate candidate, Intent intent) {
        String snippet = extractSnippet(projectRoot, candidate);
        List<String> symbols = candidate.matchedSymbols().isEmpty() ? intent.symbols() : candidate.matchedSymbols();
        return new RetrievedFile(
                candidate.file(),
                candidate.language(),
                candidate.type(),
                candidate.score(),
                candidate.reasons(),
                symbols,
                snippet
        );
    }

    private String extractSnippet(Path projectRoot, ScoredCandidate candidate) {
        Path filePath = projectRoot.resolve(candidate.file().getFilePath()).normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return "";
        }
        try {
            List<String> lines = Files.readString(filePath).lines().toList();
            return SymbolCenteredSnippetExtractor.extract(
                    lines,
                    candidate.snippetAnchorSymbols(),
                    maxSnippetLines,
                    maxSnippetChars
            );
        } catch (IOException ex) {
            return "";
        }
    }

    private record ScoredCandidate(
            ProjectFileIndex file,
            String language,
            String type,
            int score,
            List<String> reasons,
            List<String> matchedSymbols,
            List<String> snippetAnchorSymbols
    ) {
    }

}
