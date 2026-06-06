package com.code.atlas.web.service.context.indexed.offline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PatternOfflineResponse(List<PatternItem> patterns) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PatternItem(String pattern, List<String> files) {
    }
}
