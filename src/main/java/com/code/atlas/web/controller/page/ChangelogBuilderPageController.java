package com.code.atlas.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChangelogBuilderPageController {

    @GetMapping("/changelog-builder")
    public String index(Model model) {
        model.addAttribute("activePage", "changelog-builder");
        return "changelog-builder";
    }
}
