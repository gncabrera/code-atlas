package com.code.atlas.web.service;

import com.code.atlas.web.domain.*;
import com.code.atlas.web.repository.AIModelApiKeyRepository;
import com.code.atlas.web.repository.AIModelRepository;
import com.code.atlas.web.service.context.indexed.ContextPipelineLogger;
import com.code.atlas.web.service.context.indexed.dto.MissingContextResponse;
import com.code.atlas.web.service.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIModelService {

    private final AIModelRepository aiModelRepository;
    private final AIModelApiKeyRepository aiModelApiKeyRepository;
    private final PromptHistoryService promptHistoryService;
    private static final int GEMINI_MAX_ATTEMPTS = 3;

    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;



    public AIModelService(
            AIModelRepository aiModelRepository,
            AIModelApiKeyRepository aiModelApiKeyRepository,
            PromptHistoryService promptHistoryService,
            @Value("${codeatlas.gemini.timeout-seconds:60}") int timeoutSeconds, ObjectMapper objectMapper
    ) {
        this.aiModelRepository = aiModelRepository;
        this.aiModelApiKeyRepository = aiModelApiKeyRepository;
        this.promptHistoryService = promptHistoryService;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
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
        return sendToModel(project, model, prompt, notes, null);
    }

    public <T> T sendToModel(Class<T> reponseClass, Project project, AIModel model, String prompt, String notes, String logLabel) {
        String raw = sendToModel(project, model, prompt, notes, logLabel).reponse();
        return JsonResponseExtractor.parseResponse(
                raw,
                reponseClass,
                objectMapper
        );
    }
    @Transactional
    public ModelResponseDto sendToModel(Project project, AIModel model, String prompt, String notes, String logLabel) {
        AIModel activeModel = getModelEntity(model.getId());
        if (!activeModel.isEnabled()) {
            throw new IllegalArgumentException("Selected AI model is disabled.");
        }
        int estimatedTokens = estimateTokens(prompt);
        if (activeModel.getTokensPerMinute() > 0 && estimatedTokens > activeModel.getTokensPerMinute()) {
            throw new IllegalArgumentException(
                    "Estimated tokens exceed model tokensPerMinute limit."
            );
        }

        boolean logLlmCall = logLabel != null && !logLabel.isBlank();
        if (logLlmCall) {
            log.info("[Context][project={}] LLM — starting: {} (model={}, estTokens={})",
                    project.getId(), logLabel, activeModel.getName(), estimatedTokens);
        }

        PromptHistory history = promptHistoryService.create(project, activeModel, prompt, notes);
        long startedNanos = System.nanoTime();

        try {
            String outputText = sendToModel(activeModel, prompt);
            promptHistoryService.success(history, outputText);
            if (logLlmCall) {
                log.info("[Context][project={}] LLM — completed: {} ({} ms, estTokens={})",
                        project.getId(), logLabel, ContextPipelineLogger.elapsedMs(startedNanos), estimatedTokens);
            }
            return new ModelResponseDto(outputText, estimatedTokens);
        } catch (Exception ex) {
            String errorDetail = ExceptionMessageFormatter.formatChain(ex);
            promptHistoryService.error(history, errorDetail);
            if (logLlmCall) {
                log.error("[Context][project={}] LLM — failed: {} ({} ms, model={}): {}",
                        project.getId(), logLabel, ContextPipelineLogger.elapsedMs(startedNanos), activeModel.getName(), errorDetail, ex);
            } else {
                log.error("Failed calling AI model (model={}, historyId={}): {}",
                        activeModel.getName(), history.getId(), errorDetail, ex);
            }
            throw new IllegalArgumentException(
                    "Failed calling AI model '" + activeModel.getName() + "': " + errorDetail
            );
        }
    }

    @Transactional
    public ModelResponseDto sendToModelMultiTurn(
            Project project,
            AIModel model,
            List<ConversationTurnDto> history,
            String newUserPrompt,
            String notes,
            String logLabel) {
        AIModel activeModel = getModelEntity(model.getId());
        if (!activeModel.isEnabled()) {
            throw new IllegalArgumentException("Selected AI model is disabled.");
        }
        List<Content> contents = buildContents(history, newUserPrompt);
        String fullPromptEstimate = history.stream()
                .map(ConversationTurnDto::content)
                .reduce("", (a, b) -> a + b) + newUserPrompt;
        int estimatedTokens = estimateTokens(fullPromptEstimate);
        if (activeModel.getTokensPerMinute() > 0 && estimatedTokens > activeModel.getTokensPerMinute()) {
            throw new IllegalArgumentException("Estimated tokens exceed model tokensPerMinute limit.");
        }

        boolean logLlmCall = logLabel != null && !logLabel.isBlank();
        if (logLlmCall) {
            log.info("[PlanMode][project={}] LLM — starting: {} (model={}, estTokens={})",
                    project != null ? project.getId() : "none", logLabel, activeModel.getName(), estimatedTokens);
        }

        PromptHistory historyRecord = promptHistoryService.create(project, activeModel, newUserPrompt, notes);
        long startedNanos = System.nanoTime();

        try {
            String outputText = sendToModelMultiTurn(activeModel, contents);
            promptHistoryService.success(historyRecord, outputText);
            if (logLlmCall) {
                log.info("[PlanMode][project={}] LLM — completed: {} ({} ms)",
                        project != null ? project.getId() : "none",
                        logLabel, ContextPipelineLogger.elapsedMs(startedNanos));
            }
            return new ModelResponseDto(outputText, estimatedTokens);
        } catch (Exception ex) {
            String errorDetail = ExceptionMessageFormatter.formatChain(ex);
            promptHistoryService.error(historyRecord, errorDetail);
            if (logLlmCall) {
                log.error("[PlanMode][project={}] LLM — failed: {} ({} ms): {}",
                        project != null ? project.getId() : "none",
                        logLabel, ContextPipelineLogger.elapsedMs(startedNanos), errorDetail, ex);
            }
            throw new IllegalArgumentException(
                    "Failed calling AI model '" + activeModel.getName() + "': " + errorDetail);
        }
    }

    private static List<Content> buildContents(List<ConversationTurnDto> history, String newUserPrompt) {
        List<Content> contents = new ArrayList<>();
        for (ConversationTurnDto turn : history) {
            contents.add(Content.builder()
                    .role(turn.role().toLowerCase())
                    .parts(ImmutableList.of(Part.fromText(turn.content())))
                    .build());
        }
        contents.add(Content.builder()
                .role("user")
                .parts(ImmutableList.of(Part.fromText(newUserPrompt)))
                .build());
        return contents;
    }

    @Nullable
    private String sendToModel(AIModel model, String prompt) {
        String apiKeyValue = resolveApiKeyValue(model);
        HttpOptions httpOptions = buildHttpOptions();
        GenerateContentResponse response;
        try (Client client = Client.builder()
                .apiKey(apiKeyValue)
                .httpOptions(httpOptions)
                .build()) {
            response = client.models.generateContent(model.getName(), prompt, null);
        }
        return response.text();
    }

    @Nullable
    private String sendToModelMultiTurn(AIModel model, List<Content> contents) {
        String apiKeyValue = resolveApiKeyValue(model);
        HttpOptions httpOptions = buildHttpOptions();
        GenerateContentResponse response;
        try (Client client = Client.builder()
                .apiKey(apiKeyValue)
                .httpOptions(httpOptions)
                .build()) {
            response = client.models.generateContent(model.getName(), contents, null);
        }
        return response.text();
    }

    private HttpOptions buildHttpOptions() {
        return HttpOptions.builder()
                .timeout(timeoutSeconds * 1000)
                .retryOptions(HttpRetryOptions.builder()
                        .attempts(GEMINI_MAX_ATTEMPTS)
                        .httpStatusCodes(408, 429, 500, 502, 503, 504)
                        .initialDelay(1.0)
                        .expBase(2.0)
                        .build())
                .build();
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
