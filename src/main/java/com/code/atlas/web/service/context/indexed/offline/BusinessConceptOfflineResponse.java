package com.code.atlas.web.service.context.indexed.offline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessConceptOfflineResponse(List<BusinessConceptItem> concepts) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BusinessConceptItem(String concept, List<String> files) {
    }
}
