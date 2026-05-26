package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code.atlas.web.service.dto.CodeReviewResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonResponseExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseResponse_parsesRawJsonObject() {
        String raw = """
                {"summary":{"score":9,"risk":"LOW","mainConcerns":[]},"findings":[]}
                """;

        CodeReviewResponseDto response = JsonResponseExtractor.parseResponse(raw, CodeReviewResponseDto.class, objectMapper);

        assertEquals(9, response.summary().score());
    }

    @Test
    void parseResponse_prefersJsonFencedBlockOverProseAndDecoyBraces() {
        String raw = """
                Here is the review { "decoy": true }
                ```json
                {
                  "summary": { "score": 6, "risk": "HIGH", "mainConcerns": ["Auth"] },
                  "findings": []
                }
                ```
                Trailing note { "also": "ignored" }
                """;

        CodeReviewResponseDto response = JsonResponseExtractor.parseResponse(raw, CodeReviewResponseDto.class, objectMapper);

        assertEquals(6, response.summary().score());
        assertEquals("HIGH", response.summary().risk());
        assertEquals(List.of("Auth"), response.summary().mainConcerns());
    }

    @Test
    void parseResponse_parsesGenericFenceWhenJsonTagMissing() {
        String raw = """
                ```
                {"summary":{"score":5,"risk":"MEDIUM","mainConcerns":[]},"findings":[]}
                ```
                """;

        CodeReviewResponseDto response = JsonResponseExtractor.parseResponse(raw, CodeReviewResponseDto.class, objectMapper);

        assertEquals(5, response.summary().score());
    }

    @Test
    void parseResponse_handlesBracesInsideStringValues() {
        String raw = """
                {
                  "summary": { "score": 4, "risk": "LOW", "mainConcerns": [] },
                  "findings": [
                    {
                      "severity": "LOW",
                      "category": "MAINTAINABILITY",
                      "title": "Patch",
                      "file": "src/App.java",
                      "line": 1,
                      "description": "Use map",
                      "impact": "Readability",
                      "suggestion": "Refactor",
                      "suggestedPatch": "return { ok: true };"
                    }
                  ]
                }
                """;

        CodeReviewResponseDto response = JsonResponseExtractor.parseResponse(raw, CodeReviewResponseDto.class, objectMapper);

        assertEquals(1, response.findings().size());
        assertEquals("return { ok: true };", response.findings().getFirst().suggestedPatch());
    }

    @Test
    void parseResponse_parsesFirstValidObjectWhenMultipleObjectsPresent() {
        String raw = """
                {"summary":{"score":2,"risk":"LOW","mainConcerns":[]},"findings":[]}
                {"summary":{"score":8,"risk":"HIGH","mainConcerns":["Late"]},"findings":[]}
                """;

        CodeReviewResponseDto response = JsonResponseExtractor.parseResponse(raw, CodeReviewResponseDto.class, objectMapper);

        assertEquals(2, response.summary().score());
    }

    @Test
    void parseResponse_rejectsEmptyInput() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> JsonResponseExtractor.parseResponse("  ", CodeReviewResponseDto.class, objectMapper)
        );

        assertEquals("AI model returned an empty response.", ex.getMessage());
    }

    @Test
    void parseResponse_failsWhenNoValidJsonObject() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> JsonResponseExtractor.parseResponse("not json at all", CodeReviewResponseDto.class, objectMapper)
        );

        assertTrue(ex.getMessage().startsWith("Failed parsing JSON response:"));
    }

    @Test
    void collectCandidates_ordersFenceBeforeBalancedScan() {
        List<String> candidates = JsonResponseExtractor.collectCandidates("""
                {"summary":{"score":1,"risk":"LOW","mainConcerns":[]},"findings":[]}
                ```json
                {"summary":{"score":9,"risk":"LOW","mainConcerns":[]},"findings":[]}
                ```
                """);

        assertTrue(candidates.indexOf("""
                {"summary":{"score":9,"risk":"LOW","mainConcerns":[]},"findings":[]}""") < candidates.indexOf("""
                {"summary":{"score":1,"risk":"LOW","mainConcerns":[]},"findings":[]}"""));
    }
}
