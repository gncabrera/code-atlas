package com.code.atlas.web.service.context.indexed.knowledge;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.repository.FileSummaryIndexRepository;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.PromptFormatService;
import com.code.atlas.web.service.context.indexed.Intent;
import com.code.atlas.web.service.context.indexed.RetrievedFile;
import com.code.atlas.web.service.dto.ModelResponseDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArchitectureSummarizerTest {

    @Mock
    private AIModelService aiModelService;

    @Mock
    private FileSummaryIndexRepository fileSummaryIndexRepository;

    private ArchitectureSummarizer summarizer;
    private Project project;
    private AIModel model;

    @BeforeEach
    void setUp() {
        summarizer = new ArchitectureSummarizer(
                aiModelService,
                new PromptFormatService(),
                fileSummaryIndexRepository
        );
        project = new Project();
        project.setId(1L);
        model = new AIModel();
        model.setId(2L);
        model.setEnabled(true);
        model.setTokensPerMinute(10_000);
    }

    @Test
    void summarize_includesRetrievalSignalsAndSnippetsInPrompt() {
        RetrievedFile file = new RetrievedFile(
                "src/UserService.java",
                "java",
                "service",
                90,
                List.of("entity match"),
                List.of("UserService", "save"),
                "class UserService { void save() {} }"
        );
        when(fileSummaryIndexRepository.findByProjectIdAndFilePath(1L, file.relativePath()))
                .thenReturn(Optional.empty());
        when(aiModelService.sendToModel(eq(project), eq(model), any(), eq("Indexed context: architecture summary"), any()))
                .thenReturn(new ModelResponseDto("Current pattern:\n- ok", 10));

        summarizer.summarize(
                project,
                "Add soft delete",
                new Intent("modify", List.of("User"), List.of("soft delete"), List.of("service"), false),
                List.of(file),
                model
        );

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelService).sendToModel(
                eq(project),
                eq(model),
                promptCaptor.capture(),
                eq("Indexed context: architecture summary"),
                eq("Indexed context: architecture summary")
        );
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("src/UserService.java (score=90)"));
        assertTrue(prompt.contains("- entity match"));
        assertTrue(prompt.contains("symbols: UserService, save"));
        assertTrue(prompt.contains("```java"));
        assertTrue(prompt.contains("class UserService { void save() {} }"));
        assertTrue(prompt.contains("(no summary)"));
    }
}
