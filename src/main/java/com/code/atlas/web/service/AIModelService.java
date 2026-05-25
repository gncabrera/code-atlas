package com.code.atlas.web.service;

import com.code.atlas.web.domain.*;
import com.code.atlas.web.repository.AIModelApiKeyRepository;
import com.code.atlas.web.repository.AIModelRepository;
import com.code.atlas.web.service.dto.*;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import jakarta.transaction.Transactional;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AIModelService {

    private final AIModelRepository aiModelRepository;
    private final AIModelApiKeyRepository aiModelApiKeyRepository;
    private final PromptHistoryService promptHistoryService;
    private final int timeoutSeconds;


    public AIModelService(
            AIModelRepository aiModelRepository,
            AIModelApiKeyRepository aiModelApiKeyRepository,
            PromptHistoryService promptHistoryService,
            @Value("${codeatlas.gemini.timeout-seconds:60}") int timeoutSeconds
    ) {
        this.aiModelRepository = aiModelRepository;
        this.aiModelApiKeyRepository = aiModelApiKeyRepository;
        this.promptHistoryService = promptHistoryService;
        this.timeoutSeconds = timeoutSeconds;
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

    @Transactional
    public ModelResponseDto sendToModel(Project project, AIModel model, String prompt, String notes) {
        if (!model.isEnabled()) {
            throw new IllegalArgumentException("Selected AI model is disabled.");
        }
        int estimatedTokens = estimateTokens(prompt);
        if (model.getTokensPerMinute() > 0 && estimatedTokens > model.getTokensPerMinute()) {
            throw new IllegalArgumentException(
                    "Estimated tokens exceed model tokensPerMinute limit."
            );
        }

        PromptHistory history = promptHistoryService.create(project, model, prompt, notes);

        try {
            String apiKeyValue = resolveApiKeyValue(model);
            HttpOptions httpOptions = HttpOptions.builder()
                    .timeout(timeoutSeconds * 1000)
                    .build();
            Client client = Client.builder()
                    .apiKey(apiKeyValue)
                    .httpOptions(httpOptions)
                    .build();
            GenerateContentResponse response = client.models.generateContent(model.getName(), prompt, null);
            String outputText = response.text();
            promptHistoryService.success(history, outputText);
            return new ModelResponseDto(outputText, estimatedTokens);
        } catch (Exception ex) {
            promptHistoryService.error(history, ex);
            throw new IllegalArgumentException("Failed calling AI model: " + ex.getMessage());
        }
    }

    public static int estimateTokens(String input) {
        int characters = input == null ? 0 : input.length();
        return (characters + 3) / 4;
    }

    private String resolveApiKeyValue(AIModel model) {
        AIModelApiKey apiKey = model.getAiModelApiKey();
        if (apiKey == null) {
            throw new IllegalArgumentException("AI model has no API key assigned.");
        }
        if (!apiKey.isActive()) {
            throw new IllegalArgumentException("Assigned API key is inactive.");
        }
        String value = apiKey.getApiKey();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Assigned API key has no value.");
        }
        return value.trim();
    }
}
