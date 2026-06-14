CREATE TABLE plan_session (
    id                INTEGER  PRIMARY KEY AUTOINCREMENT,
    title             TEXT     NOT NULL,
    user_request      TEXT     NOT NULL,
    output_type       TEXT     NOT NULL,
    project_id        INTEGER,
    context_model_id  INTEGER  NOT NULL,
    plan_model_id     INTEGER  NOT NULL,
    context_data      TEXT,
    status            TEXT     NOT NULL DEFAULT 'DRAFT',
    created_at        TEXT     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TEXT     NOT NULL,
    FOREIGN KEY (context_model_id) REFERENCES ai_models(id),
    FOREIGN KEY (plan_model_id)    REFERENCES ai_models(id),
    FOREIGN KEY (project_id)       REFERENCES projects(id) ON DELETE SET NULL
);

CREATE TABLE plan_discovery (
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id               INTEGER NOT NULL UNIQUE,
    questions_json           TEXT    NOT NULL,
    suggestions_json         TEXT    NOT NULL,
    answers_json             TEXT,
    selected_suggestions_json TEXT,
    created_at               TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES plan_session(id) ON DELETE CASCADE
);

CREATE TABLE plan_conversation_message (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id  INTEGER NOT NULL,
    thread_type TEXT    NOT NULL,
    role        TEXT    NOT NULL,
    content     TEXT    NOT NULL,
    created_at  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES plan_session(id) ON DELETE CASCADE
);

CREATE TABLE plan_result (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id     INTEGER NOT NULL UNIQUE,
    generated_plan TEXT    NOT NULL,
    created_at     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES plan_session(id) ON DELETE CASCADE
);
