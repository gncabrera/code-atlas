package com.code.atlas.web.service.dto;

public record ProjectTypeRequestDto(
        String name,
        String allowedExtensions,
        String description
) {
    public ProjectTypeRequestDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project type name is required.");
        }
        if (allowedExtensions == null || allowedExtensions.isBlank()) {
            throw new IllegalArgumentException("Allowed extensions are required.");
        }
    }
}
