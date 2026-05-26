package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.service.PromptOptimizerModeService;
import com.code.atlas.web.service.dto.PromptOptimizerModeDto;
import com.code.atlas.web.service.dto.PromptOptimizerModeRequestDto;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/prompt-optimizer-modes")
public class PromptOptimizerModeController extends BaseRestController {

    private final PromptOptimizerModeService promptOptimizerModeService;

    public PromptOptimizerModeController(PromptOptimizerModeService promptOptimizerModeService) {
        this.promptOptimizerModeService = promptOptimizerModeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllModes() {
        try {
            List<PromptOptimizerModeDto> modes = promptOptimizerModeService.getAllModes();
            return ResponseEntity.ok(ApiResponse.success("Prompt optimizer modes fetched.", modes));
        } catch (Exception ex) {
            return handledException("GET /api/admin/prompt-optimizer-modes", ex);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getModeById(@PathVariable Long id) {
        try {
            PromptOptimizerModeDto mode = promptOptimizerModeService.getModeById(id);
            return ResponseEntity.ok(ApiResponse.success("Prompt optimizer mode fetched.", mode));
        } catch (Exception ex) {
            return handledException("GET /api/admin/prompt-optimizer-modes/{id}", ex);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createMode(@RequestBody PromptOptimizerModeRequestDto request) {
        try {
            PromptOptimizerModeDto created = promptOptimizerModeService.createMode(request);
            return new ResponseEntity<>(ApiResponse.success("Prompt optimizer mode created.", created), HttpStatus.CREATED);
        } catch (Exception ex) {
            return handledException("POST /api/admin/prompt-optimizer-modes", ex);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateMode(
            @PathVariable Long id,
            @RequestBody PromptOptimizerModeRequestDto request
    ) {
        try {
            PromptOptimizerModeDto updated = promptOptimizerModeService.updateMode(id, request);
            return ResponseEntity.ok(ApiResponse.success("Prompt optimizer mode updated.", updated));
        } catch (Exception ex) {
            return handledException("PUT /api/admin/prompt-optimizer-modes/{id}", ex);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteMode(@PathVariable Long id) {
        try {
            promptOptimizerModeService.deleteMode(id);
            return ResponseEntity.ok(ApiResponse.success("Prompt optimizer mode deleted.", null));
        } catch (Exception ex) {
            return handledException("DELETE /api/admin/prompt-optimizer-modes/{id}", ex);
        }
    }
}
