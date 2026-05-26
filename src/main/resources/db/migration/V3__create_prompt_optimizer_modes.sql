CREATE TABLE IF NOT EXISTS prompt_optimizer_mode (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    prompt TEXT NOT NULL,
    hidden INTEGER NOT NULL DEFAULT 0,
    read_only INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_prompt_optimizer_mode_code
    ON prompt_optimizer_mode(code);
