package com.code.atlas.web.service.context.indexed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class IndexedPromptLoader {

    private IndexedPromptLoader() {
    }

    public static String load(String classpathResource) {
        try (InputStream stream = IndexedPromptLoader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (stream == null) {
                throw new IllegalStateException("Prompt template not found: " + classpathResource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed loading prompt template: " + classpathResource, ex);
        }
    }
}
