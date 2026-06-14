package com.code.atlas.web.domain;

import com.code.atlas.web.config.SqliteLocalDateTimeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "plan_session")
@Data
public class PlanSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(name = "user_request", nullable = false, columnDefinition = "TEXT")
    private String userRequest;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "output_type", nullable = false)
    private PlanOutputType outputType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_model_id", nullable = false)
    private AIModel contextModel;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_model_id", nullable = false)
    private AIModel planModel;

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanSessionStatus status = PlanSessionStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Convert(converter = SqliteLocalDateTimeConverter.class)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
