package com.code.atlas.web.project;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProjectPageController {

    @GetMapping("/projects")
    public String projectsPage() {
        return "projects";
    }
}
