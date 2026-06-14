package com.code.atlas.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PlanModePageController {

    @GetMapping("/plan-mode")
    public String planModePage() {
        return "plan-mode";
    }
}
