package com.code.atlas.web.domain;

public enum PlanOutputType {
    CURSOR_OPTIMIZED("Cursor Optimized"),
    HUMAN_READABLE("Human Readable"),
    ARCHITECTURE_PROPOSAL("Architecture Proposal"),
    TECHNICAL_SPECIFICATION("Technical Specification"),
    IMPLEMENTATION_CHECKLIST("Implementation Checklist"),
    TASK_BREAKDOWN("Task Breakdown");

    private final String displayName;

    PlanOutputType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
