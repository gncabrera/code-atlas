package com.code.atlas.web.prompt.context;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.Project;
import java.util.List;

import com.code.atlas.web.service.PromptContextService;
import com.code.atlas.web.service.context.*;
import org.junit.jupiter.api.Test;

class PromptContextServiceTest {

    @Test
    void buildContextLimitsCandidatesByCharacterBudget() {
        ContextQueryParser parser = mock(ContextQueryParser.class);
        ContextRetriever retriever = mock(ContextRetriever.class);
        ContextFormatter formatter = new ContextFormatter();
        PromptContextService service = new PromptContextService(parser, retriever, formatter, 250);

        Project project = new Project();
        project.setId(1L);
        project.setPath("C:/repo");

        ContextQuery query = new ContextQuery(
                "Feature: soft delete users",
                "DELETE",
                "/api/profile",
                java.util.Set.of("delete", "profile"),
                java.util.List.of("controller")
        );
        when(parser.parse("Feature: soft delete users")).thenReturn(query);
        when(retriever.retrieve(project, query)).thenReturn(List.of(
                new ContextCandidate(
                        "src/main/java/A.java",
                        90,
                        List.of("reason"),
                        List.of("symbol"),
                        "line1\nline2\nline3",
                        "java"
                ),
                new ContextCandidate(
                        "src/main/java/B.java",
                        85,
                        List.of("long reason long reason long reason long reason"),
                        List.of("otherSymbol"),
                        "x".repeat(600),
                        "java"
                )
        ));

        String output = service.buildContext(project, "Feature: soft delete users");

        assertTrue(output.contains("### 1. src/main/java/A.java"));
        assertTrue(!output.contains("### 2. src/main/java/B.java"));
    }
}
