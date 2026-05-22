package com.code.atlas.web.prompt;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PromptPageController {

    @GetMapping("/")
    public String rootPage() {
        return "redirect:/prompt-optimizer";
    }

    @GetMapping("/prompt-optimizer")
    public String promptOptimizerPage() {
        return "prompt-optimizer";
    }
}
