package com.code.atlas.web.service.context.indexed.indexer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code.atlas.web.service.context.deterministic.ContextSymbolExtractor;
import org.junit.jupiter.api.Test;

class JavaIndexerTest {

    private final JavaIndexer indexer = new JavaIndexer(new ContextSymbolExtractor());

    @Test
    void supportsJavaExtension() {
        assertTrue(indexer.supports("java"));
        assertFalse(indexer.supports("ts"));
    }

    @Test
    void indexesControllerSymbolsAndEndpoints() {
        String source = """
                @RestController
                @RequestMapping("/api/users")
                public class UserController {
                    private final UserService userService;
                    @GetMapping("/{id}")
                    public User getUser() { return null; }
                }
                """;
        IndexerOutput output = indexer.index(new IndexFileInput("UserController.java", "java", source));
        assertFalse(output.symbols().isEmpty());
        assertTrue(output.endpoints().stream().anyMatch(row -> "GET".equals(row.httpMethod())));
        assertTrue(output.graphEdges().stream().anyMatch(edge -> "UserService".equals(edge.target())));
    }

    @Test
    void deduplicatesRepeatedUsesEdgesWithinFile() {
        String source = """
                public class UserController {
                    private final UserService userService;
                    private final UserService backupUserService;
                }
                """;
        IndexerOutput output = indexer.index(new IndexFileInput("UserController.java", "java", source));
        long usesCount = output.graphEdges().stream()
                .filter(edge -> "USES".equals(edge.relation()) && "UserService".equals(edge.target()))
                .count();
        assertTrue(usesCount <= 1);
    }
}
