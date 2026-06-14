package com.code.atlas.web.service;

import com.code.atlas.web.domain.PlanModePrompt;
import com.code.atlas.web.domain.PlanModePromptType;
import com.code.atlas.web.repository.PlanModePromptRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanModePromptSeedService {

    private static final String SEED_DIRECTORY = "db/seed/plan-mode-prompts/";

    private final PlanModePromptRepository planModePromptRepository;

    public PlanModePromptSeedService(PlanModePromptRepository planModePromptRepository) {
        this.planModePromptRepository = planModePromptRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedPrompts() {
        for (PlanModePromptType type : PlanModePromptType.values()) {
            String code = type.name();
            String seedPrompt = loadSeedPrompt(type.templateFileName());
            Optional<PlanModePrompt> existing = planModePromptRepository.findByCode(code);
            if (existing.isEmpty()) {
                planModePromptRepository.save(newPrompt(code, type, seedPrompt));
                continue;
            }
            PlanModePrompt prompt = existing.get();
            if (syncFromSeed(prompt, type, seedPrompt)) {
                planModePromptRepository.save(prompt);
            }
        }
    }

    private static PlanModePrompt newPrompt(String code, PlanModePromptType type, String seedPrompt) {
        PlanModePrompt prompt = new PlanModePrompt();
        prompt.setCode(code);
        prompt.setName(type.displayName());
        prompt.setPrompt(seedPrompt);
        return prompt;
    }

    private static boolean syncFromSeed(PlanModePrompt prompt, PlanModePromptType type, String seedPrompt) {
        boolean changed = false;
        if (!type.displayName().equals(prompt.getName())) {
            prompt.setName(type.displayName());
            changed = true;
        }
        if (!seedPrompt.equals(prompt.getPrompt())) {
            prompt.setPrompt(seedPrompt);
            changed = true;
        }
        return changed;
    }

    private String loadSeedPrompt(String templateFileName) {
        String resourcePath = SEED_DIRECTORY + templateFileName;
        try (InputStream inputStream = PlanModePromptSeedService.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Seed prompt file not found in classpath: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed reading seed prompt file: " + resourcePath, ex);
        }
    }
}
