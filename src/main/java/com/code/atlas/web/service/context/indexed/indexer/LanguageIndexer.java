package com.code.atlas.web.service.context.indexed.indexer;

import com.code.atlas.web.domain.IndexerProfile;

public interface LanguageIndexer {

    IndexerProfile profile();

    boolean supports(String extension);

    IndexerOutput index(IndexFileInput input);
}
