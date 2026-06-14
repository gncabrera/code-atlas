package com.code.atlas.web.service.dto;

public record ConversationTurnDto(String role, String content) {

    public ConversationTurnDto {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Conversation turn role is required.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Conversation turn content is required.");
        }
    }
}
