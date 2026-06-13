package com.code.atlas.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProjectTypePageController {

    @GetMapping("/admin/project-types")
    public String projectTypesPage() {
        return "admin/project-types";
    }
}
