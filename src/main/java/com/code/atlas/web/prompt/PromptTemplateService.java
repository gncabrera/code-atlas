package com.code.atlas.web.prompt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    private static final String PROMPTS_DIRECTORY = "prompts";

    public String loadTemplate(PromptMode mode) {
        Path templatePath = Path.of(PROMPTS_DIRECTORY, mode.templateFileName()).normalize();
        if (!Files.exists(templatePath)) {
            throw new IllegalArgumentException("Template file not found: " + templatePath);
        }
        try {
            return Files.readString(templatePath);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed reading template file: " + templatePath);
        }
    }
}
