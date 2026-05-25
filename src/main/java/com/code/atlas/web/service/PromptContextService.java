package com.code.atlas.web.service;

import com.code.atlas.web.domain.Project;
import java.util.ArrayList;
import java.util.List;

import com.code.atlas.web.service.context.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromptContextService {

    private final ContextQueryParser contextQueryParser;
    private final ContextRetriever contextRetriever;
    private final ContextFormatter contextFormatter;
    private final int maxContextChars;

    public PromptContextService(
            ContextQueryParser contextQueryParser,
            ContextRetriever contextRetriever,
            ContextFormatter contextFormatter,
            @Value("${codeatlas.context.max-total-chars:7000}") int maxContextChars
    ) {
        this.contextQueryParser = contextQueryParser;
        this.contextRetriever = contextRetriever;
        this.contextFormatter = contextFormatter;
        this.maxContextChars = Math.max(1, maxContextChars);
    }

    public String buildContext(Project project, String userRequest) {
        if (project == null) {
            return "## Relevant Files\n\nNo project selected. Context generation skipped.";
        }
        ContextQuery query = contextQueryParser.parse(userRequest);
        List<ContextCandidate> candidates = contextRetriever.retrieve(project, query);
        List<ContextCandidate> limitedCandidates = limitByCharacterBudget(candidates);
        String formatted = contextFormatter.format(limitedCandidates);
        return appendAssumptionsIfNeeded(formatted, query);
    }

    private List<ContextCandidate> limitByCharacterBudget(List<ContextCandidate> candidates) {
        List<ContextCandidate> selected = new ArrayList<>();
        int currentSize = 0;
        for (ContextCandidate candidate : candidates) {
            int candidateSize = candidate.relativePath().length()
                    + candidate.snippet().length()
                    + String.join(" ", candidate.reasons()).length()
                    + String.join(" ", candidate.symbols()).length();
            if (!selected.isEmpty() && currentSize + candidateSize > maxContextChars) {
                break;
            }
            selected.add(candidate);
            currentSize += candidateSize;
        }
        return selected;
    }

    private String appendAssumptionsIfNeeded(String formattedContext, ContextQuery query) {
        String lowerRequest = query.rawRequest().toLowerCase();
        if (lowerRequest.contains("liquibase") && !lowerRequest.contains("flyway")) {
            return formattedContext
                    + "\n\nAssumptions:\n- Repository appears Flyway-based. Map Liquibase intent to Flyway migration changes.";
        }
        return formattedContext;
    }
}
