package com.code.atlas.web.prompt.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContextQueryParserTest {

    private final ContextQueryParser parser = new ContextQueryParser();

    @Test
    void parseExtractsEndpointKeywordsAndFocusAreas() {
        String request = """
                Feature: soft delete users
                Endpoint: DELETE /api/profile
                Need:
                - schema changes with liquibase migrations
                - update queries where needed
                - auth/session implications
                """;

        ContextQuery query = parser.parse(request);

        assertEquals("DELETE", query.endpointMethod());
        assertEquals("/api/profile", query.endpointPath());
        assertTrue(query.keywords().contains("delete"));
        assertTrue(query.keywords().contains("/api/profile"));
        assertTrue(query.focusAreas().contains("migration"));
        assertTrue(query.focusAreas().contains("repository"));
        assertTrue(query.focusAreas().contains("auth"));
    }
}
