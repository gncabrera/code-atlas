package com.code.atlas.web.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GitProcessRunner {

    private static final Logger log = LoggerFactory.getLogger(GitProcessRunner.class);
    private static final long GIT_COMMAND_TIMEOUT_SECONDS = 30;
    private static final Pattern BRANCH_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-.\\/]+$");
    private static final String DEFAULT_REMOTE = "origin";

    public String run(Path workingDir, List<String> command) {
        return runInternal(workingDir, command, false);
    }

    public String runAllowDiffExit(Path workingDir, List<String> command) {
        return runInternal(workingDir, command, true);
    }

    public List<String> listBranches(Path projectRoot) {
        String output = run(
                projectRoot,
                List.of(
                        "git",
                        "for-each-ref",
                        "--format=%(refname:short)",
                        "refs/heads",
                        "refs/remotes"
                )
        );
        if (output.isBlank()) {
            return List.of();
        }
        return Arrays.stream(output.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.endsWith("/HEAD"))
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> listTrackedFiles(Path projectRoot) {
        String output = run(projectRoot, List.of("git", "ls-files"));
        if (output.isBlank()) {
            return List.of();
        }
        return Arrays.stream(output.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .sorted()
                .toList();
    }

    public String diffBetweenBranches(Path projectRoot, String branchA, String branchB) {
        validateBranchName(branchA);
        validateBranchName(branchB);
        return runAllowDiffExit(projectRoot, List.of("git", "diff", branchA + ".." + branchB));
    }

    public void pushCurrentBranch(Path projectRoot) {
        try {
            run(projectRoot, List.of("git", "push"));
        } catch (IllegalArgumentException ex) {
            if (!isMissingUpstreamPushError(ex.getMessage())) {
                throw ex;
            }
            run(projectRoot, List.of("git", "push", "-u", DEFAULT_REMOTE, "HEAD"));
        }
    }

    private boolean isMissingUpstreamPushError(String message) {
        return message != null && message.contains("no upstream branch");
    }

    private List<String> withNoPager(List<String> command) {
        if (command.isEmpty() || !"git".equals(command.get(0))) {
            return command;
        }
        if (command.size() > 1 && "--no-pager".equals(command.get(1))) {
            return command;
        }
        List<String> adjusted = new ArrayList<>(command.size() + 1);
        adjusted.add("git");
        adjusted.add("--no-pager");
        adjusted.addAll(command.subList(1, command.size()));
        return adjusted;
    }

    private void disableGitPager(ProcessBuilder processBuilder) {
        processBuilder.environment().put("GIT_PAGER", "");
        processBuilder.environment().put("PAGER", "");
    }

    private void validateBranchName(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        if (!BRANCH_NAME_PATTERN.matcher(branchName.trim()).matches()) {
            throw new IllegalArgumentException("Invalid branch name: " + branchName);
        }
    }

    private String runInternal(Path workingDir, List<String> command, boolean allowDiffExit) {
        List<String> effectiveCommand = withNoPager(command);
        ProcessBuilder processBuilder = new ProcessBuilder(effectiveCommand);
        processBuilder.directory(workingDir.toFile());
        processBuilder.redirectErrorStream(true);
        disableGitPager(processBuilder);

        long startedAt = System.currentTimeMillis();
        log.info("Starting git command in {}: {}", workingDir, formatCommand(effectiveCommand));

        try {
            Process process = processBuilder.start();
            process.getOutputStream().close();

            CompletableFuture<byte[]> outputFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.getInputStream().readAllBytes();
                } catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            });

            boolean finished = process.waitFor(GIT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                log.warn(
                        "Git command timed out after {}s in {}: {}",
                        GIT_COMMAND_TIMEOUT_SECONDS,
                        formatElapsed(startedAt),
                        formatCommand(effectiveCommand)
                );
                throw new IllegalArgumentException(
                        "Git command timed out after " + GIT_COMMAND_TIMEOUT_SECONDS + "s: "
                                + formatCommand(effectiveCommand)
                );
            }

            byte[] outputBytes = readOutputBytes(outputFuture, effectiveCommand);
            String output = new String(outputBytes, StandardCharsets.UTF_8).trim();
            int exitCode = process.exitValue();
            log.info(
                    "Git command finished in {} (exit {}, {} bytes): {}",
                    formatElapsed(startedAt),
                    exitCode,
                    outputBytes.length,
                    formatCommand(effectiveCommand)
            );
            if (exitCode != 0 && !(allowDiffExit && exitCode == 1)) {
                throw new IllegalArgumentException(formatFailure(effectiveCommand, output, exitCode));
            }
            return output;
        } catch (IOException ex) {
            log.warn(
                    "Git command failed in {}: {} ({})",
                    formatElapsed(startedAt),
                    formatCommand(effectiveCommand),
                    ex.getMessage()
            );
            throw new IllegalArgumentException(
                    "Failed running git command: " + formatCommand(effectiveCommand),
                    ex
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(
                    "Git command interrupted: " + formatCommand(effectiveCommand),
                    ex
            );
        }
    }

    private byte[] readOutputBytes(CompletableFuture<byte[]> outputFuture, List<String> command)
            throws InterruptedException {
        try {
            return outputFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalArgumentException(
                    "Failed reading git command output: " + formatCommand(command),
                    cause
            );
        } catch (TimeoutException ex) {
            throw new IllegalArgumentException(
                    "Timed out reading git command output: " + formatCommand(command),
                    ex
            );
        }
    }

    private String formatCommand(List<String> command) {
        return String.join(" ", command);
    }

    private String formatElapsed(long startedAtMillis) {
        return (System.currentTimeMillis() - startedAtMillis) + "ms";
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
