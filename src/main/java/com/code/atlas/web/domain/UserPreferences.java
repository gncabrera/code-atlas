package com.code.atlas.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "user_preferences")
@Data
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt_optimizer_default_ai_model_id", nullable = false)
    private int promptOptimizerDefaultAiModelId = 0;

    @Column(name = "prompt_optimizer_default_prompt_mode_id", nullable = false)
    private int promptOptimizerDefaultPromptModeId = 0;

    @Column(name = "commit_helper_default_ai_model_id", nullable = false)
    private int commitHelperDefaultAiModelId = 0;

    @Column(name = "code_review_default_ai_model_id", nullable = false)
    private int codeReviewDefaultAiModelId = 0;
}
