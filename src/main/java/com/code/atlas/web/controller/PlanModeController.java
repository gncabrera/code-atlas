package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.service.PlanModeService;
import com.code.atlas.web.service.dto.CreatePlanSessionRequest;
import com.code.atlas.web.service.dto.RefinePlanRequest;
import com.code.atlas.web.service.dto.SubmitAnswersRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plan-mode")
public class PlanModeController extends BaseRestController {

    private final PlanModeService planModeService;

    public PlanModeController(PlanModeService planModeService) {
        this.planModeService = planModeService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<?>> createSession(@RequestBody CreatePlanSessionRequest request) {
        try {
            var session = planModeService.createSession(request);
            return new ResponseEntity<>(ApiResponse.success("Session created.", session), HttpStatus.CREATED);
        } catch (Exception ex) {
            return handledException("POST /api/plan-mode/sessions", ex);
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<?>> listSessions() {
        try {
            List<?> sessions = planModeService.listSessions();
            return ResponseEntity.ok(ApiResponse.success("Sessions fetched.", sessions));
        } catch (Exception ex) {
            return handledException("GET /api/plan-mode/sessions", ex);
        }
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<?>> getSession(@PathVariable Long id) {
        try {
            var session = planModeService.getSessionDetail(id);
            return ResponseEntity.ok(ApiResponse.success("Session fetched.", session));
        } catch (Exception ex) {
            return handledException("GET /api/plan-mode/sessions/{id}", ex);
        }
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<?>> deleteSession(@PathVariable Long id) {
        try {
            planModeService.deleteSession(id);
            return ResponseEntity.ok(ApiResponse.success("Session deleted.", null));
        } catch (Exception ex) {
            return handledException("DELETE /api/plan-mode/sessions/{id}", ex);
        }
    }

    @PostMapping("/sessions/{id}/generate-context")
    public ResponseEntity<ApiResponse<?>> generateContext(@PathVariable Long id) {
        try {
            var session = planModeService.generateContext(id);
            return ResponseEntity.ok(ApiResponse.success("Context generated.", session));
        } catch (Exception ex) {
            return handledException("POST /api/plan-mode/sessions/{id}/generate-context", ex);
        }
    }

    @PostMapping("/sessions/{id}/generate-discovery")
    public ResponseEntity<ApiResponse<?>> generateDiscovery(@PathVariable Long id) {
        try {
            var session = planModeService.generateDiscovery(id);
            return ResponseEntity.ok(ApiResponse.success("Discovery generated.", session));
        } catch (Exception ex) {
            return handledException("POST /api/plan-mode/sessions/{id}/generate-discovery", ex);
        }
    }

    @PostMapping("/sessions/{id}/answers")
    public ResponseEntity<ApiResponse<?>> saveAnswers(
            @PathVariable Long id, @RequestBody SubmitAnswersRequest request) {
        try {
            var session = planModeService.saveAnswers(id, request);
            return ResponseEntity.ok(ApiResponse.success("Answers saved.", session));
        } catch (Exception ex) {
            return handledException("POST /api/plan-mode/sessions/{id}/answers", ex);
        }
    }

    @PostMapping("/sessions/{id}/generate-plan")
    public ResponseEntity<ApiResponse<?>> generatePlan(@PathVariable Long id) {
        try {
            var session = planModeService.generatePlan(id);
            return ResponseEntity.ok(ApiResponse.success("Plan generated.", session));
        } catch (Exception ex) {
            return handledException("POST /api/plan-mode/sessions/{id}/generate-plan", ex);
        }
    }

    @PostMapping("/sessions/{id}/refine-plan")
    public ResponseEntity<ApiResponse<?>> refinePlan(
            @PathVariable Long id, @RequestBody RefinePlanRequest request) {
        try {
            var session = planModeService.refinePlan(id, request);
            return ResponseEntity.ok(ApiResponse.success("Plan refined.", session));
        } catch (Exception ex) {
            return handledException("POST /api/plan-mode/sessions/{id}/refine-plan", ex);
        }
    }
}
