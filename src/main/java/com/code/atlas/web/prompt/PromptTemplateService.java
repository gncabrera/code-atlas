package com.code.atlas.web.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    private static final String PROMPTS_DIRECTORY = "prompts/";

    public String loadTemplate(PromptMode mode) {
        String templatePath = PROMPTS_DIRECTORY + mode.templateFileName();
        try {
            try (InputStream inputStream = PromptTemplateService.class.getClassLoader().getResourceAsStream(templatePath)) {
                if (inputStream == null) {
                    throw new IllegalArgumentException("Template file not found in classpath: " + templatePath);
                }
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed reading template file: " + templatePath);
        }
    }
}
