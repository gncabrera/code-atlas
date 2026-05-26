package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import com.code.atlas.web.service.ProjectService;
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

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
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
}
