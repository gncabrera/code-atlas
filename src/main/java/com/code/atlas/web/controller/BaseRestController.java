package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BaseRestController {
    @NotNull
    public ResponseEntity<ApiResponse<?>> handledException(String context, Exception ex) {
        GlobalExceptionHandler.logCaughtException(context, ex);
        return GlobalExceptionHandler.errorResponseEntity(
                GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
                HttpStatus.BAD_REQUEST);
    }
}
