package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.ProjectIndexService;
import com.code.atlas.web.service.ProjectService;
import com.code.atlas.web.service.context.indexed.IndexBuilderService;
import com.code.atlas.web.service.dto.OfflineIndexJobResponseDto;
import com.code.atlas.web.service.dto.OfflineIndexRequestDto;
import com.code.atlas.web.service.dto.ProjectRequestDto;
import com.code.atlas.web.service.dto.ProjectResponseDto;
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
@RequestMapping("/api/projects")
public class ProjectController extends BaseRestController {

    private final ProjectService projectService;
    private final ProjectIndexService projectIndexService;
    private final IndexBuilderService indexBuilderService;
    private final AIModelService aiModelService;

    public ProjectController(
            ProjectService projectService,
            ProjectIndexService projectIndexService,
            IndexBuilderService indexBuilderService,
            AIModelService aiModelService
    ) {
        this.projectService = projectService;
        this.projectIndexService = projectIndexService;
        this.indexBuilderService = indexBuilderService;
        this.aiModelService = aiModelService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllProjects() {
        try {
            List<ProjectResponseDto> projects = projectService.getAllProjects();
            return ResponseEntity.ok(ApiResponse.success("Projects fetched.", projects));
        } catch (Exception ex) {
            return handledException("GET /api/projects", ex);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getProjectById(@PathVariable Long id) {
        try {
            ProjectResponseDto project = projectService.getProjectById(id);
            return ResponseEntity.ok(ApiResponse.success("Project fetched.", project));
        } catch (Exception ex) {
            return handledException("GET /api/projects/{id}", ex);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createProject(@RequestBody ProjectRequestDto requestDto) {
        try {
            ProjectResponseDto created = projectService.createProject(requestDto);
            return new ResponseEntity<>(ApiResponse.success("Project created.", created), HttpStatus.CREATED);
        } catch (Exception ex) {
            return handledException("POST /api/projects", ex);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateProject(@PathVariable Long id, @RequestBody ProjectRequestDto requestDto) {
        try {
            ProjectResponseDto updated = projectService.updateProject(id, requestDto);
            return ResponseEntity.ok(ApiResponse.success("Project updated.", updated));
        } catch (Exception ex) {
            return handledException("PUT /api/projects/{id}", ex);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteProject(@PathVariable Long id) {
        try {
            projectService.deleteProject(id);
            return ResponseEntity.ok(ApiResponse.success("Project deleted.", null));
        } catch (Exception ex) {
            return handledException("DELETE /api/projects/{id}", ex);
        }
    }

    @GetMapping("/{id}/index/offline/status")
    public ResponseEntity<ApiResponse<?>> getOfflineIndexJobStatus(@PathVariable Long id) {
        try {
            projectService.getProjectEntity(id);
            OfflineIndexJobResponseDto status = indexBuilderService.getJobStatus(id);
            if (status == null) {
                return ResponseEntity.ok(ApiResponse.success("No offline index job found for project.", null));
            }
            return ResponseEntity.ok(ApiResponse.success("Offline index job status fetched.", status));
        } catch (Exception ex) {
            return handledException("GET /api/projects/{id}/index/offline/status", ex);
        }
    }

    @PostMapping("/{id}/index/offline")
    public ResponseEntity<ApiResponse<?>> regenerateOfflineIndex(
            @PathVariable Long id,
            @RequestBody OfflineIndexRequestDto requestDto
    ) {
        try {
            Project project = projectService.getProjectEntity(id);
            aiModelService.getModelEntity(requestDto.aiModelId());
            projectIndexService.refreshIndex(project, "offline");
            OfflineIndexJobResponseDto job = indexBuilderService.startRegenerateAsync(id, requestDto.aiModelId());
            return new ResponseEntity<>(ApiResponse.success("Offline index regeneration started.", job), HttpStatus.ACCEPTED);
        } catch (Exception ex) {
            return handledException("POST /api/projects/{id}/index/offline", ex);
        }
    }

    @PostMapping("/{id}/index/offline/incremental")
    public ResponseEntity<ApiResponse<?>> regenerateOfflineIndexIncremental(
            @PathVariable Long id,
            @RequestBody OfflineIndexRequestDto requestDto
    ) {
        try {
            Project project = projectService.getProjectEntity(id);
            aiModelService.getModelEntity(requestDto.aiModelId());
            projectIndexService.refreshIndex(project, "offline");
            OfflineIndexJobResponseDto job = indexBuilderService.startRegenerateIncrementalAsync(id, requestDto.aiModelId());
            return new ResponseEntity<>(ApiResponse.success("Offline incremental index regeneration started.", job), HttpStatus.ACCEPTED);
        } catch (Exception ex) {
            return handledException("POST /api/projects/{id}/index/offline/incremental", ex);
        }
    }
}
