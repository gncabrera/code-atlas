package com.code.atlas.web.service.context.indexed.offline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileSummaryOfflineResponse(List<SummaryItem> summaries) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SummaryItem(String file, String summary) {
    }
}
