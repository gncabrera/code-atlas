package com.code.atlas.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PromptHistoryPageController {

    @GetMapping("/admin/prompt-history")
    public String promptHistoryPage() {
        return "prompt-history";
    }
}
