package com.code.atlas.web.project;

import com.code.atlas.web.project.dto.ProjectRequestDto;
import com.code.atlas.web.project.dto.ProjectResponseDto;
import jakarta.transaction.Transactional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
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
                project.isUseAgentsFile()
        );
    }
}
