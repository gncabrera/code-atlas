package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import com.code.atlas.web.service.CommitHelperService;
import com.code.atlas.web.service.dto.CommitActionRequestDto;
import com.code.atlas.web.service.dto.CommitHelperMetadataDto;
import com.code.atlas.web.service.dto.GenerateCommitRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commit-helper")
public class CommitHelperController {

    private final CommitHelperService commitHelperService;

    public CommitHelperController(CommitHelperService commitHelperService) {
        this.commitHelperService = commitHelperService;
    }

    @GetMapping("/metadata")
    public ResponseEntity<ApiResponse<?>> getMetadata() {
        try {
            CommitHelperMetadataDto metadata = commitHelperService.getMetadata();
            return ResponseEntity.ok(ApiResponse.success("Commit helper metadata fetched.", metadata));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("GET /api/commit-helper/metadata", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<?>> generateCommitMessage(@RequestBody GenerateCommitRequestDto requestDto) {
        try {
            String commitMessage = commitHelperService.generateCommitMessage(
                    requestDto.projectId(),
                    requestDto.aiModelId()
            );
            return ResponseEntity.ok(ApiResponse.success("Commit message generated.", commitMessage));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("POST /api/commit-helper/generate", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/commit")
    public ResponseEntity<ApiResponse<?>> commit(@RequestBody CommitActionRequestDto requestDto) {
        try {
            commitHelperService.executeCommit(requestDto.projectId(), requestDto.commitMessage());
            return ResponseEntity.ok(ApiResponse.success("Changes committed.", null));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("POST /api/commit-helper/commit", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/push")
    public ResponseEntity<ApiResponse<?>> commitAndPush(@RequestBody CommitActionRequestDto requestDto) {
        try {
            commitHelperService.executeCommitAndPush(requestDto.projectId(), requestDto.commitMessage());
            return ResponseEntity.ok(ApiResponse.success("Changes committed and pushed.", null));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("POST /api/commit-helper/push", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }
}
