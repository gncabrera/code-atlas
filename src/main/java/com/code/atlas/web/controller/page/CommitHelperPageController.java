package com.code.atlas.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommitHelperPageController {

    @GetMapping("/commit-helper")
    public String commitHelperPage() {
        return "commit-helper";
    }
}
