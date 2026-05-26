package com.code.atlas.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PromptOptimizerModePageController {

    @GetMapping("/admin/prompt-optimizer-modes")
    public String promptOptimizerModesPage() {
        return "admin/prompt-optimizer-modes";
    }
}
