package com.code.atlas.web.service;

public enum PromptTemplate {
    CONTEXT_FILE_SUMMARY("prompts/context/file-summary.md"),
    CONTEXT_ARCHITECTURE_SUMMARY("prompts/context/architecture-summary.md"),
    CONTEXT_INTENT_EXTRACTION("prompts/context/intent-extraction.md"),
    CONTEXT_MISSING_CONTEXT("prompts/context/missing-context.md"),
    CHANGELOG_BUILDER("prompts/changelog-builder.md"),
    CODE_REVIEW("prompts/code-review.md"),
    COMMIT_MESSAGE("prompts/commit-message.md"),
    ;


    private final String resourcePath;

    PromptTemplate(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }
}
