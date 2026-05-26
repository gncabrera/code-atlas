package com.code.atlas.web.service;

import com.code.atlas.web.domain.PromptOptimizerMode;
import com.code.atlas.web.domain.PromptOptimizerReadOnlyMode;
import com.code.atlas.web.repository.PromptOptimizerModeRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PromptOptimizerModeSeedService {

    private static final String SEED_DIRECTORY = "db/seed/prompt-optimizer-modes/";

    private final PromptOptimizerModeRepository promptOptimizerModeRepository;

    public PromptOptimizerModeSeedService(PromptOptimizerModeRepository promptOptimizerModeRepository) {
        this.promptOptimizerModeRepository = promptOptimizerModeRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedReadOnlyModes() {
        for (PromptOptimizerReadOnlyMode readOnlyMode : PromptOptimizerReadOnlyMode.values()) {
            String code = readOnlyMode.name();
            String seedPrompt = loadSeedPrompt(readOnlyMode.templateFileName());
            Optional<PromptOptimizerMode> existing = promptOptimizerModeRepository.findByCode(code);
            if (existing.isEmpty()) {
                promptOptimizerModeRepository.save(newReadOnlyMode(code, readOnlyMode, seedPrompt));
                continue;
            }
            PromptOptimizerMode mode = existing.get();
            if (syncReadOnlyModeFromSeed(mode, readOnlyMode, seedPrompt)) {
                mode.setReadOnly(true);
                promptOptimizerModeRepository.save(mode);
            }
        }
    }

    private static PromptOptimizerMode newReadOnlyMode(
            String code, PromptOptimizerReadOnlyMode readOnlyMode, String seedPrompt) {
        PromptOptimizerMode mode = new PromptOptimizerMode();
        mode.setCode(code);
        mode.setName(readOnlyMode.displayName());
        mode.setPrompt(seedPrompt);
        mode.setHidden(false);
        mode.setReadOnly(true);
        return mode;
    }

    private static boolean syncReadOnlyModeFromSeed(
            PromptOptimizerMode mode, PromptOptimizerReadOnlyMode readOnlyMode, String seedPrompt) {
        boolean changed = false;
        if (!readOnlyMode.displayName().equals(mode.getName())) {
            mode.setName(readOnlyMode.displayName());
            changed = true;
        }
        if (!seedPrompt.equals(mode.getPrompt())) {
            mode.setPrompt(seedPrompt);
            changed = true;
        }
        return changed;
    }

    private String loadSeedPrompt(String templateFileName) {
        String resourcePath = SEED_DIRECTORY + templateFileName;
        try (InputStream inputStream = PromptOptimizerModeSeedService.class.getClassLoader()
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
