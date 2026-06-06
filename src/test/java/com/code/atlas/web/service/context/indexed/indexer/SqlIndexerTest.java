package com.code.atlas.web.service.context.indexed.indexer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SqlIndexerTest {

    private final SqlIndexer indexer = new SqlIndexer();

    @Test
    void supportsSqlExtension() {
        assertTrue(indexer.supports("sql"));
        assertFalse(indexer.supports("java"));
    }

    @Test
    void indexesMigrationSqlWithTableNames() {
        String sql = """
                CREATE TABLE IF NOT EXISTS ai_model (
                    id INTEGER PRIMARY KEY
                );
                """;
        IndexerOutput output = indexer.index(new IndexFileInput(
                "src/main/resources/db/migration/V10__ai_model.sql",
                "sql",
                sql
        ));
        assertTrue(output.databaseRows().stream().anyMatch(row -> "ai_model".equals(row.tableName())));
        assertTrue(output.databaseRows().stream().anyMatch(row -> row.migration().contains("V10__ai_model.sql")));
    }
}
