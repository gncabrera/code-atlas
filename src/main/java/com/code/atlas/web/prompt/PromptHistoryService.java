package com.code.atlas.web.prompt;

import com.code.atlas.web.aimodel.AIModel;
import com.code.atlas.web.project.Project;
import com.code.atlas.web.prompt.dto.PromptHistoryResponseDto;
import java.util.List;
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
                history.getMode(),
                history.isShouldSendAgentsFile(),
                history.getEstimatedTokens(),
                history.getRequestPrompt(),
                history.getResponsePrompt(),
                history.getStatus(),
                history.getErrorMessage(),
                history.getCreatedAt());
    }
}
