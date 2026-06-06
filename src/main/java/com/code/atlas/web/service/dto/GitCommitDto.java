package com.code.atlas.web.service.dto;

public record GitCommitDto(
        String hash,
        String author,
        String dateFormatted,
        String subject
) {
    public GitCommitDto {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Hash cannot be blank.");
        }
    }
}
