package com.code.atlas.web.controller.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.springframework.test.web.servlet.ResultMatcher;

public final class ApiResponseContractSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> ALLOWED_ROOT_KEYS = Set.of("result", "message", "data");

    private ApiResponseContractSupport() {
    }

    public static ResultMatcher strictSuccessContract() {
        return result -> {
            JsonNode root = parseRoot(result.getResponse().getContentAsString());
            assertAllowedRootKeys(root);
            if (!root.path("result").asText().equals("success")) {
                throw new AssertionError("Expected result=success but was: " + root.path("result").asText());
            }
            if (!root.hasNonNull("message") || root.get("message").asText().isBlank()) {
                throw new AssertionError("Expected non-blank message on success response");
            }
        };
    }

    public static ResultMatcher strictErrorContract() {
        return result -> {
            JsonNode root = parseRoot(result.getResponse().getContentAsString());
            assertAllowedRootKeys(root);
            if (!root.path("result").asText().equals("error")) {
                throw new AssertionError("Expected result=error but was: " + root.path("result").asText());
            }
            if (!root.hasNonNull("message") || root.get("message").asText().isBlank()) {
                throw new AssertionError("Expected non-blank message on error response");
            }
            if (root.has("data") && !root.get("data").isNull()) {
                throw new AssertionError("Expected data to be absent or null on error response");
            }
        };
    }

    private static JsonNode parseRoot(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new AssertionError("Response body is not valid JSON", ex);
        }
    }

    private static void assertAllowedRootKeys(JsonNode root) {
        Set<String> keys = new HashSet<>();
        root.fieldNames().forEachRemaining(keys::add);
        if (!ALLOWED_ROOT_KEYS.containsAll(keys)) {
            throw new AssertionError("Unexpected root-level JSON keys: " + keys);
        }
        if (!keys.contains("result") || !keys.contains("message")) {
            throw new AssertionError("Missing required root keys result/message: " + keys);
        }
    }
}
