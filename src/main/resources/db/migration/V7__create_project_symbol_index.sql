CREATE TABLE IF NOT EXISTS project_symbol_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    symbol_name TEXT NOT NULL,
    symbol_kind TEXT NOT NULL,
    snippet TEXT NOT NULL,
    embedding BLOB,
    embedding_model TEXT,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_project_symbol_index_project_path_symbol
    ON project_symbol_index(project_id, file_path, symbol_name);

CREATE INDEX IF NOT EXISTS idx_project_symbol_index_project_updated
    ON project_symbol_index(project_id, updated_at);
