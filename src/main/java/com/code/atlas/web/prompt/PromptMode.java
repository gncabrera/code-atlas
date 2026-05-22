package com.code.atlas.web.prompt;

public enum PromptMode {
    CHEAP("cheap.md"),
    BALANCED("balanced.md"),
    ARCHITECT("architect.md"),
    IMPLEMENTATION("implementation.md"),
    REVIEWER("reviewer.md"),
    REFACTOR("refactor.md"),
    SECURITY("security.md");

    private final String templateFileName;

    PromptMode(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    public String templateFileName() {
        return templateFileName;
    }

    public static PromptMode fromNullableValue(String value) {
        if (value == null || value.isBlank()) {
            return BALANCED;
        }
        try {
            return PromptMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return BALANCED;
        }
    }
}
