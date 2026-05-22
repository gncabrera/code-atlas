CREATE TABLE IF NOT EXISTS projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    path TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    use_agents_file INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS ai_models (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    tokens_per_minute INTEGER NOT NULL,
    requests_per_minute INTEGER NOT NULL,
    requests_per_day INTEGER NOT NULL,
    api_key TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS prompt_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER,
    ai_model_id INTEGER NOT NULL,
    mode TEXT NOT NULL,
    should_send_agents_file INTEGER NOT NULL,
    estimated_tokens INTEGER NOT NULL,
    request_prompt TEXT NOT NULL,
    response_prompt TEXT,
    status TEXT NOT NULL,
    error_message TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (ai_model_id) REFERENCES ai_models(id)
);
