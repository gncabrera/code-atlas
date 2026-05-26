CREATE TABLE IF NOT EXISTS user_preferences (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    prompt_optimizer_default_ai_model_id INTEGER NOT NULL DEFAULT 0,
    prompt_optimizer_default_prompt_mode_id INTEGER NOT NULL DEFAULT 0,
    commit_helper_default_ai_model_id INTEGER NOT NULL DEFAULT 0,
    code_review_default_ai_model_id INTEGER NOT NULL DEFAULT 0
);

INSERT INTO user_preferences (
    id,
    prompt_optimizer_default_ai_model_id,
    prompt_optimizer_default_prompt_mode_id,
    commit_helper_default_ai_model_id,
    code_review_default_ai_model_id
)
VALUES (1, 0, 0, 0, 0);
