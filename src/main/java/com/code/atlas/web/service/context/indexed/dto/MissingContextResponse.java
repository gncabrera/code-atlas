package com.code.atlas.web.service.context.indexed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MissingContextResponse(List<String> missing) {
}
