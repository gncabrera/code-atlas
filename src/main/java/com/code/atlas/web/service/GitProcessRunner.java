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
}
