package com.code.atlas.web.service.context.indexed.engine.context.builder;

import org.springframework.stereotype.Service;

@Service
public class MetadataFilter {

    // TODO: implement it!
    public boolean shouldGenerateMetadata() {
        /* return extensionPermitida
                && !excludedDirectory(path)
                && !gitIgnored(path)*/
        return false;
    }
}
