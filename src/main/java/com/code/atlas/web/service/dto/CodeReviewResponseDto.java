package com.code.atlas.web.service.dto;

import java.util.List;

public record CodeReviewResponseDto(
        Summary summary,
        List<Finding> findings
) {
    public record Summary(
            int score,
            String risk,
            List<String> mainConcerns
    ) {
    }

    public record Finding(
            String severity,
            String category,
            String title,
            String file,
            Integer line,
            String description,
            String impact,
            String suggestion,
            String suggestedPatch
    ) {
    }
}
