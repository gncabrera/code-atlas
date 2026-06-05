package com.code.atlas.web.service;

import java.util.HashSet;
import java.util.Set;

public final class ExceptionMessageFormatter {

    private ExceptionMessageFormatter() {
    }

    public static String formatChain(Throwable ex) {
        if (ex == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Set<Throwable> seen = new HashSet<>();
        Throwable current = ex;
        while (current != null && seen.add(current)) {
            appendThrowable(builder, current);
            current = current.getCause();
        }
        if (builder.isEmpty()) {
            return ex.getClass().getSimpleName();
        }
        return builder.toString();
    }

    private static void appendThrowable(StringBuilder builder, Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" — caused by: ");
        }
        builder.append(throwable.getClass().getSimpleName())
                .append(": ")
                .append(message.trim());
    }
}
