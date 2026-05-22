package com.code.atlas.web.aimodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "ai_models")
@Data
public class AIModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    @Column(nullable = false)
    private String description = "";

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "tokens_per_minute", nullable = false)
    private int tokensPerMinute;

    @Column(name = "requests_per_minute", nullable = false)
    private int requestsPerMinute;

    @Column(name = "requests_per_day", nullable = false)
    private int requestsPerDay;

    @NotBlank
    @Column(name = "api_key", nullable = false)
    private String apiKey;
}
