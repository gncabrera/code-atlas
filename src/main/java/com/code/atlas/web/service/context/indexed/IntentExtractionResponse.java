package com.code.atlas.web.service.context.indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntentExtractionResponse(
        String action,
        List<String> entities,
        List<String> operations,
        List<String> layers,
        boolean frontendImpact
) {
}
