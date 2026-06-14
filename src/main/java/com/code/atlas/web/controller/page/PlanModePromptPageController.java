package com.code.atlas.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PlanModePromptPageController {

    @GetMapping("/admin/plan-mode-prompts")
    public String planModePromptsPage() {
        return "admin/plan-mode-prompts";
    }
}
