package com.code.atlas.web.service.context;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContextFormatter {

    public String format(List<ContextCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "## Relevant Files\n\nNo high-confidence context files found for this request.";
        }
        StringBuilder builder = new StringBuilder("## Relevant Files\n\n");
        int index = 1;
        for (ContextCandidate candidate : candidates) {
            builder.append("### ").append(index).append(". ").append(candidate.relativePath()).append('\n');
            builder.append("Score: ").append(candidate.score()).append('\n');
            builder.append("Reason:\n");
            for (String reason : candidate.reasons()) {
                builder.append("- ").append(reason).append('\n');
            }
            builder.append("\nSymbols:\n");
            if (candidate.symbols().isEmpty()) {
                builder.append("- None extracted\n\n");
            } else {
                for (String symbol : candidate.symbols()) {
                    builder.append("- ").append(symbol).append('\n');
                }
                builder.append('\n');
            }
            builder.append("```").append(candidate.snippetLanguage()).append('\n');
            builder.append(candidate.snippet().isBlank() ? "// No snippet extracted" : candidate.snippet()).append('\n');
            builder.append("```\n\n");
            index++;
        }
        return builder.toString().trim();
    }
}
