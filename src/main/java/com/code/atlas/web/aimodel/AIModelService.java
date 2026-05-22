package com.code.atlas.web.aimodel;

import com.code.atlas.web.aimodel.dto.AIModelRequestDto;
import com.code.atlas.web.aimodel.dto.AIModelResponseDto;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AIModelService {

    private final AIModelRepository aiModelRepository;

    public AIModelService(AIModelRepository aiModelRepository) {
        this.aiModelRepository = aiModelRepository;
    }

    public List<AIModelResponseDto> getAllModels() {
        return aiModelRepository.findAll().stream().map(this::toResponseDto).toList();
    }

    public List<AIModelResponseDto> getEnabledModels() {
        return aiModelRepository.findByEnabledTrue().stream().map(this::toResponseDto).toList();
    }

    public AIModelResponseDto getModelById(Long id) {
        AIModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI model not found for id: " + id));
        return toResponseDto(model);
    }

    @Transactional
    public AIModelResponseDto createModel(AIModelRequestDto requestDto) {
        AIModel model = new AIModel();
        updateEntity(model, requestDto);
        return toResponseDto(aiModelRepository.save(model));
    }

    @Transactional
    public AIModelResponseDto updateModel(Long id, AIModelRequestDto requestDto) {
        AIModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI model not found for id: " + id));
        updateEntity(model, requestDto);
        return toResponseDto(aiModelRepository.save(model));
    }

    @Transactional
    public void deleteModel(Long id) {
        AIModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI model not found for id: " + id));
        aiModelRepository.delete(model);
    }

    public AIModel getModelEntity(Long id) {
        return aiModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI model not found for id: " + id));
    }

    private void updateEntity(AIModel model, AIModelRequestDto requestDto) {
        model.setName(requestDto.name().trim());
        model.setEnabled(requestDto.enabled());
        model.setTokensPerMinute(requestDto.tokensPerMinute());
        model.setRequestsPerMinute(requestDto.requestsPerMinute());
        model.setRequestsPerDay(requestDto.requestsPerDay());
        model.setApiKey(requestDto.apiKey().trim());
    }

    private AIModelResponseDto toResponseDto(AIModel model) {
        return new AIModelResponseDto(
                model.getId(),
                model.getName(),
                model.isEnabled(),
                model.getTokensPerMinute(),
                model.getRequestsPerMinute(),
                model.getRequestsPerDay(),
                model.getApiKey()
        );
    }
}
