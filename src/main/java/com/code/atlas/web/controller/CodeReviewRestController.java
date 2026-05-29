package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.service.CodeReviewService;
import com.code.atlas.web.service.dto.CodeReviewMetadataDto;
import com.code.atlas.web.service.dto.CodeReviewRequestDto;
import com.code.atlas.web.service.dto.CodeReviewResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code-review")
public class CodeReviewRestController extends BaseRestController {

    private final CodeReviewService codeReviewService;

    public CodeReviewRestController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @GetMapping("/metadata")
    public ResponseEntity<ApiResponse<?>> getMetadata(@RequestParam(required = false) Long projectId) {
        try {
            CodeReviewMetadataDto metadata = codeReviewService.getMetadata(projectId);
            return ResponseEntity.ok(ApiResponse.success("Code review metadata fetched.", metadata));
        } catch (Exception ex) {
            return handledException("GET /api/code-review/metadata", ex);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> executeReview(@RequestBody CodeReviewRequestDto request) {
        try {
            CodeReviewResponseDto result = codeReviewService.runBranchCodeReview(
                    request.projectId(),
                    request.modelId(),
                    request.branchA(),
                    request.branchB()
            );
            return ResponseEntity.ok(ApiResponse.success("Code review completed.", result));
        } catch (Exception ex) {
            return handledException("POST /api/code-review", ex);
        }
    }
}
