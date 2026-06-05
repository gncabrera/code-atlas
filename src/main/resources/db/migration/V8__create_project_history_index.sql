CREATE TABLE IF NOT EXISTS project_history_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    commit_hash TEXT NOT NULL,
    subject TEXT NOT NULL,
    summary TEXT NOT NULL,
    embedding BLOB,
    embedding_model TEXT,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_project_history_index_project_hash
    ON project_history_index(project_id, commit_hash);

CREATE INDEX IF NOT EXISTS idx_project_history_index_project_updated
    ON project_history_index(project_id, updated_at);
