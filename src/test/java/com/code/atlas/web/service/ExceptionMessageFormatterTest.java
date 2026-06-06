package com.code.atlas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExceptionMessageFormatterTest {

    @Test
    void formatChain_includesCauseMessages() {
        Exception root = new IllegalStateException(
                "Failed to execute HTTP request.",
                new RuntimeException("Connection timed out after 60000 ms")
        );

        String formatted = ExceptionMessageFormatter.formatChain(root);

        assertTrue(formatted.contains("IllegalStateException: Failed to execute HTTP request."));
        assertTrue(formatted.contains("caused by: RuntimeException: Connection timed out after 60000 ms"));
    }

    @Test
    void formatChain_includesNestedCauseClassWhenOuterHasDefaultMessage() {
        Exception root = new IllegalStateException(new RuntimeException("inner"));

        String formatted = ExceptionMessageFormatter.formatChain(root);

        assertTrue(formatted.contains("RuntimeException: inner"));
    }
}
