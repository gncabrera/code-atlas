package com.code.atlas.web.api;

import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static ResponseEntity<ApiResponse<?>> errorResponseEntity(String message, HttpStatus status) {
        ApiResponse<?> response = ApiResponse.error(message);
        return new ResponseEntity<>(response, status);
    }

    public static void logCaughtException(String context, Exception ex) {
        if (isExpectedException(ex)) {
            log.warn("{}: {}", context, ex.getMessage());
            return;
        }
        log.error("{}: {}", context, ex.getMessage(), ex);
    }

    public static String resolveMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return errorResponseEntity(resolveMessage(ex, "Invalid request."), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<?>> handleNoSuchElementException(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return errorResponseEntity(resolveMessage(ex, "Resource not found."), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed.");
        log.warn("Validation failed: {}", message);
        return errorResponseEntity(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return errorResponseEntity(resolveMessage(ex, "Unexpected server error."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static boolean isExpectedException(Throwable ex) {
        return ex instanceof IllegalArgumentException || ex instanceof NoSuchElementException;
    }
}
