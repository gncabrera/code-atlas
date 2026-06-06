package com.code.atlas.web.service.context.indexed.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MissingContextCategoryNormalizerTest {

    @Test
    void normalizesLongPhrasesToCanonicalCategories() {
        List<String> normalized = MissingContextCategoryNormalizer.normalize(List.of(
                "Gaps (Database Migrations)",
                "frontend caller",
                "repository layer"
        ));
        assertEquals(List.of("migration", "frontend", "repository"), normalized);
    }
}
