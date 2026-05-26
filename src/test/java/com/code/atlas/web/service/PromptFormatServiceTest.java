package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptFormatServiceTest {

    private final PromptFormatService promptFormatService = new PromptFormatService();

    @Test
    void formatPrompt_replacesSpacedPlaceholders() {
        String template = "Request:\n{{ USER_REQUEST }}\nContext:\n{{ CONTEXT }}";
        Map<String, String> parameters = Map.of(
                "USER_REQUEST", "fix login",
                "CONTEXT", "auth module"
        );

        String result = promptFormatService.formatPrompt(template, parameters);

        assertEquals("Request:\nfix login\nContext:\nauth module", result);
    }

    @Test
    void formatPrompt_replacesCompactAndMixedSpacingPlaceholders() {
        String template = "{{USER_REQUEST}} {{ USER_REQUEST}} {{USER_REQUEST }}";
        Map<String, String> parameters = Map.of("USER_REQUEST", "done");

        String result = promptFormatService.formatPrompt(template, parameters);

        assertEquals("done done done", result);
    }

    @Test
    void formatPrompt_leavesUnknownPlaceholdersUntouched() {
        String template = "{{ USER_REQUEST }} {{ UNKNOWN }}";
        Map<String, String> parameters = Map.of("USER_REQUEST", "ok");

        String result = promptFormatService.formatPrompt(template, parameters);

        assertEquals("ok {{ UNKNOWN }}", result);
    }

    @Test
    void formatPrompt_replacesDiffPlaceholderForCommitHelper() {
        String template = "Diff:\n{{DIFF}}\nEnd";
        Map<String, String> parameters = Map.of("DIFF", "line 1");

        String result = promptFormatService.formatPrompt(template, parameters);

        assertEquals("Diff:\nline 1\nEnd", result);
    }

    @Test
    void formatPrompt_escapesReplacementSpecialCharacters() {
        String template = "{{VALUE}}";
        Map<String, String> parameters = Map.of("VALUE", "cost is $5");

        String result = promptFormatService.formatPrompt(template, parameters);

        assertEquals("cost is $5", result);
    }

    @Test
    void formatPrompt_treatsNullValueAsEmptyString() {
        String template = "{{ AGENTS_FILE }}";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("AGENTS_FILE", null);

        String result = promptFormatService.formatPrompt(template, parameters);

        assertEquals("", result);
    }
}
