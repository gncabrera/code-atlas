CREATE TABLE IF NOT EXISTS projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    path TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    use_agents_file INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS ai_model_api_key (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_models (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    enabled INTEGER NOT NULL DEFAULT 1,
    tokens_per_minute INTEGER NOT NULL,
    requests_per_minute INTEGER NOT NULL,
    requests_per_day INTEGER NOT NULL,
    api_key_id INTEGER,
    FOREIGN KEY (api_key_id) REFERENCES ai_model_api_key(id)
);

CREATE TABLE IF NOT EXISTS prompt_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER,
    ai_model_id INTEGER NOT NULL,
    notes TEXT,
    estimated_tokens INTEGER NOT NULL,
    request_prompt TEXT NOT NULL,
    response_prompt TEXT,
    status TEXT NOT NULL,
    error_message TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (ai_model_id) REFERENCES ai_models(id)
);

CREATE TABLE IF NOT EXISTS skill (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    prompt TEXT NOT NULL,
    target_path TEXT NOT NULL,
    description TEXT,
    category TEXT,
    default_in_output_prompt INTEGER NOT NULL DEFAULT 0
);

-- SQLite datetime convention: store as TEXT (DEFAULT CURRENT_TIMESTAMP).
-- JPA entities use java.time.LocalDateTime only (never Instant).
-- DB-managed: insertable=false, updatable=false on the entity column.
-- App-managed: service sets LocalDateTime.now() on insert/update.
CREATE TABLE IF NOT EXISTS project_file_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    file_extension TEXT NOT NULL,
    last_modified_epoch INTEGER NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    token_count INTEGER NOT NULL,
    symbols TEXT NOT NULL,
    endpoint_hints TEXT NOT NULL,
    searchable_text TEXT NOT NULL,
    -- App-managed timestamp; JPA writes LocalDateTime (see AGENTS.md SQLite timestamps).
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_project_file_index_project_path
    ON project_file_index(project_id, file_path);

CREATE INDEX IF NOT EXISTS idx_project_file_index_project_updated
    ON project_file_index(project_id, updated_at);

INSERT INTO ai_model_api_key (name, api_key, provider, is_active)
VALUES ('Default Gemini', 'changeme', 'Gemini', 1);

INSERT INTO ai_models (name, description, enabled, tokens_per_minute, requests_per_minute, requests_per_day, api_key_id)
VALUES
    ('gemini-3.5-flash', 'Gemini 3.5 Flash (gemini-3.5-flash)', 1, 250000, 5, 20, 1),
    ('gemini-2.5-flash', 'Gemini 2.5 Flash (gemini-2.5-flash)', 1, 250000, 5, 20, 1),
    ('gemini-3-flash-preview', 'Gemini 3 Flash (gemini-3-flash-preview)', 1, 250000, 5, 20, 1),
    ('gemini-2.5-flash-lite', 'Gemini 2.5 Flash Lite (gemini-2.5-flash-lite)', 1, 250000, 5, 20, 1),
    ('gemini-3.1-flash-lite', 'Gemini 3.1 Flash Lite (gemini-3.1-flash-lite)', 1, 250000, 15, 500, 1),
    ('gemma-4-26b-a4b-it', 'Gemma 4 26B (gemma-4-26b-a4b-it)', 1, 0, 15, 1500, 1),
    ('gemma-4-31b-it', 'Gemma 4 31B (gemma-4-31b-it)', 1, 0, 15, 1500, 1);
