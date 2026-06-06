package com.code.atlas.web.service.dto;

public record ChangelogBuilderResponseDto(
        String changelog,
        String semverBump,
        String semverReason
) {
    public ChangelogBuilderResponseDto {
        if (changelog == null) {
            throw new IllegalArgumentException("Changelog output cannot be null.");
        }
        if (semverBump == null || semverBump.isBlank()) {
            throw new IllegalArgumentException("Semver bump is required.");
        }
        if (semverReason == null) {
            semverReason = "";
        }
    }
}
