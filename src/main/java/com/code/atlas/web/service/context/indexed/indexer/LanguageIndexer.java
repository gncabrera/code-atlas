package com.code.atlas.web.service.context.indexed.indexer;

public interface LanguageIndexer {

    boolean supports(String extension);

    IndexerOutput index(IndexFileInput input);
}
