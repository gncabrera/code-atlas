CREATE TABLE plan_mode_prompt (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    code    TEXT    NOT NULL UNIQUE,
    name    TEXT    NOT NULL,
    prompt  TEXT    NOT NULL
);
