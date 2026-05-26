package com.code.atlas.web.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GitProcessRunner {

    public String run(Path workingDir, List<String> command) {
        return runInternal(workingDir, command, false);
    }

    public String runAllowDiffExit(Path workingDir, List<String> command) {
        return runInternal(workingDir, command, true);
    }

    private String runInternal(Path workingDir, List<String> command, boolean allowDiffExit) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDir.toFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0 && !(allowDiffExit && exitCode == 1)) {
                throw new IllegalArgumentException(formatFailure(command, output, exitCode));
            }
            return output;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed running git command: " + String.join(" ", command), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Git command interrupted: " + String.join(" ", command), ex);
        }
    }

    private String formatFailure(List<String> command, String output, int exitCode) {
        String message = output.isBlank() ? "Git command failed." : output;
        return "Git command failed (" + String.join(" ", command) + ", exit " + exitCode + "): " + message;
    }

    public String collectWorkingTreeDiff(Path projectRoot) {
        StringBuilder diff = new StringBuilder();
        appendDiffSection(diff, runAllowDiffExit(projectRoot, List.of("git", "diff", "HEAD")));

        String untrackedFiles = run(
                projectRoot,
                List.of("git", "ls-files", "--others", "--exclude-standard")
        );
        if (!untrackedFiles.isBlank()) {
            String nullDevice = nullDevicePath();
            for (String file : untrackedFiles.split("\n")) {
                String relativePath = file.trim();
                if (relativePath.isEmpty()) {
                    continue;
                }
                appendDiffSection(
                        diff,
                        runAllowDiffExit(
                                projectRoot,
                                List.of("git", "diff", "--no-index", nullDevice, relativePath)
                        )
                );
            }
        }

        return diff.toString().trim();
    }

    private void appendDiffSection(StringBuilder diff, String section) {
        if (section == null || section.isBlank()) {
            return;
        }
        if (!diff.isEmpty()) {
            diff.append("\n");
        }
        diff.append(section.trim());
    }

    private String nullDevicePath() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win") ? "NUL" : "/dev/null";
    }
}
