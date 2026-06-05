package com.code.atlas.web.service.context.indexed.prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.KnowledgeResult;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndexedContextAssemblerTest {

    @Test
    void assembleIncludesArchitectureFactsAndSnippets() {
        IndexedContextAssembler assembler = new IndexedContextAssembler(5000);
        Intent intent = new Intent("modify", List.of("User"), List.of("soft delete"), List.of("service"), false);
        KnowledgeResult knowledge = new KnowledgeResult(
                "Current pattern:\n- Services own business logic",
                List.of(new RetrievedFile(
                        "UserService.java",
                        "java",
                        "service",
                        90,
                        List.of("match"),
                        List.of("UserService"),
                        "class UserService {}"
                )),
                List.of()
        );
        String output = assembler.assemble(intent, knowledge);
        assertTrue(output.contains("# Architecture Facts"));
        assertTrue(output.contains("Services own business logic"));
        assertTrue(output.contains("UserService.java"));
    }
}
