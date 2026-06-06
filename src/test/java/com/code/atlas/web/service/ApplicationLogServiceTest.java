package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code.atlas.web.service.dto.LogTailResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationLogServiceTest {

    @TempDir
    Path tempDir;

    private Path logFile;
    private ApplicationLogService service;

    @BeforeEach
    void setUp() {
        logFile = tempDir.resolve("app.log");
        service = new ApplicationLogService(logFile.toString());
    }

    @Test
    void tailLog_returnsLastRequestedLines() throws Exception {
        Files.writeString(logFile, """
                line-1
                line-2
                line-3
                line-4
                """);

        LogTailResponse response = service.tailLog(2);

        assertEquals(2, response.lines().size());
        assertEquals("line-3", response.lines().get(0));
        assertEquals("line-4", response.lines().get(1));
    }

    @Test
    void tailLog_missingFile_returnsEmptyLines() {
        LogTailResponse response = service.tailLog(50);

        assertTrue(response.lines().isEmpty());
        assertFalse(response.running());
        assertEquals(-1.0, response.progressPercent());
    }

    @Test
    void tailLog_detectsRunningWhenStepStartedWithoutCompletion() throws Exception {
        Files.writeString(logFile, """
                2026-06-06 10:00:00.000 INFO  [thread] c.c.a.w.s.c.i.ContextPipelineLogger - [Context][project=1][phase=offline][step=2/4] Scan files \u2014 completed (10 ms)
                2026-06-06 10:00:01.000 INFO  [thread] c.c.a.w.s.c.i.ContextPipelineLogger - [Context][project=1][phase=offline][step=3/4] Summarize files \u2014 starting
                """);

        LogTailResponse response = service.tailLog(100);

        assertEquals(2, response.lines().size());
        assertTrue(response.running(), () -> "lines=" + response.lines());
        assertEquals(75.0, response.progressPercent());
    }

    @Test
    void tailLog_notRunningAfterStepCompleted() throws Exception {
        Files.writeString(logFile, """
                2026-06-06 10:00:00.000 INFO  [thread] c.c.a.w.s.c.i.ContextPipelineLogger - [Context][project=1][phase=offline][step=3/4] Summarize files \u2014 starting
                2026-06-06 10:00:05.000 INFO  [thread] c.c.a.w.s.c.i.ContextPipelineLogger - [Context][project=1][phase=offline][step=3/4] Summarize files \u2014 completed (5000 ms)
                """);

        LogTailResponse response = service.tailLog(100);

        assertFalse(response.running());
        assertEquals(75.0, response.progressPercent());
    }

    @Test
    void tailLog_parsesChunkProgressFromLlmLabel() throws Exception {
        Files.writeString(logFile, """
                2026-06-06 10:00:00.000 INFO  [thread] c.c.a.w.s.AIModelService - [Context][project=1] LLM \u2014 starting: Offline index: file summaries chunk 2/5 (model=test, estTokens=100)
                """);

        LogTailResponse response = service.tailLog(100);

        assertTrue(response.running());
        assertEquals(40.0, response.progressPercent());
    }

    @Test
    void tailLog_notRunningAfterOfflineRegenerationFinished() throws Exception {
        Files.writeString(logFile, """
                2026-06-06 10:00:00.000 INFO  [thread] c.c.a.w.s.c.i.ContextPipelineLogger - [Context][project=1][phase=offline] Starting offline incremental index regeneration
                2026-06-06 10:05:00.000 INFO  [thread] c.c.a.w.s.c.i.ContextPipelineLogger - [Context][project=1][phase=offline] Offline incremental index regeneration finished (300000 ms)
                """);

        LogTailResponse response = service.tailLog(100);

        assertFalse(response.running());
    }

    @Test
    void clampLineCount_enforcesDefaultsAndMaximum() {
        assertEquals(500, service.clampLineCount(null));
        assertEquals(500, service.clampLineCount(0));
        assertEquals(100, service.clampLineCount(100));
        assertEquals(2000, service.clampLineCount(5000));
    }
}
