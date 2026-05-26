package com.code.atlas.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CodeReviewPageController {

    @GetMapping("/code-review")
    public String showCodeReviewPage() {
        return "code-review";
    }
}
