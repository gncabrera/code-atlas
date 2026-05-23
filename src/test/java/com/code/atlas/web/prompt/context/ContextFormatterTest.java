package com.code.atlas.web.prompt.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContextFormatterTest {

    private final ContextFormatter formatter = new ContextFormatter();

    @Test
    void formatBuildsDeterministicRelevantFilesLayout() {
        ContextCandidate candidate = new ContextCandidate(
                "src/main/java/com/code/atlas/web/profile/ProfileController.java",
                94,
                List.of("Defines DELETE /api/profile", "Entry point for profile deletion flow"),
                List.of("ProfileController", "deleteProfile()"),
                "@DeleteMapping(\"/api/profile\")\npublic ResponseEntity<Void> deleteProfile() {",
                "java"
        );

        String output = formatter.format(List.of(candidate));

        assertTrue(output.contains("## Relevant Files"));
        assertTrue(output.contains("### 1. src/main/java/com/code/atlas/web/profile/ProfileController.java"));
        assertTrue(output.contains("Score: 94"));
        assertTrue(output.contains("Symbols:"));
        assertTrue(output.contains("```java"));
    }
}
