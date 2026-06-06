package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitProcessRunnerTest {

    private GitProcessRunner gitProcessRunner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gitProcessRunner = new GitProcessRunner();
    }

    @Test
    void pushCurrentBranch_whenNoUpstream_setsUpstreamAndPushes() throws IOException {
        Path bareRemote = tempDir.resolve("remote.git");
        Path workTree = tempDir.resolve("work-no-upstream");
        initBareRemote(bareRemote);
        initWorkRepo(workTree, bareRemote);
        gitProcessRunner.run(workTree, List.of("git", "checkout", "-b", "feature/no-upstream"));
        Files.writeString(workTree.resolve("change.txt"), "first push");
        gitProcessRunner.run(workTree, List.of("git", "add", "-A"));
        gitProcessRunner.run(workTree, List.of("git", "commit", "-m", "feature commit"));

        assertDoesNotThrow(() -> gitProcessRunner.pushCurrentBranch(workTree));
        assertEquals(
                "origin/feature/no-upstream",
                gitProcessRunner.run(workTree, List.of("git", "rev-parse", "--abbrev-ref", "@{u}"))
        );
    }

    @Test
    void pushCurrentBranch_whenUpstreamAlreadySet_pushesWithoutError() throws IOException {
        Path bareRemote = tempDir.resolve("remote.git");
        Path workTree = tempDir.resolve("work-with-upstream");
        initBareRemote(bareRemote);
        initWorkRepo(workTree, bareRemote);
        gitProcessRunner.run(workTree, List.of("git", "checkout", "-b", "feature/with-upstream"));
        Files.writeString(workTree.resolve("first.txt"), "first");
        gitProcessRunner.run(workTree, List.of("git", "add", "-A"));
        gitProcessRunner.run(workTree, List.of("git", "commit", "-m", "first commit"));
        gitProcessRunner.pushCurrentBranch(workTree);

        Files.writeString(workTree.resolve("second.txt"), "second");
        gitProcessRunner.run(workTree, List.of("git", "add", "-A"));
        gitProcessRunner.run(workTree, List.of("git", "commit", "-m", "second commit"));

        assertDoesNotThrow(() -> gitProcessRunner.pushCurrentBranch(workTree));
    }

    @Test
    void pushCurrentBranch_whenPushFailsForOtherReason_doesNotMaskError() throws IOException {
        Path workTree = tempDir.resolve("work-no-remote");
        Files.createDirectories(workTree);
        gitProcessRunner.run(workTree, List.of("git", "init"));
        configureGitIdentity(workTree);
        Files.writeString(workTree.resolve("README.md"), "init");
        gitProcessRunner.run(workTree, List.of("git", "add", "README.md"));
        gitProcessRunner.run(workTree, List.of("git", "commit", "-m", "init"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> gitProcessRunner.pushCurrentBranch(workTree)
        );

        assertTrue(ex.getMessage().contains("Git command failed"));
        assertTrue(ex.getMessage().contains("no upstream branch") || ex.getMessage().contains("push"));
    }

    private void initBareRemote(Path bareRemote) {
        gitProcessRunner.run(tempDir, List.of("git", "init", "--bare", bareRemote.toString()));
    }

    private void initWorkRepo(Path workTree, Path bareRemote) throws IOException {
        Files.createDirectories(workTree);
        gitProcessRunner.run(workTree, List.of("git", "init"));
        configureGitIdentity(workTree);
        gitProcessRunner.run(workTree, List.of("git", "remote", "add", "origin", bareRemote.toString()));
        Files.writeString(workTree.resolve("README.md"), "init");
        gitProcessRunner.run(workTree, List.of("git", "add", "README.md"));
        gitProcessRunner.run(workTree, List.of("git", "commit", "-m", "init"));
    }

    private void configureGitIdentity(Path workTree) {
        gitProcessRunner.run(workTree, List.of("git", "config", "user.email", "test@example.com"));
        gitProcessRunner.run(workTree, List.of("git", "config", "user.name", "Test User"));
    }
}
