package com.code.atlas.web.domain;

import com.code.atlas.web.config.SqliteLocalDateTimeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "project_file_metadata_index")
public class ProjectFileMetadataIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "project_file_index_id",
            nullable = false,
            unique = true
    )
    private ProjectFileIndex file;

    @Lob
    @Column(name = "metadata_json", nullable = false)
    private String metadataJson;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Convert(converter = SqliteLocalDateTimeConverter.class)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
