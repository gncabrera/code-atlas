package com.code.atlas.web.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class DevAssetModelAdvice {

    private final DevAssetVersionProvider devAssetVersionProvider;

    public DevAssetModelAdvice(DevAssetVersionProvider devAssetVersionProvider) {
        this.devAssetVersionProvider = devAssetVersionProvider;
    }

    @ModelAttribute("assetVersion")
    public long assetVersion() {
        return devAssetVersionProvider.getAssetVersion();
    }
}
