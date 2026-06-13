package com.code.atlas.web.service.dto;

public record ProjectTypeDto(
        Long id,
        String name,
        String allowedExtensions,
        String description
) {
}
