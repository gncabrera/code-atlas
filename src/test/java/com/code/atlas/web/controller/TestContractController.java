package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestContractController {

    @GetMapping("/api/test/contract-success")
    ResponseEntity<ApiResponse<Map<String, String>>> testSuccess() {
        return ResponseEntity.ok(ApiResponse.success("Operation successful", Map.of("testKey", "testValue")));
    }

    @GetMapping("/api/test/contract-error")
    ResponseEntity<ApiResponse<?>> testError(@RequestParam String type) {
        if ("illegal".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Invalid contract request");
        }
        if ("notfound".equalsIgnoreCase(type)) {
            throw new NoSuchElementException("Element not found");
        }
        throw new RuntimeException("Fallback failure");
    }

    @PostMapping("/api/test/contract-validation")
    ResponseEntity<ApiResponse<?>> testValidation(@Valid @RequestBody TestContractValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Validation passed", request.name()));
    }
}
