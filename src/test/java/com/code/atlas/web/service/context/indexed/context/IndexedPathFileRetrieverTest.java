package com.code.atlas.web.service.context.indexed.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IndexedPathFileRetrieverTest {

    @Test
    void detectsMigrationAndFrontendPaths() {
        assertTrue(IndexedPathFileRetriever.isMigrationPath("src/main/resources/db/migration/V1__init.sql"));
        assertFalse(IndexedPathFileRetriever.isMigrationPath("src/main/resources/schema.sql"));
        assertTrue(IndexedPathFileRetriever.isFrontendPath("src/main/resources/templates/projects.html"));
        assertTrue(IndexedPathFileRetriever.isFrontendPath("src/main/resources/static/js/projects.js"));
        assertFalse(IndexedPathFileRetriever.isFrontendPath("src/main/resources/static/js/vendor/jquery.js"));
    }
}
