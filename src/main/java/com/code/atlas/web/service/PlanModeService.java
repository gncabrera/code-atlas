package com.code.atlas.web.service;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.PlanConversationMessage;
import com.code.atlas.web.domain.PlanDiscovery;
import com.code.atlas.web.domain.PlanModePromptType;
import com.code.atlas.web.domain.PlanOutputType;
import com.code.atlas.web.domain.PlanResult;
import com.code.atlas.web.domain.PlanSession;
import com.code.atlas.web.domain.PlanSessionStatus;
import com.code.atlas.web.domain.PlanThreadType;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.repository.PlanConversationMessageRepository;
import com.code.atlas.web.repository.PlanDiscoveryRepository;
import com.code.atlas.web.repository.PlanModePromptRepository;
import com.code.atlas.web.repository.PlanResultRepository;
import com.code.atlas.web.repository.PlanSessionRepository;
import com.code.atlas.web.repository.ProjectRepository;
import com.code.atlas.web.service.dto.ConversationMessageDto;
import com.code.atlas.web.service.dto.ConversationTurnDto;
import com.code.atlas.web.service.dto.CreatePlanSessionRequest;
import com.code.atlas.web.service.dto.PlanDiscoveryDto;
import com.code.atlas.web.service.dto.PlanModePromptDto;
import com.code.atlas.web.service.dto.PlanModePromptRequestDto;
import com.code.atlas.web.service.dto.PlanQuestionDto;
import com.code.atlas.web.service.dto.PlanResultDto;
import com.code.atlas.web.service.dto.PlanSessionDetailDto;
import com.code.atlas.web.service.dto.PlanSessionSummaryDto;
import com.code.atlas.web.service.dto.PlanSuggestionDto;
import com.code.atlas.web.service.dto.RefinePlanRequest;
import com.code.atlas.web.service.dto.SubmitAnswersRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanModeService {

    private final PlanSessionRepository planSessionRepository;
    private final PlanDiscoveryRepository planDiscoveryRepository;
    private final PlanConversationMessageRepository planConversationMessageRepository;
    private final PlanResultRepository planResultRepository;
    private final PlanModePromptRepository planModePromptRepository;
    private final ProjectRepository projectRepository;
    private final AIModelService aiModelService;
    private final PromptContextService promptContextService;
    private final PromptFormatService promptFormatService;
    private final ObjectMapper objectMapper;

    public PlanModeService(
            PlanSessionRepository planSessionRepository,
            PlanDiscoveryRepository planDiscoveryRepository,
            PlanConversationMessageRepository planConversationMessageRepository,
            PlanResultRepository planResultRepository,
            PlanModePromptRepository planModePromptRepository,
            ProjectRepository projectRepository,
            AIModelService aiModelService,
            PromptContextService promptContextService,
            PromptFormatService promptFormatService,
            ObjectMapper objectMapper
    ) {
        this.planSessionRepository = planSessionRepository;
        this.planDiscoveryRepository = planDiscoveryRepository;
        this.planConversationMessageRepository = planConversationMessageRepository;
        this.planResultRepository = planResultRepository;
        this.planModePromptRepository = planModePromptRepository;
        this.projectRepository = projectRepository;
        this.aiModelService = aiModelService;
        this.promptContextService = promptContextService;
        this.promptFormatService = promptFormatService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlanSessionDetailDto createSession(CreatePlanSessionRequest request) {
        PlanOutputType outputType = resolveOutputType(request.outputType());
        AIModel contextModel = aiModelService.getModelEntity(request.contextModelId());
        AIModel planModel = aiModelService.getModelEntity(request.planModelId());

        PlanSession session = new PlanSession();
        session.setTitle(buildTitle(request.userRequest()));
        session.setUserRequest(request.userRequest().trim());
        session.setOutputType(outputType);
        session.setContextModel(contextModel);
        session.setPlanModel(planModel);
        session.setStatus(PlanSessionStatus.DRAFT);
        session.setUpdatedAt(LocalDateTime.now());

        if (request.projectId() != null) {
            Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.projectId()));
            session.setProject(project);
        }

        return toDetailDto(planSessionRepository.save(session));
    }

    public List<PlanSessionSummaryDto> listSessions() {
        return planSessionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public PlanSessionDetailDto getSessionDetail(Long id) {
        return toDetailDto(findSession(id));
    }

    @Transactional
    public void deleteSession(Long id) {
        PlanSession session = findSession(id);
        planConversationMessageRepository.deleteBySessionId(id);
        planDiscoveryRepository.findBySessionId(id).ifPresent(planDiscoveryRepository::delete);
        planResultRepository.findBySessionId(id).ifPresent(planResultRepository::delete);
        planSessionRepository.delete(session);
    }

    @Transactional
    public PlanSessionDetailDto generateContext(Long id) {
        PlanSession session = findSession(id);
        AIModel contextModel = session.getContextModel();
        Project project = session.getProject();

        String contextData = promptContextService.buildIndexedContext(
                project, session.getUserRequest(), contextModel);

        session.setContextData(contextData);
        session.setStatus(PlanSessionStatus.CONTEXT_READY);
        session.setUpdatedAt(LocalDateTime.now());
        return toDetailDto(planSessionRepository.save(session));
    }

    @Transactional
    public PlanSessionDetailDto generateDiscovery(Long id) {
        PlanSession session = findSession(id);
        String discoveryPromptTemplate = loadPromptTemplate(PlanModePromptType.DISCOVERY);

        String prompt = promptFormatService.formatPrompt(discoveryPromptTemplate, Map.of(
                "CONTEXT", nullToEmpty(session.getContextData()),
                "USER_REQUEST", session.getUserRequest(),
                "OUTPUT_TYPE", session.getOutputType().displayName()
        ));

        String rawResponse = aiModelService.sendToModel(
                session.getProject(), session.getPlanModel(), prompt,
                "Plan Mode — Discovery", "PlanMode:discovery").reponse();

        PlanDiscoveryDto discoveryDto = JsonResponseExtractor.parseResponse(
                rawResponse, PlanDiscoveryDto.class, objectMapper);

        String questionsJson = toJson(discoveryDto.questions());
        String suggestionsJson = toJson(discoveryDto.suggestions());

        PlanDiscovery discovery = planDiscoveryRepository.findBySessionId(id)
                .orElseGet(() -> {
                    PlanDiscovery d = new PlanDiscovery();
                    d.setSession(session);
                    return d;
                });
        discovery.setQuestionsJson(questionsJson);
        discovery.setSuggestionsJson(suggestionsJson);
        discovery.setAnswersJson(null);
        discovery.setSelectedSuggestionsJson(null);
        planDiscoveryRepository.save(discovery);

        saveConversationMessages(session, PlanThreadType.DISCOVERY, prompt, rawResponse);

        session.setStatus(PlanSessionStatus.DISCOVERY_READY);
        session.setUpdatedAt(LocalDateTime.now());
        return toDetailDto(planSessionRepository.save(session));
    }

    @Transactional
    public PlanSessionDetailDto saveAnswers(Long id, SubmitAnswersRequest request) {
        PlanSession session = findSession(id);
        PlanDiscovery discovery = planDiscoveryRepository.findBySessionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Discovery not generated yet for session: " + id));

        discovery.setAnswersJson(toJson(request.answers()));
        discovery.setSelectedSuggestionsJson(toJson(request.selectedSuggestions()));
        planDiscoveryRepository.save(discovery);

        session.setStatus(PlanSessionStatus.ANSWERED);
        session.setUpdatedAt(LocalDateTime.now());
        return toDetailDto(planSessionRepository.save(session));
    }

    @Transactional
    public PlanSessionDetailDto generatePlan(Long id) {
        PlanSession session = findSession(id);
        PlanDiscovery discovery = planDiscoveryRepository.findBySessionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Discovery not generated yet for session: " + id));

        PlanModePromptType planPromptType = PlanModePromptType.planPromptFor(session.getOutputType());
        String planPromptTemplate = loadPromptTemplate(planPromptType);

        String questionsAndAnswers = buildQuestionsAndAnswersSummary(
                discovery.getQuestionsJson(), discovery.getAnswersJson());
        String selectedSuggestions = buildSelectedSuggestionsSummary(
                discovery.getSuggestionsJson(), discovery.getSelectedSuggestionsJson());

        String newUserPrompt = promptFormatService.formatPrompt(planPromptTemplate, Map.of(
                "CONTEXT", nullToEmpty(session.getContextData()),
                "USER_REQUEST", session.getUserRequest(),
                "QUESTIONS_AND_ANSWERS", questionsAndAnswers,
                "SELECTED_SUGGESTIONS", selectedSuggestions,
                "OUTPUT_TYPE", session.getOutputType().displayName()
        ));

        List<ConversationTurnDto> planHistory = loadPlanHistory(id);
        String rawResponse = aiModelService.sendToModelMultiTurn(
                session.getProject(), session.getPlanModel(), planHistory, newUserPrompt,
                "Plan Mode — Plan Generation", "PlanMode:generate-plan").reponse();

        saveConversationMessages(session, PlanThreadType.PLAN, newUserPrompt, rawResponse);

        PlanResult result = planResultRepository.findBySessionId(id).orElseGet(() -> {
            PlanResult r = new PlanResult();
            r.setSession(session);
            return r;
        });
        result.setGeneratedPlan(rawResponse);
        planResultRepository.save(result);

        session.setStatus(PlanSessionStatus.PLAN_READY);
        session.setUpdatedAt(LocalDateTime.now());
        return toDetailDto(planSessionRepository.save(session));
    }

    @Transactional
    public PlanSessionDetailDto refinePlan(Long id, RefinePlanRequest request) {
        PlanSession session = findSession(id);
        planResultRepository.findBySessionId(id)
                .orElseThrow(() -> new IllegalArgumentException("No plan generated yet for session: " + id));

        List<ConversationTurnDto> planHistory = loadPlanHistory(id);
        String rawResponse = aiModelService.sendToModelMultiTurn(
                session.getProject(), session.getPlanModel(), planHistory, request.userMessage(),
                "Plan Mode — Plan Refinement", "PlanMode:refine-plan").reponse();

        saveConversationMessages(session, PlanThreadType.PLAN, request.userMessage(), rawResponse);

        PlanResult result = planResultRepository.findBySessionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan result missing for session: " + id));
        result.setGeneratedPlan(rawResponse);
        planResultRepository.save(result);

        session.setUpdatedAt(LocalDateTime.now());
        return toDetailDto(planSessionRepository.save(session));
    }

    public List<PlanModePromptDto> listPrompts() {
        return planModePromptRepository.findAll().stream()
                .map(p -> new PlanModePromptDto(p.getId(), p.getCode(), p.getName(), p.getPrompt()))
                .toList();
    }

    public PlanModePromptDto getPromptById(Long id) {
        return planModePromptRepository.findById(id)
                .map(p -> new PlanModePromptDto(p.getId(), p.getCode(), p.getName(), p.getPrompt()))
                .orElseThrow(() -> new IllegalArgumentException("Plan mode prompt not found: " + id));
    }

    @Transactional
    public PlanModePromptDto updatePrompt(Long id, PlanModePromptRequestDto request) {
        var prompt = planModePromptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan mode prompt not found: " + id));
        prompt.setPrompt(request.prompt().trim());
        var saved = planModePromptRepository.save(prompt);
        return new PlanModePromptDto(saved.getId(), saved.getCode(), saved.getName(), saved.getPrompt());
    }

    private PlanSession findSession(Long id) {
        return planSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan session not found: " + id));
    }

    private String loadPromptTemplate(PlanModePromptType type) {
        return planModePromptRepository.findByCode(type.name())
                .map(com.code.atlas.web.domain.PlanModePrompt::getPrompt)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plan mode prompt not found for type: " + type.name()));
    }

    private void saveConversationMessages(
            PlanSession session, PlanThreadType threadType, String userPrompt, String modelResponse) {
        PlanConversationMessage userMsg = new PlanConversationMessage();
        userMsg.setSession(session);
        userMsg.setThreadType(threadType);
        userMsg.setRole("user");
        userMsg.setContent(userPrompt);
        planConversationMessageRepository.save(userMsg);

        PlanConversationMessage modelMsg = new PlanConversationMessage();
        modelMsg.setSession(session);
        modelMsg.setThreadType(threadType);
        modelMsg.setRole("model");
        modelMsg.setContent(modelResponse);
        planConversationMessageRepository.save(modelMsg);
    }

    private List<ConversationTurnDto> loadPlanHistory(Long sessionId) {
        return planConversationMessageRepository
                .findBySessionIdAndThreadTypeOrderByCreatedAtAsc(sessionId, PlanThreadType.PLAN)
                .stream()
                .map(m -> new ConversationTurnDto(m.getRole(), m.getContent()))
                .toList();
    }

    private String buildQuestionsAndAnswersSummary(String questionsJson, String answersJson) {
        if (questionsJson == null || questionsJson.isBlank()) {
            return "No questions were generated.";
        }
        try {
            List<PlanQuestionDto> questions = objectMapper.readValue(
                    questionsJson, new TypeReference<>() {});
            Map<String, String> answers = answersJson != null
                    ? objectMapper.readValue(answersJson, new TypeReference<>() {})
                    : Map.of();

            StringBuilder sb = new StringBuilder();
            for (PlanQuestionDto q : questions) {
                sb.append("Q: ").append(q.question()).append("\n");
                String selectedId = answers.get(q.id());
                if (selectedId != null) {
                    q.options().stream()
                            .filter(o -> o.id().equals(selectedId))
                            .findFirst()
                            .ifPresent(o -> sb.append("A: ").append(o.text()).append("\n"));
                } else {
                    sb.append("A: (not answered)\n");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (JsonProcessingException ex) {
            return "Questions/answers could not be parsed.";
        }
    }

    private String buildSelectedSuggestionsSummary(String suggestionsJson, String selectedJson) {
        if (suggestionsJson == null || selectedJson == null) {
            return "No suggestions selected.";
        }
        try {
            List<PlanSuggestionDto> suggestions = objectMapper.readValue(
                    suggestionsJson, new TypeReference<>() {});
            List<String> selectedTitles = objectMapper.readValue(
                    selectedJson, new TypeReference<>() {});

            List<PlanSuggestionDto> selected = suggestions.stream()
                    .filter(s -> selectedTitles.contains(s.title()))
                    .toList();

            if (selected.isEmpty()) {
                return "No suggestions selected.";
            }

            StringBuilder sb = new StringBuilder();
            for (PlanSuggestionDto s : selected) {
                sb.append("- ").append(s.title()).append(": ").append(s.change()).append("\n");
            }
            return sb.toString().trim();
        } catch (JsonProcessingException ex) {
            return "Suggestions could not be parsed.";
        }
    }

    private PlanSessionSummaryDto toSummaryDto(PlanSession session) {
        return new PlanSessionSummaryDto(
                session.getId(),
                session.getTitle(),
                session.getOutputType().name(),
                session.getOutputType().displayName(),
                session.getStatus().name(),
                session.getCreatedAt()
        );
    }

    private PlanSessionDetailDto toDetailDto(PlanSession session) {
        PlanDiscovery discovery = planDiscoveryRepository.findBySessionId(session.getId()).orElse(null);
        PlanResult result = planResultRepository.findBySessionId(session.getId()).orElse(null);

        PlanDiscoveryDto discoveryDto = discovery != null ? parseDiscovery(discovery) : null;
        Map<String, String> answers = discovery != null ? parseAnswers(discovery.getAnswersJson()) : null;
        List<String> selectedSuggestions = discovery != null
                ? parseSelectedSuggestions(discovery.getSelectedSuggestionsJson()) : null;

        PlanResultDto resultDto = null;
        if (result != null) {
            List<ConversationMessageDto> messages = planConversationMessageRepository
                    .findBySessionIdAndThreadTypeOrderByCreatedAtAsc(session.getId(), PlanThreadType.PLAN)
                    .stream()
                    .map(m -> new ConversationMessageDto(m.getRole(), m.getContent(), m.getCreatedAt()))
                    .toList();
            resultDto = new PlanResultDto(result.getGeneratedPlan(), result.getCreatedAt(), messages);
        }

        return new PlanSessionDetailDto(
                session.getId(),
                session.getTitle(),
                session.getUserRequest(),
                session.getOutputType().name(),
                session.getOutputType().displayName(),
                session.getProject() != null ? session.getProject().getId() : null,
                session.getProject() != null ? session.getProject().getName() : null,
                session.getContextModel().getId(),
                session.getContextModel().getName(),
                session.getPlanModel().getId(),
                session.getPlanModel().getName(),
                session.getContextData(),
                session.getStatus().name(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                discoveryDto,
                answers,
                selectedSuggestions,
                resultDto
        );
    }

    private PlanDiscoveryDto parseDiscovery(PlanDiscovery discovery) {
        try {
            List<PlanQuestionDto> questions = objectMapper.readValue(
                    discovery.getQuestionsJson(), new TypeReference<>() {});
            List<PlanSuggestionDto> suggestions = objectMapper.readValue(
                    discovery.getSuggestionsJson(), new TypeReference<>() {});
            return new PlanDiscoveryDto(questions, suggestions);
        } catch (JsonProcessingException ex) {
            return new PlanDiscoveryDto(List.of(), List.of());
        }
    }

    private Map<String, String> parseAnswers(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return new HashMap<>();
        }
    }

    private List<String> parseSelectedSuggestions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private static String buildTitle(String userRequest) {
        String trimmed = userRequest.trim();
        if (trimmed.length() <= 60) {
            return trimmed;
        }
        return trimmed.substring(0, 57) + "...";
    }

    private static PlanOutputType resolveOutputType(String value) {
        try {
            return PlanOutputType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid output type: " + value);
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize object to JSON", ex);
        }
    }
}
