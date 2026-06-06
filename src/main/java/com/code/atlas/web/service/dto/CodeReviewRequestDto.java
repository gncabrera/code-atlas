package com.code.atlas.web.service.dto;

public record CodeReviewRequestDto(
        Long projectId,
        Long modelId,
        String branchA,
        String branchB,
        boolean currentChangesOnly
) {
    public CodeReviewRequestDto {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is mandatory");
        }
        if (modelId == null) {
            throw new IllegalArgumentException("AI Model ID is mandatory");
        }
        if (!currentChangesOnly) {
            if (branchA == null || branchA.isBlank()) {
                throw new IllegalArgumentException("Base branch is mandatory");
            }
            if (branchB == null || branchB.isBlank()) {
                throw new IllegalArgumentException("Target branch is mandatory");
            }
        }
    }
}
