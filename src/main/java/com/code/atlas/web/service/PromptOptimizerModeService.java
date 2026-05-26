package com.code.atlas.web.service;

import com.code.atlas.web.domain.PromptOptimizerMode;
import com.code.atlas.web.domain.PromptOptimizerReadOnlyMode;
import com.code.atlas.web.repository.PromptOptimizerModeRepository;
import com.code.atlas.web.service.dto.PromptOptimizerModeDto;
import com.code.atlas.web.service.dto.PromptOptimizerModeRequestDto;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromptOptimizerModeService {

    private final PromptOptimizerModeRepository promptOptimizerModeRepository;

    public PromptOptimizerModeService(PromptOptimizerModeRepository promptOptimizerModeRepository) {
        this.promptOptimizerModeRepository = promptOptimizerModeRepository;
    }

    public List<PromptOptimizerModeDto> getAllModes() {
        return promptOptimizerModeRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<PromptOptimizerModeDto> getVisibleModes() {
        return promptOptimizerModeRepository.findAllByHiddenFalseOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public PromptOptimizerModeDto getModeById(Long id) {
        return toDto(findEntity(id));
    }

    public PromptOptimizerMode getModeEntity(Long id) {
        return findEntity(id);
    }

    @Transactional
    public PromptOptimizerModeDto createMode(PromptOptimizerModeRequestDto request) {
        validateCreateRequest(request);
        String normalizedCode = normalizeCode(request.code());
        if (promptOptimizerModeRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Prompt optimizer mode code already exists: " + normalizedCode);
        }
        PromptOptimizerMode mode = new PromptOptimizerMode();
        mode.setCode(normalizedCode);
        mode.setName(request.name().trim());
        mode.setPrompt(request.prompt().trim());
        mode.setHidden(request.hidden());
        mode.setReadOnly(false);
        return toDto(promptOptimizerModeRepository.save(mode));
    }

    @Transactional
    public PromptOptimizerModeDto updateMode(Long id, PromptOptimizerModeRequestDto request) {
        PromptOptimizerMode existing = findEntity(id);
        if (existing.isReadOnly()) {
            if (request.code() != null && !normalizeCode(request.code()).equals(existing.getCode())) {
                throw new IllegalArgumentException("Read-only prompt mode code cannot be changed.");
            }
            if (request.name() != null && !request.name().trim().equals(existing.getName().trim())) {
                throw new IllegalArgumentException("Read-only prompt mode name cannot be changed.");
            }
            if (request.prompt() != null && !request.prompt().trim().equals(existing.getPrompt().trim())) {
                throw new IllegalArgumentException("Read-only prompt mode prompt cannot be changed.");
            }
            existing.setHidden(request.hidden());
            return toDto(promptOptimizerModeRepository.save(existing));
        }
        validateUpdateRequest(request);
        existing.setCode(normalizeCode(request.code()));
        existing.setName(request.name().trim());
        existing.setPrompt(request.prompt().trim());
        existing.setHidden(request.hidden());
        return toDto(promptOptimizerModeRepository.save(existing));
    }

    @Transactional
    public void deleteMode(Long id) {
        PromptOptimizerMode existing = findEntity(id);
        if (existing.isReadOnly()) {
            throw new IllegalArgumentException("Read-only prompt mode cannot be deleted.");
        }
        promptOptimizerModeRepository.delete(existing);
    }

    private PromptOptimizerMode findEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Prompt optimizer mode id is required.");
        }
        return promptOptimizerModeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt optimizer mode not found for id: " + id));
    }

    private void validateCreateRequest(PromptOptimizerModeRequestDto request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("Mode code is required.");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Mode name is required.");
        }
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new IllegalArgumentException("Mode prompt is required.");
        }
        if (PromptOptimizerReadOnlyMode.isProtectedCode(request.code())) {
            throw new IllegalArgumentException("Mode code is reserved for a system prompt mode.");
        }
    }

    private void validateUpdateRequest(PromptOptimizerModeRequestDto request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("Mode code is required.");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Mode name is required.");
        }
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new IllegalArgumentException("Mode prompt is required.");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private PromptOptimizerModeDto toDto(PromptOptimizerMode mode) {
        return new PromptOptimizerModeDto(
                mode.getId(),
                mode.getCode(),
                mode.getName(),
                mode.getPrompt(),
                mode.isHidden(),
                mode.isReadOnly()
        );
    }
}
