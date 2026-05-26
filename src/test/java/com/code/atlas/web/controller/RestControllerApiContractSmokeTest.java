package com.code.atlas.web.controller;

import com.code.atlas.web.config.DevAssetModelAdvice;
import com.code.atlas.web.controller.support.ApiResponseContractSupport;
import com.code.atlas.web.service.AIModelApiKeyService;
import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.CommitHelperService;
import com.code.atlas.web.service.ProjectService;
import com.code.atlas.web.service.PromptHistoryService;
import com.code.atlas.web.service.PromptOptimizerModeService;
import com.code.atlas.web.service.PromptService;
import com.code.atlas.web.service.SkillService;
import com.code.atlas.web.service.dto.CommitHelperMetadataDto;
import com.code.atlas.web.service.dto.PromptPageMetadataDto;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestControllerApiContractSmokeTest {

    @Nested
    @WebMvcTest(
            controllers = ProjectController.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DevAssetModelAdvice.class
            )
    )
    class ProjectControllerSmoke {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ProjectService projectService;

        @Test
        void listEndpointReturnsApiResponseContract() throws Exception {
            when(projectService.getAllProjects()).thenReturn(List.of());

            mockMvc.perform(get("/api/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("success"))
                    .andExpect(jsonPath("$.message").value("Projects fetched."))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(ApiResponseContractSupport.strictSuccessContract());
        }
    }

    @Nested
    @WebMvcTest(
            controllers = SkillController.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DevAssetModelAdvice.class
            )
    )
    class SkillControllerSmoke {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SkillService skillService;

        @Test
        void listEndpointReturnsApiResponseContract() throws Exception {
            when(skillService.getAllSkills()).thenReturn(List.of());

            mockMvc.perform(get("/api/skills"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("success"))
                    .andExpect(jsonPath("$.message").value("Skills fetched."))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(ApiResponseContractSupport.strictSuccessContract());
        }
    }

    @Nested
    @WebMvcTest(
            controllers = AIModelController.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DevAssetModelAdvice.class
            )
    )
    class AIModelControllerSmoke {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AIModelService aiModelService;

        @Test
        void listEndpointReturnsApiResponseContract() throws Exception {
            when(aiModelService.getAllModels()).thenReturn(List.of());

            mockMvc.perform(get("/api/ai-models"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("success"))
                    .andExpect(jsonPath("$.message").value("AI models fetched."))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(ApiResponseContractSupport.strictSuccessContract());
        }
    }

    @Nested
    @WebMvcTest(
            controllers = AIModelApiKeyController.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DevAssetModelAdvice.class
            )
    )
    class AIModelApiKeyControllerSmoke {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AIModelApiKeyService aiModelApiKeyService;

        @Test
        void listEndpointReturnsApiResponseContract() throws Exception {
            when(aiModelApiKeyService.getAllKeys(false)).thenReturn(List.of());

            mockMvc.perform(get("/api/api-keys"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("success"))
                    .andExpect(jsonPath("$.message").value("API keys fetched."))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(ApiResponseContractSupport.strictSuccessContract());
        }
    }

    @Nested
    @WebMvcTest(
            controllers = PromptHistoryController.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DevAssetModelAdvice.class
            )
    )
    class PromptHistoryControllerSmoke {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PromptHistoryService promptHistoryService;

        @Test
        void listEndpointReturnsApiResponseContract() throws Exception {
            when(promptHistoryService.getAllHistory()).thenReturn(List.of());

            mockMvc.perform(get("/api/prompt-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("success"))
                    .andExpect(jsonPath("$.message").value("Prompt history fetched."))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(ApiResponseContractSupport.strictSuccessContract());
        }
    }

    @Nested
    @WebMvcTest(
            controllers = PromptController.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DevAssetModelAdvice.class
            )
    )
    class PromptControllerSmoke {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PromptService promptService;

        @MockBean
        private ProjectService projectService;

        @MockBean
        private AIModelService aiModelService;

        @MockBean
        private PromptOptimizerModeService promptOptimizerModeService;

        @Test
        void metadataEndpointReturnsApiResponseContract() throws Exception {
            when(projectService.getAllProjects()).thenReturn(List.of());
            when(aiModelService.getEnabledModels()).thenReturn(List.of());
            when(promptOptimizerModeService.getVisibleModes()).thenReturn(List.of());

            mockMvc.perform(get("/api/prompts/metadata"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("success"))
                    .andExpect(jsonPath("$.message").value("Prompt metadata fetched."))
                    .andExpect(jsonPath("$.data.projects").isArray())
                    .andExpect(jsonPath("$.data.enabledModels").isArray())
                    .andExpect(jsonPath("$.data.promptModes").isArray())
                    .andExpect(ApiResponseContractSupport.strictSuccessContract());
        }
    }

    @Nested
    @WebMvcTest(
            controllers = PromptOptimizerModeController.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DevAssetModelAdvice.class
            )
    )
    class PromptOptimizerModeControllerSmoke {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PromptOptimizerModeService promptOptimizerModeService;

        @Test
        void listEndpointReturnsApiResponseContract() throws Exception {
            when(promptOptimizerModeService.getAllModes()).thenReturn(List.of());

            mockMvc.perform(get("/api/admin/prompt-optimizer-modes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("success"))
                    .andExpect(jsonPath("$.message").value("Prompt optimizer modes fetched."))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(ApiResponseContractSupport.strictSuccessContract());
        }
    }

    @Nested
    @WebMvcTest(
            controllers = CommitHelperController.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DevAssetModelAdvice.class
            )
    )
    class CommitHelperControllerSmoke {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CommitHelperService commitHelperService;

        @Test
        void metadataEndpointReturnsApiResponseContract() throws Exception {
            when(commitHelperService.getMetadata(null))
                    .thenReturn(new CommitHelperMetadataDto(List.of(), List.of(), "main"));

            mockMvc.perform(get("/api/commit-helper/metadata"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("success"))
                    .andExpect(jsonPath("$.message").value("Commit helper metadata fetched."))
                    .andExpect(jsonPath("$.data.currentBranch").value("main"))
                    .andExpect(ApiResponseContractSupport.strictSuccessContract());
        }
    }
}
