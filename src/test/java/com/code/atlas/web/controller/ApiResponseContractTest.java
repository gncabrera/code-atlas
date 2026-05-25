package com.code.atlas.web.controller;

import com.code.atlas.web.api.GlobalExceptionHandler;
import com.code.atlas.web.config.DevAssetModelAdvice;
import com.code.atlas.web.controller.support.ApiResponseContractSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TestContractController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = DevAssetModelAdvice.class
        )
)
@Import(GlobalExceptionHandler.class)
class ApiResponseContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnStrictSuccessContract() throws Exception {
        mockMvc.perform(get("/api/test/contract-success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("success"))
                .andExpect(jsonPath("$.message").value("Operation successful"))
                .andExpect(jsonPath("$.data.testKey").value("testValue"))
                .andExpect(ApiResponseContractSupport.strictSuccessContract());
    }

    @Test
    void shouldReturnStrictErrorContractOnIllegalArgument() throws Exception {
        mockMvc.perform(get("/api/test/contract-error").param("type", "illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.message").value("Invalid contract request"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(ApiResponseContractSupport.strictErrorContract());
    }

    @Test
    void shouldReturnStrictErrorContractOnNotFound() throws Exception {
        mockMvc.perform(get("/api/test/contract-error").param("type", "notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.message").value("Element not found"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(ApiResponseContractSupport.strictErrorContract());
    }

    @Test
    void shouldReturnStrictErrorContractOnGenericException() throws Exception {
        mockMvc.perform(get("/api/test/contract-error").param("type", "runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.message").value("Fallback failure"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(ApiResponseContractSupport.strictErrorContract());
    }

    @Test
    void shouldReturnStrictErrorContractOnValidationFailure() throws Exception {
        mockMvc.perform(post("/api/test/contract-validation")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("error"))
                .andExpect(jsonPath("$.message").value("name: must not be blank"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(ApiResponseContractSupport.strictErrorContract());
    }
}
