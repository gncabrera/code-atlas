package com.code.atlas.web.skill.dto;

public record SkillCreateRequest(
        String name,
        String prompt,
        String targetPath,
        String description,
        String category,
        Boolean defaultInOutputPrompt
) {
    public SkillCreateRequest {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Skill name is required.");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Skill prompt is required.");
        }
        if (targetPath == null || targetPath.isBlank()) {
            throw new IllegalArgumentException("Skill target path is required.");
        }
    }
}
