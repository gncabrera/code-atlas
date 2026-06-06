CREATE TABLE IF NOT EXISTS symbol_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    symbol TEXT NOT NULL,
    kind TEXT NOT NULL,
    file_path TEXT NOT NULL,
    line INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_symbol_index_project_file_symbol_line
    ON symbol_index(project_id, file_path, symbol, line);

CREATE INDEX IF NOT EXISTS idx_symbol_index_project_symbol
    ON symbol_index(project_id, symbol);

CREATE TABLE IF NOT EXISTS endpoint_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    http_method TEXT NOT NULL,
    path TEXT NOT NULL,
    controller TEXT NOT NULL DEFAULT '',
    service TEXT NOT NULL DEFAULT '',
    file_path TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_endpoint_index_project_method_path
    ON endpoint_index(project_id, http_method, path);

CREATE INDEX IF NOT EXISTS idx_endpoint_index_project_path
    ON endpoint_index(project_id, path);

CREATE TABLE IF NOT EXISTS graph_edge (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    source TEXT NOT NULL,
    target TEXT NOT NULL,
    relation TEXT NOT NULL,
    source_file_path TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_graph_edge_project_source_target_relation
    ON graph_edge(project_id, source, target, relation);

CREATE INDEX IF NOT EXISTS idx_graph_edge_project_source
    ON graph_edge(project_id, source);

CREATE INDEX IF NOT EXISTS idx_graph_edge_project_target
    ON graph_edge(project_id, target);

CREATE TABLE IF NOT EXISTS database_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    table_name TEXT NOT NULL,
    entity TEXT NOT NULL DEFAULT '',
    repository TEXT NOT NULL DEFAULT '',
    migration TEXT NOT NULL DEFAULT '',
    file_path TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_database_index_project_table
    ON database_index(project_id, table_name);

CREATE TABLE IF NOT EXISTS frontend_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    component TEXT NOT NULL,
    service TEXT NOT NULL DEFAULT '',
    endpoint TEXT NOT NULL DEFAULT '',
    file_path TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_frontend_index_project_component_endpoint
    ON frontend_index(project_id, component, endpoint);

CREATE TABLE IF NOT EXISTS business_concept_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    concept TEXT NOT NULL,
    files TEXT NOT NULL DEFAULT '',
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_business_concept_index_project_concept
    ON business_concept_index(project_id, concept);

CREATE TABLE IF NOT EXISTS pattern_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    pattern TEXT NOT NULL,
    files TEXT NOT NULL DEFAULT '',
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pattern_index_project_pattern
    ON pattern_index(project_id, pattern);

CREATE TABLE IF NOT EXISTS file_summary_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_file_summary_index_project_file
    ON file_summary_index(project_id, file_path);
