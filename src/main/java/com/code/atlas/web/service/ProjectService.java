package com.code.atlas.web.service;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectProjectType;
import com.code.atlas.web.domain.ProjectType;
import com.code.atlas.web.repository.ProjectProjectTypeRepository;
import com.code.atlas.web.repository.ProjectRepository;
import com.code.atlas.web.repository.ProjectTypeRepository;
import com.code.atlas.web.service.dto.ProjectRequestDto;
import com.code.atlas.web.service.dto.ProjectResponseDto;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectProjectTypeRepository projectProjectTypeRepository;
    private final ProjectTypeRepository projectTypeRepository;
    private final GitProcessRunner gitProcessRunner;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectProjectTypeRepository projectProjectTypeRepository,
            ProjectTypeRepository projectTypeRepository,
            GitProcessRunner gitProcessRunner) {
        this.projectRepository = projectRepository;
        this.projectProjectTypeRepository = projectProjectTypeRepository;
        this.projectTypeRepository = projectTypeRepository;
        this.gitProcessRunner = gitProcessRunner;
    }

    public List<ProjectResponseDto> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        if (projects.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ProjectProjectType>> assignmentsByProjectId = loadAssignmentsByProjectId(
                projects.stream().map(Project::getId).toList());
        return projects.stream()
                .map(project -> toResponseDto(project, assignmentsByProjectId.getOrDefault(project.getId(), List.of())))
                .toList();
    }

    public ProjectResponseDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found for id: " + id));
        return toResponseDto(project, projectProjectTypeRepository.findByProjectIdOrderByProjectTypeNameAsc(id));
    }

    @Transactional
    public ProjectResponseDto createProject(ProjectRequestDto requestDto) {
        Path normalizedPath = validateAndNormalizePath(requestDto.path());
        validateProjectTypeIds(requestDto.projectTypeIds());
        Project project = new Project();
        project.setPath(normalizedPath.toString());
        project.setName(requestDto.name().trim());
        project.setDescription(requestDto.description().trim());
        project.setUseAgentsFile(requestDto.useAgentsFile());
        project.setUseDesignFile(requestDto.useDesignFile());
        Project saved = projectRepository.save(project);
        syncProjectTypes(saved, requestDto.projectTypeIds());
        return getProjectById(saved.getId());
    }

    @Transactional
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto requestDto) {
        Path normalizedPath = validateAndNormalizePath(requestDto.path());
        validateProjectTypeIds(requestDto.projectTypeIds());
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found for id: " + id));
        project.setPath(normalizedPath.toString());
        project.setName(requestDto.name().trim());
        project.setDescription(requestDto.description().trim());
        project.setUseAgentsFile(requestDto.useAgentsFile());
        project.setUseDesignFile(requestDto.useDesignFile());
        projectRepository.save(project);
        syncProjectTypes(project, requestDto.projectTypeIds());
        return getProjectById(id);
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

    private ProjectResponseDto toResponseDto(Project project, List<ProjectProjectType> assignments) {
        List<Long> projectTypeIds = assignments.stream()
                .map(assignment -> assignment.getProjectType().getId())
                .toList();
        List<String> projectTypeNames = assignments.stream()
                .map(assignment -> assignment.getProjectType().getName())
                .toList();
        return new ProjectResponseDto(
                project.getId(),
                project.getPath(),
                project.getName(),
                project.getDescription(),
                project.isUseAgentsFile(),
                project.isUseDesignFile(),
                projectTypeIds,
                projectTypeNames
        );
    }

    private Map<Long, List<ProjectProjectType>> loadAssignmentsByProjectId(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return projectProjectTypeRepository.findByProjectIdInOrderByProjectIdAscProjectTypeNameAsc(projectIds)
                .stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getProject().getId()));
    }

    private void validateProjectTypeIds(List<Long> projectTypeIds) {
        if (projectTypeIds == null || projectTypeIds.isEmpty()) {
            throw new IllegalArgumentException("At least one project type is required.");
        }
    }

    private void syncProjectTypes(Project project, List<Long> projectTypeIds) {
        List<Long> normalizedIds = normalizeProjectTypeIds(projectTypeIds);
        projectProjectTypeRepository.deleteByProjectId(project.getId());
        if (normalizedIds.isEmpty()) {
            return;
        }
        List<ProjectType> projectTypes = projectTypeRepository.findAllById(normalizedIds);
        if (projectTypes.size() != normalizedIds.size()) {
            throw new IllegalArgumentException("One or more project types were not found.");
        }
        Map<Long, ProjectType> projectTypeById = projectTypes.stream()
                .collect(Collectors.toMap(ProjectType::getId, type -> type));
        for (Long projectTypeId : normalizedIds) {
            ProjectProjectType assignment = new ProjectProjectType();
            assignment.setProject(project);
            assignment.setProjectType(projectTypeById.get(projectTypeId));
            projectProjectTypeRepository.save(assignment);
        }
    }

    private List<Long> normalizeProjectTypeIds(List<Long> projectTypeIds) {
        if (projectTypeIds == null || projectTypeIds.isEmpty()) {
            return List.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long projectTypeId : projectTypeIds) {
            if (projectTypeId == null) {
                continue;
            }
            normalized.add(projectTypeId);
        }
        return List.copyOf(normalized);
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

    public Path resolveProjectRoot(Project project) {
        Path projectRoot = Paths.get(project.getPath()).normalize();
        if (!Files.exists(projectRoot)) {
            throw new IllegalArgumentException("Project path does not exist: " + projectRoot);
        }
        if (!Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Project path is not a directory: " + projectRoot);
        }
        return projectRoot;
    }
}
