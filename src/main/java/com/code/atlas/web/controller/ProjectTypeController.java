package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.service.ProjectTypeService;
import com.code.atlas.web.service.dto.ProjectTypeDto;
import com.code.atlas.web.service.dto.ProjectTypeRequestDto;
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
@RequestMapping("/api/admin/project-types")
public class ProjectTypeController extends BaseRestController {

    private final ProjectTypeService projectTypeService;

    public ProjectTypeController(ProjectTypeService projectTypeService) {
        this.projectTypeService = projectTypeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllProjectTypes() {
        try {
            List<ProjectTypeDto> projectTypes = projectTypeService.getAllProjectTypes();
            return ResponseEntity.ok(ApiResponse.success("Project types fetched.", projectTypes));
        } catch (Exception ex) {
            return handledException("GET /api/admin/project-types", ex);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getProjectTypeById(@PathVariable Long id) {
        try {
            ProjectTypeDto projectType = projectTypeService.getProjectTypeById(id);
            return ResponseEntity.ok(ApiResponse.success("Project type fetched.", projectType));
        } catch (Exception ex) {
            return handledException("GET /api/admin/project-types/{id}", ex);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createProjectType(@RequestBody ProjectTypeRequestDto request) {
        try {
            ProjectTypeDto created = projectTypeService.createProjectType(request);
            return new ResponseEntity<>(ApiResponse.success("Project type created.", created), HttpStatus.CREATED);
        } catch (Exception ex) {
            return handledException("POST /api/admin/project-types", ex);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateProjectType(
            @PathVariable Long id,
            @RequestBody ProjectTypeRequestDto request
    ) {
        try {
            ProjectTypeDto updated = projectTypeService.updateProjectType(id, request);
            return ResponseEntity.ok(ApiResponse.success("Project type updated.", updated));
        } catch (Exception ex) {
            return handledException("PUT /api/admin/project-types/{id}", ex);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteProjectType(@PathVariable Long id) {
        try {
            projectTypeService.deleteProjectType(id);
            return ResponseEntity.ok(ApiResponse.success("Project type deleted.", null));
        } catch (Exception ex) {
            return handledException("DELETE /api/admin/project-types/{id}", ex);
        }
    }
}
