package com.code.atlas.web.service;

import com.code.atlas.web.service.dto.LogTailResponse;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApplicationLogService {

    private static final int DEFAULT_LINE_COUNT = 500;
    private static final int MAX_LINE_COUNT = 2000;
    private static final int RUNNING_SCAN_LINE_COUNT = 100;
    private static final int ESTIMATED_BYTES_PER_LINE = 512;
    private static final double NO_PROGRESS = -1.0;

    private static final String DASH = "[\u2014-]";

    private static final Pattern STEP_PATTERN = Pattern.compile("\\[step=(\\d+)/(\\d+)\\]");
    private static final Pattern CHUNK_PATTERN = Pattern.compile("chunk\\s+(\\d+)/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STEP_START_PATTERN = Pattern.compile("\\[step=(\\d+)/(\\d+)\\][^\\n]*" + DASH + " starting");
    private static final Pattern STEP_TERMINAL_PATTERN = Pattern.compile("\\[step=(\\d+)/(\\d+)\\][^\\n]*" + DASH + " (?:completed|failed)");
    private static final Pattern LLM_START_PATTERN = Pattern.compile("LLM " + DASH + " starting: (.+?) \\(");
    private static final Pattern LLM_TERMINAL_PATTERN = Pattern.compile("LLM " + DASH + " (?:completed|failed): (.+?) \\(");
    private static final Pattern OFFLINE_START_PATTERN = Pattern.compile("Starting offline (?:incremental )?index regeneration");
    private static final Pattern OFFLINE_FINISHED_PATTERN = Pattern.compile("Offline (?:incremental )?index regeneration finished");

    private final Path logFilePath;

    public ApplicationLogService(@Value("${codeatlas.log.file}") String logFile) {
        this.logFilePath = Paths.get(logFile).toAbsolutePath().normalize();
    }

    public LogTailResponse tailLog(Integer requestedLines) {
        int lineCount = clampLineCount(requestedLines);
        List<String> lines = readLastLines(lineCount);
        boolean running = detectRunning(lines);
        double progressPercent = parseProgressPercent(lines);
        return new LogTailResponse(lines, running, progressPercent);
    }

    public int clampLineCount(Integer requestedLines) {
        if (requestedLines == null || requestedLines <= 0) {
            return DEFAULT_LINE_COUNT;
        }
        return Math.min(requestedLines, MAX_LINE_COUNT);
    }

    private List<String> readLastLines(int lineCount) {
        if (!Files.isRegularFile(logFilePath)) {
            log.warn("Application log file not found or not readable: {}", logFilePath);
            return List.of();
        }
        try (RandomAccessFile file = new RandomAccessFile(logFilePath.toFile(), "r")) {
            long fileLength = file.length();
            if (fileLength == 0L) {
                return List.of();
            }
            long bytesToRead = Math.min(fileLength, (long) lineCount * ESTIMATED_BYTES_PER_LINE);
            long startOffset = fileLength - bytesToRead;
            byte[] buffer = new byte[(int) bytesToRead];
            file.seek(startOffset);
            file.readFully(buffer);
            String chunk = new String(buffer, StandardCharsets.UTF_8);
            if (startOffset > 0L) {
                int firstLineBreak = chunk.indexOf('\n');
                if (firstLineBreak >= 0) {
                    chunk = chunk.substring(firstLineBreak + 1);
                }
            }
            List<String> allLines = chunk.lines().toList();
            if (allLines.isEmpty()) {
                return List.of();
            }
            int fromIndex = Math.max(0, allLines.size() - lineCount);
            return new ArrayList<>(allLines.subList(fromIndex, allLines.size()));
        } catch (IOException ex) {
            log.warn("Failed reading application log file {}: {}", logFilePath, ex.getMessage());
            return List.of();
        }
    }

    private boolean detectRunning(List<String> lines) {
        if (lines.isEmpty()) {
            return false;
        }
        int scanCount = Math.min(RUNNING_SCAN_LINE_COUNT, lines.size());
        List<String> recent = lines.subList(lines.size() - scanCount, lines.size());
        for (int index = recent.size() - 1; index >= 0; index--) {
            String line = recent.get(index);
            if (OFFLINE_START_PATTERN.matcher(line).find()) {
                return !hasOfflineFinished(recent, index);
            }
            Matcher stepStart = STEP_START_PATTERN.matcher(line);
            if (stepStart.find()) {
                return !hasStepTerminal(recent, index, stepStart.group(1), stepStart.group(2));
            }
            Matcher llmStart = LLM_START_PATTERN.matcher(line);
            if (llmStart.find()) {
                return !hasLlmTerminal(recent, index, llmStart.group(1));
            }
        }
        return false;
    }

    private boolean hasOfflineFinished(List<String> recent, int startIndex) {
        for (int index = startIndex + 1; index < recent.size(); index++) {
            String line = recent.get(index);
            if (OFFLINE_FINISHED_PATTERN.matcher(line).find()) {
                return true;
            }
            if (line.contains("index regeneration") && (line.contains("\u2014 failed") || line.contains("- failed"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStepTerminal(List<String> recent, int startIndex, String step, String total) {
        String stepKey = "[step=" + step + "/" + total + "]";
        for (int index = startIndex + 1; index < recent.size(); index++) {
            String line = recent.get(index);
            if (line.contains(stepKey) && STEP_TERMINAL_PATTERN.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLlmTerminal(List<String> recent, int startIndex, String label) {
        for (int index = startIndex + 1; index < recent.size(); index++) {
            String line = recent.get(index);
            Matcher terminal = LLM_TERMINAL_PATTERN.matcher(line);
            if (terminal.find() && label.equals(terminal.group(1))) {
                return true;
            }
        }
        return false;
    }

    private double parseProgressPercent(List<String> lines) {
        if (lines.isEmpty()) {
            return NO_PROGRESS;
        }
        for (int index = lines.size() - 1; index >= 0; index--) {
            String line = lines.get(index);
            Matcher chunkMatcher = CHUNK_PATTERN.matcher(line);
            if (chunkMatcher.find()) {
                return toPercent(chunkMatcher.group(1), chunkMatcher.group(2));
            }
            Matcher stepMatcher = STEP_PATTERN.matcher(line);
            if (stepMatcher.find()) {
                return toPercent(stepMatcher.group(1), stepMatcher.group(2));
            }
        }
        return NO_PROGRESS;
    }

    private double toPercent(String currentText, String totalText) {
        int current = Integer.parseInt(currentText);
        int total = Integer.parseInt(totalText);
        if (total <= 0) {
            return NO_PROGRESS;
        }
        return (current * 100.0) / total;
    }
}
