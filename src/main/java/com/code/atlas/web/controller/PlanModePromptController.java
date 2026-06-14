package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.service.PlanModeService;
import com.code.atlas.web.service.dto.PlanModePromptRequestDto;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/plan-mode-prompts")
public class PlanModePromptController extends BaseRestController {

    private final PlanModeService planModeService;

    public PlanModePromptController(PlanModeService planModeService) {
        this.planModeService = planModeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> listPrompts() {
        try {
            List<?> prompts = planModeService.listPrompts();
            return ResponseEntity.ok(ApiResponse.success("Plan mode prompts fetched.", prompts));
        } catch (Exception ex) {
            return handledException("GET /api/admin/plan-mode-prompts", ex);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getPrompt(@PathVariable Long id) {
        try {
            var prompt = planModeService.getPromptById(id);
            return ResponseEntity.ok(ApiResponse.success("Plan mode prompt fetched.", prompt));
        } catch (Exception ex) {
            return handledException("GET /api/admin/plan-mode-prompts/{id}", ex);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updatePrompt(
            @PathVariable Long id, @RequestBody PlanModePromptRequestDto request) {
        try {
            var updated = planModeService.updatePrompt(id, request);
            return ResponseEntity.ok(ApiResponse.success("Plan mode prompt updated.", updated));
        } catch (Exception ex) {
            return handledException("PUT /api/admin/plan-mode-prompts/{id}", ex);
        }
    }
}
