package com.code.atlas.web.prompt.context;

import com.code.atlas.web.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

@Entity
@Table(name = "project_file_index")
@Data
public class ProjectFileIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_extension", nullable = false)
    private String fileExtension;

    @Column(name = "last_modified_epoch", nullable = false)
    private long lastModifiedEpoch;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(name = "symbols", nullable = false, columnDefinition = "TEXT")
    private String symbols;

    @Column(name = "endpoint_hints", nullable = false, columnDefinition = "TEXT")
    private String endpointHints;

    @Column(name = "searchable_text", nullable = false, columnDefinition = "TEXT")
    private String searchableText;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
