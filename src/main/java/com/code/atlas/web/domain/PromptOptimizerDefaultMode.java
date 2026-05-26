package com.code.atlas.web.domain;

public enum PromptOptimizerDefaultMode {
    CHEAP("Cheap"),
    BALANCED("Balanced"),
    ARCHITECT("Architect"),
    IMPLEMENTATION("Implementation"),
    REVIEWER("Reviewer"),
    REFACTOR("Refactor"),
    SECURITY("Security");

    private final String name;
    PromptOptimizerDefaultMode(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
