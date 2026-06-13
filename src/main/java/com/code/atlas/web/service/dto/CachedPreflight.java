package com.code.atlas.web.service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CachedPreflight(
        Long projectId,
        List<FlatProjectIndexDto> records,
        LocalDateTime expiresAt
) {
}
