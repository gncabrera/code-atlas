Detect missing context categories needed to implement the user request.

User request:
{{USER_REQUEST}}

Intent:
{{INTENT}}

Retrieved files:
{{RETRIEVED_FILES}}

Pattern hints:
{{PATTERN_HINTS}}

Return exactly one JSON object. No markdown fences or prose.

Schema:
{
  "missing": ["migration","frontend caller","repository","entity","test"]
}

Rules:
- use short canonical category labels only: migration, frontend, repository, entity, dto, test, config
- missing is empty when retrieved files appear sufficient
- common categories: migration, frontend, repository, entity, dto, test, config

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}