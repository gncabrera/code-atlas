package com.code.atlas.web.service.dto;

public record IndexClearRequestDto(String confirmationText) {
    public IndexClearRequestDto {
        if (confirmationText == null || confirmationText.isBlank()) {
            throw new IllegalArgumentException("Confirmation text is required.");
        }
    }
}
