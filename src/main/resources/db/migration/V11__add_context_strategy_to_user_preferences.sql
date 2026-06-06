ALTER TABLE user_preferences ADD COLUMN prompt_optimizer_default_context_strategy TEXT NOT NULL DEFAULT 'deterministic';
ALTER TABLE user_preferences ADD COLUMN prompt_optimizer_default_context_ai_model_id INTEGER NOT NULL DEFAULT 0;
