package com.code.atlas.web.service.context.indexed.offline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.service.PromptFormatService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfflineFileSummaryChunkBuilderTest {

    private static final String SUMMARY_TEMPLATE = """
            Summarize each file.

            Files:
            {{FILES}}
            """;

    @TempDir
    Path tempDir;

    private OfflineFileSummaryChunkBuilder builder;
    private Project project;
    private AIModel model;

    @BeforeEach
    void setUp() {
        builder = new OfflineFileSummaryChunkBuilder(new PromptFormatService(), 5, 2000, 0.8);
        project = new Project();
        project.setId(1L);
        project.setPath(tempDir.toString());

        model = new AIModel();
        model.setTokensPerMinute(10_000);
    }

    @Test
    void buildChunks_includesPathContentDelimiterFormat() throws Exception {
        Path file = tempDir.resolve("src/Example.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "public class Example {}");

        ProjectFileIndex entry = indexedEntry("src/Example.java", "Example");

        List<String> chunks = builder.buildChunks(project, List.of(entry), model, SUMMARY_TEMPLATE);

        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().contains("src/Example.java:\n---\npublic class Example {}\n---"));
    }

    @Test
    void buildChunks_splitsIntoMaxFiveFilesPerChunk() {
        List<ProjectFileIndex> files = java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> indexedEntry("file" + i + ".java", "Symbol" + i))
                .toList();

        List<String> chunks = builder.buildChunks(project, files, model, SUMMARY_TEMPLATE);

        assertEquals(3, chunks.size());
    }

    @Test
    void buildChunks_fallsBackToSymbolsWhenContentUnavailable() {
        ProjectFileIndex entry = indexedEntry("missing/Example.java", "Example, save");

        List<String> chunks = builder.buildChunks(project, List.of(entry), model, SUMMARY_TEMPLATE);

        assertEquals(1, chunks.size());
        String chunk = chunks.getFirst();
        assertTrue(chunk.contains("missing/Example.java:\n---"));
        assertTrue(chunk.contains("symbols: Example, save"));
        assertTrue(chunk.contains("(content unavailable"));
    }

    @Test
    void buildChunks_usesSearchableTextWhenDiskReadFails() {
        ProjectFileIndex entry = indexedEntry("missing/Example.java", "Example");
        entry.setSearchableText("class Example { void save() {} }");

        List<String> chunks = builder.buildChunks(project, List.of(entry), model, SUMMARY_TEMPLATE);

        assertTrue(chunks.getFirst().contains("class Example { void save() {} }"));
    }

    @Test
    void buildChunks_truncatesLargeFileContent() throws Exception {
        Path file = tempDir.resolve("Large.java");
        Files.writeString(file, "x".repeat(100_000));

        ProjectFileIndex entry = indexedEntry("Large.java", "Large");

        List<String> chunks = builder.buildChunks(project, List.of(entry), model, SUMMARY_TEMPLATE);

        assertTrue(chunks.getFirst().contains("[content truncated]"));
        assertTrue(chunks.getFirst().length() < 100_000);
    }

    private ProjectFileIndex indexedEntry(String filePath, String symbols) {
        ProjectFileIndex entry = new ProjectFileIndex();
        entry.setFilePath(filePath);
        entry.setSymbols(symbols);
        entry.setSearchableText("");
        return entry;
    }
}
