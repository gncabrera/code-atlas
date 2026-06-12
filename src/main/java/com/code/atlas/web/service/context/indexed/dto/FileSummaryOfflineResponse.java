package com.code.atlas.web.service.context.indexed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileSummaryOfflineResponse(String filePath, Metadata metadata) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            String summary,
            List<String> keywords,
            List<String> concepts,
            List<String> responsibilities,
            List<String> capabilities,
            List<String> symbols,
            List<String> dependencies,
            List<String> externalReferences,
            List<String> dataStructures,
            List<String> contracts,
            List<String> entryPoints,
            List<String> outputs,
            List<String> sideEffects,
            List<String> patterns,
            List<String> frameworks,
            List<String> technologies,
            String architecturalRole,
            String executionContext,
            List<String> businessDomains,
            List<String> relatedTopics,
            List<String> changeImpactAreas,
            List<String> implementationHints,
            String searchText,
            Confidence confidence
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Confidence(
            Double overall,
            Double summary,
            Double concepts,
            Double responsibilities,
            Double capabilities,
            Double dependencies,
            Double architecturalRole
    ) {
    }
}
