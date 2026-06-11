package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.PromptOptimizerMode;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ContextStrategy;
import com.code.atlas.web.service.dto.BuildPreviewRequestDto;
import com.code.atlas.web.service.dto.BuildPreviewResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptServiceIndexedContextTest {

    @Mock
    private PromptOptimizerModeService promptOptimizerModeService;

    @Mock
    private ProjectService projectService;

    @Mock
    private PromptContextService promptContextService;

    @Mock
    private AIModelService aiModelService;

    @Mock
    private PromptFormatService promptFormatService;

    @InjectMocks
    private PromptService promptService;

    private Project project;
    private PromptOptimizerMode mode;
    private AIModel aiModel;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        mode = new PromptOptimizerMode();
        mode.setId(10L);
        mode.setHidden(false);
        mode.setPrompt("Context:\n{{CONTEXT}}");
        aiModel = new AIModel();
        aiModel.setId(5L);
        aiModel.setEnabled(true);
    }

    @Test
    void buildPreviewUsesIndexedContextWhenStrategyIndexed() {
        when(promptOptimizerModeService.getModeEntity(10L)).thenReturn(mode);
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(aiModelService.getModelEntity(5L)).thenReturn(aiModel);
        when(promptContextService.buildIndexedContext(eq(project), any(), eq(aiModel))).thenReturn("indexed-context");
        when(promptFormatService.formatPrompt(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var parameters = (java.util.Map<String, String>) invocation.getArgument(1);
            return parameters.get("CONTEXT");
        });

        BuildPreviewResponseDto response = promptService.buildPreview(
                new BuildPreviewRequestDto(1L, "add soft delete", false, false, 10L, 7L, ContextStrategy.INDEXED, 5L)
        );

        assertTrue(response.aiModelPrompt().contains("indexed-context"));
    }
}
