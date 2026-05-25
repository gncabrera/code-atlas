package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import com.code.atlas.web.service.PromptHistoryService;
import com.code.atlas.web.service.dto.PromptHistoryResponseDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompt-history")
public class PromptHistoryController {

    private final PromptHistoryService promptHistoryService;

    public PromptHistoryController(PromptHistoryService promptHistoryService) {
        this.promptHistoryService = promptHistoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllHistory() {
        try {
            List<PromptHistoryResponseDto> history = promptHistoryService.getAllHistory();
            return ResponseEntity.ok(ApiResponse.success("Prompt history fetched.", history));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("GET /api/prompt-history", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }
}
