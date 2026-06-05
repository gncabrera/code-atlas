package com.code.atlas.web.service.context.indexed.indexer;

public record DatabaseRow(String tableName, String entity, String repository, String migration) {
}
