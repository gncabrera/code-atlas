package com.code.atlas.web.skill.dto;

import java.util.List;

public record SkillInstallRequest(
        Long projectId,
        List<Long> skillIds
) {
    public SkillInstallRequest {
        if (projectId == null) {
            throw new IllegalArgumentException("Project id is required.");
        }
        if (skillIds == null || skillIds.isEmpty()) {
            throw new IllegalArgumentException("At least one skill id is required.");
        }
    }
}
