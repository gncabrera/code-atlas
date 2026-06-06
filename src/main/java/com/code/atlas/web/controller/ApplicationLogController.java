package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.service.ApplicationLogService;
import com.code.atlas.web.service.dto.LogTailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class ApplicationLogController extends BaseRestController {

    private final ApplicationLogService applicationLogService;

    public ApplicationLogController(ApplicationLogService applicationLogService) {
        this.applicationLogService = applicationLogService;
    }

    @GetMapping("/tail")
    public ResponseEntity<ApiResponse<?>> tailLog(@RequestParam(required = false) Integer lines) {
        try {
            LogTailResponse response = applicationLogService.tailLog(lines);
            return ResponseEntity.ok(ApiResponse.success("Log tail fetched.", response));
        } catch (Exception ex) {
            return handledException("GET /api/logs/tail", ex);
        }
    }
}
