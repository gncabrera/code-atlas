package com.code.atlas.web.service;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.repository.ProjectRepository;
import com.code.atlas.web.service.dto.ProjectRequestDto;
import com.code.atlas.web.service.dto.ProjectResponseDto;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final GitProcessRunner gitProcessRunner;

    public ProjectService(ProjectRepository projectRepository, GitProcessRunner gitProcessRunner) {
        this.projectRepository = projectRepository;
        this.gitProcessRunner = gitProcessRunner;
    }

    public List<ProjectResponseDto> getAllProjects() {
        return projectRepository.findAll().stream().map(this::toResponseDto).toList();
    }

    public ProjectResponseDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found for id: " + id));
        return toResponseDto(project);
    }

    @Transactional
    public ProjectResponseDto createProject(ProjectRequestDto requestDto) {
        Path normalizedPath = validateAndNormalizePath(requestDto.path());
        Project project = new Project();
        project.setPath(normalizedPath.toString());
        project.setName(requestDto.name().trim());
        project.setDescription(requestDto.description().trim());
        project.setUseAgentsFile(requestDto.useAgentsFile());
        project.setUseDesignFile(requestDto.useDesignFile());
        return toResponseDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto requestDto) {
        Path normalizedPath = validateAndNormalizePath(requestDto.path());
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found for id: " + id));
        project.setPath(normalizedPath.toString());
        project.setName(requestDto.name().trim());
        project.setDescription(requestDto.description().trim());
        project.setUseAgentsFile(requestDto.useAgentsFile());
        project.setUseDesignFile(requestDto.useDesignFile());
        return toResponseDto(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found for id: " + id));
        projectRepository.delete(project);
    }

    public Project getProjectEntity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found for id: " + id));
    }

    private Path validateAndNormalizePath(String rawPath) {
        Path normalizedPath = Paths.get(rawPath.trim()).normalize();
        if (!Files.exists(normalizedPath)) {
            throw new IllegalArgumentException("Project path does not exist: " + normalizedPath);
        }
        return normalizedPath;
    }

    private ProjectResponseDto toResponseDto(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getPath(),
                project.getName(),
                project.getDescription(),
                project.isUseAgentsFile(),
                project.isUseDesignFile()
        );
    }

    public List<String> getProjectFiles(Project project) {
        if (project == null || project.getPath() == null || project.getPath().isBlank()) {
            return List.of();
        }
        Path projectRoot = Paths.get(project.getPath()).normalize();
        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            return List.of();
        }
        try {
            String isRepo = gitProcessRunner.run(projectRoot, List.of("git", "rev-parse", "--is-inside-work-tree"));
            if (!"true".equalsIgnoreCase(isRepo.trim())) {
                return List.of();
            }
            return gitProcessRunner.listTrackedFiles(projectRoot);
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    public String resolveAgentsFileContent(Project project) {
        if (project == null) {
            return "";
        }
        if (!project.isUseAgentsFile()) {
            return "";
        }
        Path agentsPath = Path.of(project.getPath(), "AGENTS.md").normalize();
        if (!Files.exists(agentsPath)) {
            return "No AGENTS.md found";
        }
        try {
            return "AGENTS.md\n\n" + Files.readString(agentsPath);
        } catch (IOException ex) {
            return "No AGENTS.md found";
        }
    }

    public String resolveDesignFileContent(Project project) {
        if (project == null) {
            return "";
        }
        if (!project.isUseDesignFile()) {
            return "";
        }
        Path designPath = Path.of(project.getPath(), "DESIGN.md").normalize();
        if (!Files.exists(designPath)) {
            return "No DESIGN.md found";
        }
        try {
            return "DESIGN.md\n\n" + Files.readString(designPath);
        } catch (IOException ex) {
            return "No DESIGN.md found";
        }
    }
}
