package com.code.atlas.web.aimodel;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AIModelPageController {

    @GetMapping("/ai-models")
    public String aiModelsPage() {
        return "ai-models";
    }
}
