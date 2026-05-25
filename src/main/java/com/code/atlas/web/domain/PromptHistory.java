package com.code.atlas.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "prompt_history")
@Data
public class PromptHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_model_id", nullable = false)
    private AIModel aiModel;

    @Column(nullable = false)
    private String mode;

    @Column(name = "should_send_agents_file", nullable = false)
    private boolean shouldSendAgentsFile;

    @Column(name = "estimated_tokens", nullable = false)
    private int estimatedTokens;

    @Column(name = "request_prompt", nullable = false, columnDefinition = "TEXT")
    private String requestPrompt;

    @Column(name = "response_prompt", columnDefinition = "TEXT")
    private String responsePrompt;

    @Column(nullable = false)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;
}
