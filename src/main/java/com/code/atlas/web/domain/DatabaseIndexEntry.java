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
import lombok.Data;

@Entity
@Table(name = "database_index")
@Data
public class DatabaseIndexEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(nullable = false)
    private String entity = "";

    @Column(nullable = false)
    private String repository = "";

    @Column(nullable = false)
    private String migration = "";

    @Column(name = "file_path", nullable = false)
    private String filePath;
}
