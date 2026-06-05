package com.code.atlas.web.service.context.indexed;

import java.util.List;

public record MissingContext(List<String> missing) {
    public MissingContext {
        missing = missing == null ? List.of() : List.copyOf(missing);
    }
}
