package com.code.atlas.web.domain;

public enum PlanModePromptType {
    DISCOVERY("Discovery", "discovery.md"),
    PLAN_CURSOR_OPTIMIZED("Cursor Optimized", "cursor-optimized.md"),
    PLAN_HUMAN_READABLE("Human Readable", "human-readable.md"),
    PLAN_ARCHITECTURE_PROPOSAL("Architecture Proposal", "architecture-proposal.md"),
    PLAN_TECHNICAL_SPECIFICATION("Technical Specification", "technical-specification.md"),
    PLAN_IMPLEMENTATION_CHECKLIST("Implementation Checklist", "implementation-checklist.md"),
    PLAN_TASK_BREAKDOWN("Task Breakdown", "task-breakdown.md");

    private final String displayName;
    private final String templateFileName;

    PlanModePromptType(String displayName, String templateFileName) {
        this.displayName = displayName;
        this.templateFileName = templateFileName;
    }

    public String displayName() {
        return displayName;
    }

    public String templateFileName() {
        return templateFileName;
    }

    public static PlanModePromptType planPromptFor(PlanOutputType outputType) {
        return switch (outputType) {
            case CURSOR_OPTIMIZED        -> PLAN_CURSOR_OPTIMIZED;
            case HUMAN_READABLE          -> PLAN_HUMAN_READABLE;
            case ARCHITECTURE_PROPOSAL   -> PLAN_ARCHITECTURE_PROPOSAL;
            case TECHNICAL_SPECIFICATION -> PLAN_TECHNICAL_SPECIFICATION;
            case IMPLEMENTATION_CHECKLIST -> PLAN_IMPLEMENTATION_CHECKLIST;
            case TASK_BREAKDOWN          -> PLAN_TASK_BREAKDOWN;
        };
    }
}
