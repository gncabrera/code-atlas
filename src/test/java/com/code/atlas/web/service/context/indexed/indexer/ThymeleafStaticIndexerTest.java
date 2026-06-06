package com.code.atlas.web.service.context.indexed.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThymeleafStaticIndexerTest {

    private final ThymeleafStaticIndexer indexer = new ThymeleafStaticIndexer();

    @Test
    void indexesTemplateAndInfersJsPath() {
        IndexerOutput output = indexer.index(new IndexFileInput(
                "src/main/resources/templates/prompt-optimizer.html",
                "html",
                "<html></html>"
        ));
        assertFalse(output.frontendRows().isEmpty());
        FrontendRow row = output.frontendRows().getFirst();
        assertEquals("prompt-optimizer", row.component());
        assertEquals("src/main/resources/static/js/prompt-optimizer.js", row.service());
    }

    @Test
    void indexesStaticJsOutsideVendor() {
        IndexerOutput output = indexer.index(new IndexFileInput(
                "src/main/resources/static/js/projects.js",
                "js",
                "console.log('ok');"
        ));
        assertEquals("projects", output.frontendRows().getFirst().component());
    }

    @Test
    void skipsVendorJs() {
        IndexerOutput output = indexer.index(new IndexFileInput(
                "src/main/resources/static/js/vendor/jquery.js",
                "js",
                "console.log('ok');"
        ));
        assertTrue(output.frontendRows().isEmpty());
    }
}
