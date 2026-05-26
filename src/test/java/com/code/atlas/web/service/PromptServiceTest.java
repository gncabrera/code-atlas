package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.PromptOptimizerMode;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.dto.BuildPreviewRequestDto;
import com.code.atlas.web.service.dto.BuildPreviewResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

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

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setPath("/tmp/project");
        project.setUseDesignFile(true);

        mode = new PromptOptimizerMode();
        mode.setId(10L);
        mode.setCode("BALANCED");
        mode.setHidden(false);
        mode.setPrompt("Request: {{ USER_REQUEST }}\nDesign: {{ DESIGN_FILE }}");
    }

    @Test
    void buildPreview_injectsDesignFileWhenRequested() {
        when(promptOptimizerModeService.getModeEntity(10L)).thenReturn(mode);
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(promptContextService.buildContext(eq(project), any())).thenReturn("ctx");
        when(projectService.resolveDesignFileContent(project)).thenReturn("DESIGN.md\n\nui rules");
        when(promptFormatService.formatPrompt(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var parameters = (java.util.Map<String, String>) invocation.getArgument(1);
            return parameters.get("DESIGN_FILE");
        });

        BuildPreviewResponseDto response = promptService.buildPreview(
                new BuildPreviewRequestDto(1L, "add button", false, true, 10L)
        );

        assertTrue(response.aiModelPrompt().contains("ui rules"));
    }

    @Test
    void buildPreview_omitsDesignFileWhenNotRequested() {
        when(promptOptimizerModeService.getModeEntity(10L)).thenReturn(mode);
        when(projectService.getProjectEntity(1L)).thenReturn(project);
        when(promptContextService.buildContext(eq(project), any())).thenReturn("ctx");
        when(promptFormatService.formatPrompt(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var parameters = (java.util.Map<String, String>) invocation.getArgument(1);
            return parameters.getOrDefault("DESIGN_FILE", "missing");
        });

        BuildPreviewResponseDto response = promptService.buildPreview(
                new BuildPreviewRequestDto(1L, "add button", false, false, 10L)
        );

        assertTrue(response.aiModelPrompt().isEmpty());
    }
}
