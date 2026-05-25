package com.code.atlas.web.service.context;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ContextSnippetExtractor {

    public String extractSnippet(
            List<String> lines,
            String fileContentLowerCase,
            ContextQuery query,
            int maxLines,
            int maxChars
    ) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        int anchorLine = findAnchorLine(lines, fileContentLowerCase, query);
        int start = Math.max(0, anchorLine - 8);
        int endExclusive = Math.min(lines.size(), start + maxLines);
        StringBuilder snippetBuilder = new StringBuilder();
        for (int index = start; index < endExclusive; index++) {
            String line = lines.get(index);
            if (snippetBuilder.length() + line.length() + 1 > maxChars) {
                snippetBuilder.append("\n// ... truncated ...");
                break;
            }
            snippetBuilder.append(line).append('\n');
        }
        return snippetBuilder.toString().trim();
    }

    private int findAnchorLine(List<String> lines, String fileLowerCase, ContextQuery query) {
        if (query.hasEndpoint()) {
            String endpointPath = query.endpointPath().toLowerCase(Locale.ROOT);
            int endpointLine = findLineIndexContaining(lines, endpointPath);
            if (endpointLine >= 0) {
                return endpointLine;
            }
        }
        for (String keyword : query.keywords()) {
            int lineIndex = findLineIndexContaining(lines, keyword.toLowerCase(Locale.ROOT));
            if (lineIndex >= 0) {
                return lineIndex;
            }
        }
        if (fileLowerCase.contains("@restcontroller")) {
            int restControllerLine = findLineIndexContaining(lines, "@RestController".toLowerCase(Locale.ROOT));
            if (restControllerLine >= 0) {
                return restControllerLine;
            }
        }
        return 0;
    }

    private int findLineIndexContaining(List<String> lines, String needle) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).toLowerCase(Locale.ROOT).contains(needle)) {
                return i;
            }
        }
        return -1;
    }
}
