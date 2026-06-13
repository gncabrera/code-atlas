package com.code.atlas.web.service;

import com.code.atlas.web.domain.ProjectType;
import com.code.atlas.web.repository.ProjectProjectTypeRepository;
import com.code.atlas.web.repository.ProjectTypeRepository;
import com.code.atlas.web.service.dto.ProjectTypeDto;
import com.code.atlas.web.service.dto.ProjectTypeRequestDto;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProjectTypeService {

    private final ProjectTypeRepository projectTypeRepository;
    private final ProjectProjectTypeRepository projectProjectTypeRepository;

    public ProjectTypeService(
            ProjectTypeRepository projectTypeRepository,
            ProjectProjectTypeRepository projectProjectTypeRepository) {
        this.projectTypeRepository = projectTypeRepository;
        this.projectProjectTypeRepository = projectProjectTypeRepository;
    }

    public List<ProjectTypeDto> getAllProjectTypes() {
        return projectTypeRepository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public ProjectTypeDto getProjectTypeById(Long id) {
        return toDto(findEntity(id));
    }

    public ProjectType findEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Project type id is required.");
        }
        return projectTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project type not found for id: " + id));
    }

    @Transactional
    public ProjectTypeDto createProjectType(ProjectTypeRequestDto request) {
        String normalizedName = request.name().trim();
        if (projectTypeRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Project type name already exists: " + normalizedName);
        }
        ProjectType projectType = new ProjectType();
        projectType.setName(normalizedName);
        projectType.setAllowedExtensions(normalizeAllowedExtensions(request.allowedExtensions()));
        projectType.setDescription(normalizeDescription(request.description()));
        return toDto(projectTypeRepository.save(projectType));
    }

    @Transactional
    public ProjectTypeDto updateProjectType(Long id, ProjectTypeRequestDto request) {
        ProjectType existing = findEntity(id);
        String normalizedName = request.name().trim();
        if (projectTypeRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new IllegalArgumentException("Project type name already exists: " + normalizedName);
        }
        existing.setName(normalizedName);
        existing.setAllowedExtensions(normalizeAllowedExtensions(request.allowedExtensions()));
        existing.setDescription(normalizeDescription(request.description()));
        return toDto(projectTypeRepository.save(existing));
    }

    @Transactional
    public void deleteProjectType(Long id) {
        ProjectType existing = findEntity(id);
        if (projectProjectTypeRepository.existsByProjectTypeId(id)) {
            throw new IllegalArgumentException("Project type is assigned to one or more projects and cannot be deleted.");
        }
        projectTypeRepository.delete(existing);
    }

    public String normalizeAllowedExtensions(String rawExtensions) {
        if (rawExtensions == null || rawExtensions.isBlank()) {
            throw new IllegalArgumentException("Allowed extensions are required.");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String segment : rawExtensions.split(",")) {
            String extension = segment.trim().toLowerCase(Locale.ROOT);
            if (extension.isEmpty()) {
                continue;
            }
            if (!extension.matches("[a-z0-9]+")) {
                throw new IllegalArgumentException("Invalid extension: " + segment.trim());
            }
            normalized.add(extension);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed extension is required.");
        }
        return String.join(",", normalized);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProjectTypeDto toDto(ProjectType projectType) {
        return new ProjectTypeDto(
                projectType.getId(),
                projectType.getName(),
                projectType.getAllowedExtensions(),
                projectType.getDescription()
        );
    }
}
