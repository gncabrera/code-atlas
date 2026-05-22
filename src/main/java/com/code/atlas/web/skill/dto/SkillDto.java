package com.code.atlas.web.skill.dto;

public record SkillDto(
        Long id,
        String name,
        String prompt,
        String targetPath,
        String description,
        String category,
        boolean defaultInOutputPrompt
) {
}
