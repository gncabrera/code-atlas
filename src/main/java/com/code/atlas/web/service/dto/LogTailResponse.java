package com.code.atlas.web.service.dto;

import java.util.List;

public record LogTailResponse(
        List<String> lines,
        boolean running,
        double progressPercent
) {
    public LogTailResponse {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
