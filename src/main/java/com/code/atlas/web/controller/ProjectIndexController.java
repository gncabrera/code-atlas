package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.ProjectIndexExportImportService;
import com.code.atlas.web.service.ProjectIndexService;
import com.code.atlas.web.service.ProjectService;
import com.code.atlas.web.service.dto.ImportPreflightDto;
import com.code.atlas.web.service.dto.IndexClearRequestDto;
import com.code.atlas.web.service.dto.IndexImportConfirmRequestDto;
import com.code.atlas.web.service.dto.IndexStatusDto;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/projects/{id}/index")
public class ProjectIndexController extends BaseRestController {

    private final ProjectService projectService;
    private final ProjectIndexService projectIndexService;
    private final ProjectIndexExportImportService projectIndexExportImportService;

    public ProjectIndexController(ProjectService projectService, ProjectIndexService projectIndexService, ProjectIndexExportImportService projectIndexExportImportService) {
        this.projectService = projectService;
        this.projectIndexService = projectIndexService;
        this.projectIndexExportImportService = projectIndexExportImportService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<?>> getIndexStatus(@PathVariable Long id) {
        try {
            IndexStatusDto status = projectIndexService.getIndexStatus(id);
            return ResponseEntity.ok(ApiResponse.success("Project index status fetched.", status));
        } catch (Exception ex) {
            return handledException("GET /api/projects/{id}/index/status", ex);
        }
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportIndex(@PathVariable Long id) {
        try {
            projectService.getProjectEntity(id);
            StreamingResponseBody body = outputStream -> {
                try {
                    projectIndexExportImportService.writeExportStream(id, outputStream);
                } catch (IOException ex) {
                    GlobalExceptionHandler.logCaughtException("GET /api/projects/{id}/index/export", ex);
                    throw new IllegalStateException("Failed streaming project index export.", ex);
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project-" + id + "-index.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("GET /api/projects/{id}/index/export", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/import/preflight")
    public ResponseEntity<ApiResponse<?>> preflightImport(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            validateJsonUpload(file);
            ImportPreflightDto preflight = projectIndexExportImportService.analyzeImport(id, file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success("Import preflight completed.", preflight));
        } catch (Exception ex) {
            return handledException("POST /api/projects/{id}/index/import/preflight", ex);
        }
    }

    @PostMapping("/import/confirm")
    public ResponseEntity<ApiResponse<?>> confirmImport(
            @PathVariable Long id,
            @RequestBody IndexImportConfirmRequestDto requestDto
    ) {
        try {
            projectIndexExportImportService.confirmImport(id, requestDto.preflightId());
            return ResponseEntity.ok(ApiResponse.success("Project index imported.", null));
        } catch (Exception ex) {
            return handledException("POST /api/projects/{id}/index/import/confirm", ex);
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<?>> clearIndex(
            @PathVariable Long id,
            @RequestBody IndexClearRequestDto requestDto
    ) {
        try {
            Project project = projectService.getProjectEntity(id);
            projectIndexService.clearIndex(id, requestDto.confirmationText(), project.getName());
            return ResponseEntity.ok(ApiResponse.success("Project index cleared.", null));
        } catch (Exception ex) {
            return handledException("DELETE /api/projects/{id}/index/clear", ex);
        }
    }

    private void validateJsonUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Import file is required.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("Import file must have a .json extension.");
        }
    }
}
