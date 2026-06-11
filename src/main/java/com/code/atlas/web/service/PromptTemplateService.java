package com.code.atlas.web.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class PromptTemplateService {

    private PromptTemplateService() {
    }

    public static String load(PromptTemplate template) {
        try (InputStream stream = PromptTemplateService.class.getClassLoader().getResourceAsStream(template.getResourcePath())) {
            if (stream == null) {
                throw new IllegalStateException("Prompt template not found: " + template.getResourcePath());
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed loading prompt template: " + template.getResourcePath(), ex);
        }
    }
}
