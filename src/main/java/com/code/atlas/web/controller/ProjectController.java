package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import com.code.atlas.web.service.ProjectService;
import com.code.atlas.web.service.SkillService;
import com.code.atlas.web.service.dto.*;

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
public class ProjectController {

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
            GlobalExceptionHandler.logCaughtException("GET /api/projects", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getProjectById(@PathVariable Long id) {
        try {
            ProjectResponseDto project = projectService.getProjectById(id);
            return ResponseEntity.ok(ApiResponse.success("Project fetched.", project));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("GET /api/projects/{id}", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createProject(@RequestBody ProjectRequestDto requestDto) {
        try {
            ProjectResponseDto created = projectService.createProject(requestDto);
            return new ResponseEntity<>(ApiResponse.success("Project created.", created), HttpStatus.CREATED);
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("POST /api/projects", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateProject(@PathVariable Long id, @RequestBody ProjectRequestDto requestDto) {
        try {
            ProjectResponseDto updated = projectService.updateProject(id, requestDto);
            return ResponseEntity.ok(ApiResponse.success("Project updated.", updated));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("PUT /api/projects/{id}", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteProject(@PathVariable Long id) {
        try {
            projectService.deleteProject(id);
            return ResponseEntity.ok(ApiResponse.success("Project deleted.", null));
        } catch (Exception ex) {
            GlobalExceptionHandler.logCaughtException("DELETE /api/projects/{id}", ex);
            return GlobalExceptionHandler.errorResponseEntity(
                    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @RestController
    @RequestMapping("/api/skills")
    public static class SkillController {

        private final SkillService skillService;

        public SkillController(SkillService skillService) {
            this.skillService = skillService;
        }

        @GetMapping
        public ResponseEntity<ApiResponse<?>> getAllSkills() {
            try {
                List<SkillDto> skills = skillService.getAllSkills();
                return ResponseEntity.ok(ApiResponse.success("Skills fetched.", skills));
            } catch (Exception ex) {
                GlobalExceptionHandler.logCaughtException("GET /api/skills", ex);
                return GlobalExceptionHandler.errorResponseEntity(
                        GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                        HttpStatus.BAD_REQUEST);
            }
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<?>> getSkillById(@PathVariable Long id) {
            try {
                SkillDto skill = skillService.getSkillById(id);
                return ResponseEntity.ok(ApiResponse.success("Skill fetched.", skill));
            } catch (Exception ex) {
                GlobalExceptionHandler.logCaughtException("GET /api/skills/{id}", ex);
                return GlobalExceptionHandler.errorResponseEntity(
                        GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                        HttpStatus.BAD_REQUEST);
            }
        }

        @PostMapping
        public ResponseEntity<ApiResponse<?>> createSkill(@RequestBody SkillCreateRequest request) {
            try {
                SkillDto created = skillService.createSkill(request);
                return new ResponseEntity<>(ApiResponse.success("Skill created.", created), HttpStatus.CREATED);
            } catch (Exception ex) {
                GlobalExceptionHandler.logCaughtException("POST /api/skills", ex);
                return GlobalExceptionHandler.errorResponseEntity(
                        GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                        HttpStatus.BAD_REQUEST);
            }
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<?>> updateSkill(@PathVariable Long id, @RequestBody SkillUpdateRequest request) {
            try {
                SkillDto updated = skillService.updateSkill(id, request);
                return ResponseEntity.ok(ApiResponse.success("Skill updated.", updated));
            } catch (Exception ex) {
                GlobalExceptionHandler.logCaughtException("PUT /api/skills/{id}", ex);
                return GlobalExceptionHandler.errorResponseEntity(
                        GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                        HttpStatus.BAD_REQUEST);
            }
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<?>> deleteSkill(@PathVariable Long id) {
            try {
                skillService.deleteSkill(id);
                return ResponseEntity.ok(ApiResponse.success("Skill deleted.", null));
            } catch (Exception ex) {
                GlobalExceptionHandler.logCaughtException("DELETE /api/skills/{id}", ex);
                return GlobalExceptionHandler.errorResponseEntity(
                        GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                        HttpStatus.BAD_REQUEST);
            }
        }

        @PostMapping("/install")
        public ResponseEntity<ApiResponse<?>> installSkills(@RequestBody SkillInstallRequest request) {
            try {
                skillService.installSkills(request);
                return ResponseEntity.ok(ApiResponse.success("Skills installed.", null));
            } catch (Exception ex) {
                GlobalExceptionHandler.logCaughtException("POST /api/skills/install", ex);
                return GlobalExceptionHandler.errorResponseEntity(
                        GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                        HttpStatus.BAD_REQUEST);
            }
        }
    }
}
