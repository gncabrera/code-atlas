package com.code.atlas.web.service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PlanSessionDetailDto(
        Long id,
        String title,
        String userRequest,
        String outputType,
        String outputTypeDisplay,
        Long projectId,
        String projectName,
        Long contextModelId,
        String contextModelName,
        Long planModelId,
        String planModelName,
        String contextData,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        PlanDiscoveryDto discovery,
        Map<String, String> answers,
        List<String> selectedSuggestions,
        PlanResultDto result
) {}
