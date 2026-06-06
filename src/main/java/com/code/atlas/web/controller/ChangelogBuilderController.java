package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import com.code.atlas.web.service.ChangelogBuilderService;
import com.code.atlas.web.service.dto.ChangelogBuilderRequestDto;
import com.code.atlas.web.service.dto.ChangelogBuilderResponseDto;
import com.code.atlas.web.service.dto.GitCommitDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/changelog-builder")
public class ChangelogBuilderController {

    private final ChangelogBuilderService changelogBuilderService;

    public ChangelogBuilderController(ChangelogBuilderService changelogBuilderService) {
        this.changelogBuilderService = changelogBuilderService;
    }

    @GetMapping("/projects/{projectId}/branches")
    public ResponseEntity<ApiResponse<?>> getBranches(@PathVariable Long projectId) {
        try {
            List<String> branches = changelogBuilderService.getBranches(projectId);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Branches retrieved successfully", branches));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException(
                    "GET /api/changelog-builder/projects/{projectId}/branches",
                    ex
            );
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/projects/{projectId}/commits")
    public ResponseEntity<ApiResponse<?>> getCommits(
            @PathVariable Long projectId,
            @RequestParam String branch
    ) {
        try {
            List<GitCommitDto> commits = changelogBuilderService.getCommits(projectId, branch);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Commits retrieved successfully", commits));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException(
                    "GET /api/changelog-builder/projects/{projectId}/commits",
                    ex
            );
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<?>> generateChangelog(
            @RequestBody ChangelogBuilderRequestDto request
    ) {
        try {
            ChangelogBuilderResponseDto response = changelogBuilderService.generateChangelog(request);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Changelog generated successfully", response));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("POST /api/changelog-builder/generate", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
