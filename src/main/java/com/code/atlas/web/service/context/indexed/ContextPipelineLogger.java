package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.domain.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ContextPipelineLogger {

    public void stepStart(Project project, String phase, int step, int totalSteps, String label) {
        log.info("[Context][project={}][phase={}][step={}/{}] {} — starting",
                project.getId(), phase, step, totalSteps, label);
    }

    public void stepComplete(Project project, String phase, int step, int totalSteps, String label, long durationMs) {
        log.info("[Context][project={}][phase={}][step={}/{}] {} — completed ({} ms)",
                project.getId(), phase, step, totalSteps, label, durationMs);
    }

    public void stepFailed(Project project, String phase, int step, int totalSteps, String label, long durationMs, String reason) {
        log.warn("[Context][project={}][phase={}][step={}/{}] {} — failed ({} ms): {}",
                project.getId(), phase, step, totalSteps, label, durationMs, reason);
    }

    public void message(Project project, String phase, String text) {
        log.info("[Context][project={}][phase={}] {}", project.getId(), phase, text);
    }

    public static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}
