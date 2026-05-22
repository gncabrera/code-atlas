package com.code.atlas.web.aimodel;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AIModelApiKeyPageController {

    @GetMapping("/api-keys")
    public String apiKeysPage() {
        return "api-keys";
    }
}
