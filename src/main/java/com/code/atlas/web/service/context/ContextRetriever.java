package com.code.atlas.web.service.context;

import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.Project;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import com.code.atlas.web.service.ProjectIndexService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ContextRetriever {

    private static final int INDEX_CANDIDATE_LIMIT = 20;

    private final ProjectIndexService projectIndexService;
    private final ContextSymbolExtractor contextSymbolExtractor;
    private final ContextSnippetExtractor contextSnippetExtractor;
    private final int maxFiles;
    private final int maxSnippetLines;
    private final int maxSnippetChars;

    public ContextRetriever(
            ProjectIndexService projectIndexService,
            ContextSymbolExtractor contextSymbolExtractor,
            ContextSnippetExtractor contextSnippetExtractor,
            @Value("${codeatlas.context.max-files:6}") int maxFiles,
            @Value("${codeatlas.context.max-snippet-lines:20}") int maxSnippetLines,
            @Value("${codeatlas.context.max-snippet-chars:1200}") int maxSnippetChars
    ) {
        this.projectIndexService = projectIndexService;
        this.contextSymbolExtractor = contextSymbolExtractor;
        this.contextSnippetExtractor = contextSnippetExtractor;
        this.maxFiles = Math.max(1, maxFiles);
        this.maxSnippetLines = Math.max(6, maxSnippetLines);
        this.maxSnippetChars = Math.max(400, maxSnippetChars);
    }

    public List<ContextCandidate> retrieve(Project project, ContextQuery query) {
        Path projectRoot = Path.of(project.getPath()).normalize();
        if (!Files.exists(projectRoot)) {
            return List.of();
        }
        if (projectIndexService.isStale(project)) {
            projectIndexService.refreshIndex(project);
        }
        List<Path> candidatePaths = collectCandidatePathsFromIndex(project, projectRoot, query);
        if (candidatePaths.size() < Math.min(3, maxFiles)) {
            candidatePaths = collectCandidatePathsOnDemand(projectRoot, query);
        }

        return candidatePaths.stream()
                .map(path -> buildCandidate(projectRoot, path, query))
                .filter(candidate -> candidate != null)
                .sorted(Comparator.comparingInt(ContextCandidate::score).reversed())
                .limit(maxFiles)
                .toList();
    }

    private List<Path> collectCandidatePathsFromIndex(Project project, Path projectRoot, ContextQuery query) {
        List<ProjectFileIndex> indexMatches = projectIndexService.search(project, query, INDEX_CANDIDATE_LIMIT);
        List<Path> paths = new ArrayList<>();
        for (ProjectFileIndex entry : indexMatches) {
            Path candidatePath = projectRoot.resolve(entry.getFilePath()).normalize();
            if (Files.exists(candidatePath) && Files.isRegularFile(candidatePath)) {
                paths.add(candidatePath);
            }
        }
        return dedupe(paths);
    }

    private List<Path> collectCandidatePathsOnDemand(Path projectRoot, ContextQuery query) {
        List<PathScore> scored = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> ContextFileSupport.isRelevantFile(projectRoot.relativize(path)))
                    .forEach(path -> {
                        int score = scorePath(path, query, projectRoot);
                        if (score > 0) {
                            scored.add(new PathScore(path, score));
                        }
                    });
        } catch (IOException ex) {
            return List.of();
        }
        return scored.stream()
                .sorted(Comparator.comparingInt(PathScore::score).reversed())
                .limit(INDEX_CANDIDATE_LIMIT)
                .map(PathScore::path)
                .toList();
    }

    private ContextCandidate buildCandidate(Path projectRoot, Path path, ContextQuery query) {
        try {
            String content = Files.readString(path);
            String contentLower = content.toLowerCase(Locale.ROOT);
            int score = scoreContent(path, contentLower, query, projectRoot);
            if (score <= 0) {
                return null;
            }
            String extension = ContextFileSupport.extensionOf(path.getFileName().toString());
            List<String> symbols = contextSymbolExtractor.extractSymbols(content, extension, 6);
            List<String> reasons = buildReasons(path, contentLower, query, projectRoot);
            List<String> lines = content.lines().toList();
            String snippet = contextSnippetExtractor.extractSnippet(
                    lines,
                    contentLower,
                    query,
                    maxSnippetLines,
                    maxSnippetChars
            );
            String relativePath = projectRoot.relativize(path).toString().replace('\\', '/');
            return new ContextCandidate(
                    relativePath,
                    Math.min(100, score),
                    reasons,
                    symbols,
                    snippet,
                    ContextFileSupport.languageByExtension(extension)
            );
        } catch (IOException ex) {
            return null;
        }
    }

    private int scorePath(Path path, ContextQuery query, Path projectRoot) {
        String relativePath = projectRoot.relativize(path).toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : query.keywords()) {
            if (relativePath.contains(keyword.toLowerCase(Locale.ROOT))) {
                score += 10;
            }
        }
        for (String area : query.focusAreas()) {
            if (relativePath.contains(area.toLowerCase(Locale.ROOT))) {
                score += 12;
            }
        }
        if (query.hasEndpoint() && relativePath.contains("controller")) {
            score += 8;
        }
        return score;
    }

    private int scoreContent(Path path, String contentLowerCase, ContextQuery query, Path projectRoot) {
        int score = scorePath(path, query, projectRoot);
        for (String keyword : query.keywords()) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            if (contentLowerCase.contains(normalizedKeyword)) {
                score += 8;
            }
        }
        if (query.hasEndpoint()) {
            String endpointLiteral = query.endpointPath().toLowerCase(Locale.ROOT);
            if (contentLowerCase.contains(endpointLiteral)) {
                score += 40;
            }
            String mapping = "@" + query.endpointMethod().substring(0, 1).toUpperCase(Locale.ROOT)
                    + query.endpointMethod().substring(1).toLowerCase(Locale.ROOT) + "Mapping";
            if (contentLowerCase.contains(mapping.toLowerCase(Locale.ROOT))) {
                score += 16;
            }
        }
        for (String area : query.focusAreas()) {
            if (contentLowerCase.contains(area.toLowerCase(Locale.ROOT))) {
                score += 12;
            }
        }
        return score;
    }

    private List<String> buildReasons(Path path, String contentLowerCase, ContextQuery query, Path projectRoot) {
        List<String> reasons = new ArrayList<>();
        String relativePath = projectRoot.relativize(path).toString().replace('\\', '/');
        if (query.hasEndpoint() && contentLowerCase.contains(query.endpointPath().toLowerCase(Locale.ROOT))) {
            reasons.add("Matches endpoint path " + query.endpointPath());
        }
        for (String area : query.focusAreas()) {
            if (relativePath.toLowerCase(Locale.ROOT).contains(area.toLowerCase(Locale.ROOT))) {
                reasons.add("Targets " + area + " layer concerns");
            }
        }
        for (String keyword : query.keywords()) {
            if (contentLowerCase.contains(keyword.toLowerCase(Locale.ROOT))) {
                reasons.add("Contains keyword '" + keyword + "'");
                if (reasons.size() >= 3) {
                    break;
                }
            }
        }
        if (reasons.isEmpty()) {
            reasons.add("Closest lexical match for requested feature.");
        }
        return reasons;
    }

    private List<Path> dedupe(List<Path> paths) {
        Set<Path> unique = new LinkedHashSet<>(paths);
        return List.copyOf(unique);
    }

    private record PathScore(Path path, int score) {
    }
}
