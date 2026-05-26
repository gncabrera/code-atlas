package com.code.atlas.web.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PromptFormatService {

    public String formatPrompt(String template, Map<String, String> parameters) {
        if (template == null || parameters == null || parameters.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            Pattern placeholder = Pattern.compile("\\{\\{\\s*" + Pattern.quote(key) + "\\s*\\}\\}");
            result = placeholder.matcher(result).replaceAll(Matcher.quoteReplacement(value));
        }
        return result;
    }
}
