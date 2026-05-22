package com.code.atlas.web.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "skill")
@Data
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @NotBlank
    @Column(name = "target_path", nullable = false)
    private String targetPath;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String category;

    @Column(name = "default_in_output_prompt", nullable = false)
    private boolean defaultInOutputPrompt = false;
}
