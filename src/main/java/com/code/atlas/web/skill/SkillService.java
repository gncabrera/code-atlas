package com.code.atlas.web.skill;

import com.code.atlas.web.project.Project;
import com.code.atlas.web.project.ProjectService;
import com.code.atlas.web.skill.dto.SkillCreateRequest;
import com.code.atlas.web.skill.dto.SkillDto;
import com.code.atlas.web.skill.dto.SkillInstallRequest;
import com.code.atlas.web.skill.dto.SkillUpdateRequest;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final ProjectService projectService;

    public SkillService(SkillRepository skillRepository, ProjectService projectService) {
        this.skillRepository = skillRepository;
        this.projectService = projectService;
    }

    public List<SkillDto> getAllSkills() {
        return skillRepository.findAll().stream().map(this::toDto).toList();
    }

    public SkillDto getSkillById(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found for id: " + id));
        return toDto(skill);
    }

    @Transactional
    public SkillDto createSkill(SkillCreateRequest request) {
        Skill skill = new Skill();
        applyRequest(skill, request.name(), request.prompt(), request.targetPath(),
                request.description(), request.category());
        return toDto(skillRepository.save(skill));
    }

    @Transactional
    public SkillDto updateSkill(Long id, SkillUpdateRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found for id: " + id));
        applyRequest(skill, request.name(), request.prompt(), request.targetPath(),
                request.description(), request.category());
        return toDto(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found for id: " + id));
        skillRepository.delete(skill);
    }

    public void installSkills(SkillInstallRequest request) throws IOException {
        Project project = projectService.getProjectEntity(request.projectId());
        Path projectRoot = Paths.get(project.getPath()).normalize();

        if (!Files.exists(projectRoot)) {
            throw new IllegalArgumentException("Project path does not exist: " + projectRoot);
        }
        if (!Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Project path is not a directory: " + projectRoot);
        }
        if (!Files.isReadable(projectRoot)) {
            throw new IllegalArgumentException("Project path is not readable: " + projectRoot);
        }
        if (!Files.isWritable(projectRoot)) {
            throw new IllegalArgumentException("Project path is not writable: " + projectRoot);
        }

        for (Long skillId : request.skillIds()) {
            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new IllegalArgumentException("Skill not found for id: " + skillId));

            Path target = projectRoot.resolve(skill.getTargetPath().trim()).normalize();
            if (!target.startsWith(projectRoot)) {
                throw new IllegalArgumentException("Target path escapes project directory.");
            }

            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    target,
                    skill.getPrompt(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void applyRequest(
            Skill skill,
            String name,
            String prompt,
            String targetPath,
            String description,
            String category) {
        skill.setName(name.trim());
        skill.setPrompt(prompt);
        skill.setTargetPath(targetPath.trim());
        skill.setDescription(normalizeOptional(description));
        skill.setCategory(normalizeOptional(category));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SkillDto toDto(Skill skill) {
        return new SkillDto(
                skill.getId(),
                skill.getName(),
                skill.getPrompt(),
                skill.getTargetPath(),
                skill.getDescription(),
                skill.getCategory());
    }
}
