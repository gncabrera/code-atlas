package com.code.atlas.web.controller;

import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.dto.AIModelRequestDto;
import com.code.atlas.web.service.dto.AIModelResponseDto;
import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-models")
public class AIModelController {

    private final AIModelService aiModelService;

    public AIModelController(AIModelService aiModelService) {
        this.aiModelService = aiModelService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getModels(@RequestParam(defaultValue = "false") boolean enabledOnly) {
        try {
            List<AIModelResponseDto> models = enabledOnly
                    ? aiModelService.getEnabledModels()
                    : aiModelService.getAllModels();
            return ResponseEntity.ok(ApiResponse.success("AI models fetched.", models));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("GET /api/ai-models", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getModelById(@PathVariable Long id) {
        try {
            AIModelResponseDto model = aiModelService.getModelById(id);
            return ResponseEntity.ok(ApiResponse.success("AI model fetched.", model));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("GET /api/ai-models/{id}", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createModel(@RequestBody AIModelRequestDto requestDto) {
        try {
            AIModelResponseDto created = aiModelService.createModel(requestDto);
            return new ResponseEntity<>(ApiResponse.success("AI model created.", created), HttpStatus.CREATED);
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("POST /api/ai-models", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateModel(@PathVariable Long id, @RequestBody AIModelRequestDto requestDto) {
        try {
            AIModelResponseDto updated = aiModelService.updateModel(id, requestDto);
            return ResponseEntity.ok(ApiResponse.success("AI model updated.", updated));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("PUT /api/ai-models/{id}", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteModel(@PathVariable Long id) {
        try {
            aiModelService.deleteModel(id);
            return ResponseEntity.ok(ApiResponse.success("AI model deleted.", null));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("DELETE /api/ai-models/{id}", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }
}
