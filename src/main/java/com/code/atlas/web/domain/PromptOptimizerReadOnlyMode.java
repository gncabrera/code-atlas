package com.code.atlas.web.domain;

public enum PromptOptimizerReadOnlyMode {
    CHEAP("Cheap", "cheap.md"),
    BALANCED("Balanced", "balanced.md"),
    ARCHITECT("Architect", "architect.md"),
    IMPLEMENTATION("Implementation", "implementation.md"),
    REVIEWER("Reviewer", "reviewer.md"),
    REFACTOR("Refactor", "refactor.md"),
    SECURITY("Security", "security.md");

    private final String displayName;
    private final String templateFileName;

    PromptOptimizerReadOnlyMode(String displayName, String templateFileName) {
        this.displayName = displayName;
        this.templateFileName = templateFileName;
    }

    public String displayName() {
        return displayName;
    }

    public String templateFileName() {
        return templateFileName;
    }

    public static boolean isProtectedCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        try {
            PromptOptimizerReadOnlyMode.valueOf(code.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
