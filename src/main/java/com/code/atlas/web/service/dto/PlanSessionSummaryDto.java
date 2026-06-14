package com.code.atlas.web.service.dto;

import java.time.LocalDateTime;

public record PlanSessionSummaryDto(
        Long id,
        String title,
        String outputType,
        String outputTypeDisplay,
        String status,
        LocalDateTime createdAt
) {}
