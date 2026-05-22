package com.code.atlas.web.aimodel;

import com.code.atlas.web.aimodel.dto.AIModelApiKeyDto;
import com.code.atlas.web.aimodel.dto.AIModelRequestDto;
import com.code.atlas.web.aimodel.dto.AIModelResponseDto;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AIModelService {

    private final AIModelRepository aiModelRepository;
    private final AIModelApiKeyRepository aiModelApiKeyRepository;

    public AIModelService(AIModelRepository aiModelRepository, AIModelApiKeyRepository aiModelApiKeyRepository) {
        this.aiModelRepository = aiModelRepository;
        this.aiModelApiKeyRepository = aiModelApiKeyRepository;
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
        model.setDescription(normalizeDescription(requestDto.description()));
        model.setEnabled(requestDto.enabled());
        model.setTokensPerMinute(requestDto.tokensPerMinute());
        model.setRequestsPerMinute(requestDto.requestsPerMinute());
        model.setRequestsPerDay(requestDto.requestsPerDay());
        model.setAiModelApiKey(resolveApiKeyLink(requestDto.apiKey()));
    }

    private AIModelApiKey resolveApiKeyLink(AIModelApiKeyDto apiKeyDto) {
        if (apiKeyDto == null || apiKeyDto.id() == null) {
            return null;
        }
        AIModelApiKey apiKey = aiModelApiKeyRepository.findById(apiKeyDto.id())
                .orElseThrow(() -> new IllegalArgumentException("API key not found for id: " + apiKeyDto.id()));
        if (!apiKey.isActive()) {
            throw new IllegalArgumentException("Selected API key is inactive.");
        }
        return apiKey;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description.trim();
    }

    private AIModelResponseDto toResponseDto(AIModel model) {
        return new AIModelResponseDto(
                model.getId(),
                model.getName(),
                model.getDescription(),
                model.isEnabled(),
                model.getTokensPerMinute(),
                model.getRequestsPerMinute(),
                model.getRequestsPerDay(),
                toApiKeyDto(model.getAiModelApiKey())
        );
    }

    private AIModelApiKeyDto toApiKeyDto(AIModelApiKey apiKey) {
        if (apiKey == null) {
            return null;
        }
        return new AIModelApiKeyDto(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getApiKey(),
                apiKey.getProvider(),
                apiKey.isActive()
        );
    }
}
