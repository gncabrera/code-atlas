package com.code.atlas.web.config;

import org.springframework.stereotype.Component;

@Component
public class DevAssetVersionProvider {

    private final long assetVersion = System.currentTimeMillis();

    public long getAssetVersion() {
        return assetVersion;
    }
}
