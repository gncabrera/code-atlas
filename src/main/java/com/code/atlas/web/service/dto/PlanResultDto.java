package com.code.atlas.web.service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PlanResultDto(
        String generatedPlan,
        LocalDateTime createdAt,
        List<ConversationMessageDto> conversationMessages
) {}
