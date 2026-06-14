package com.code.atlas.web.service.dto;

import java.time.LocalDateTime;

public record ConversationMessageDto(String role, String content, LocalDateTime createdAt) {}
