package com.code.atlas.web.controller.page;

import com.code.atlas.web.service.ProjectService;
import com.code.atlas.web.service.dto.ProjectResponseDto;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SkillPageController {

    private final ProjectService projectService;

    public SkillPageController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/skills")
    public String skillsPage(Model model) {
        List<ProjectResponseDto> projects = projectService.getAllProjects();
        model.addAttribute("projects", projects);
        return "skills";
    }
}
