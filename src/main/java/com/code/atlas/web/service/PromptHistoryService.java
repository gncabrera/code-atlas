package com.code.atlas.web.service;

import com.code.atlas.web.domain.*;
import com.code.atlas.web.service.dto.PromptHistoryResponseDto;
import java.util.List;

import com.code.atlas.web.repository.PromptHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class PromptHistoryService {

    private final PromptHistoryRepository promptHistoryRepository;

    public PromptHistoryService(PromptHistoryRepository promptHistoryRepository) {
        this.promptHistoryRepository = promptHistoryRepository;
    }

    public List<PromptHistoryResponseDto> getAllHistory() {
        return promptHistoryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponseDto)
                .toList();
    }

    private PromptHistoryResponseDto toResponseDto(PromptHistory history) {
        Project project = history.getProject();
        AIModel aiModel = history.getAiModel();
        return new PromptHistoryResponseDto(
                history.getId(),
                project != null ? project.getId() : null,
                project != null ? project.getName() : null,
                aiModel.getId(),
                aiModel.getName(),
                history.getNotes(),
                history.getEstimatedTokens(),
                history.getRequestPrompt(),
                history.getResponsePrompt(),
                history.getStatus(),
                history.getErrorMessage(),
                history.getCreatedAt());
    }

    public PromptHistory create(Project project, AIModel model, String prompt, String notes) {

        PromptHistory history = new PromptHistory();
        history.setProject(project);
        history.setAiModel(model);
        history.setNotes(notes);
        history.setEstimatedTokens(PromptService.estimateTokens(prompt));
        history.setRequestPrompt(prompt);
        history.setStatus(PromptStatus.PENDING);
        return promptHistoryRepository.save(history);
    }

    public void success(PromptHistory history, String outputText) {
        history.setResponsePrompt(outputText);
        history.setStatus(PromptStatus.SUCCESS);
        promptHistoryRepository.save(history);
    }

    public void error(PromptHistory history, Exception ex) {
        error(history, ex.getMessage());
    }

    public void error(PromptHistory history, String message) {
        history.setStatus(PromptStatus.ERROR);
        history.setErrorMessage(message);
        promptHistoryRepository.save(history);
    }
}
