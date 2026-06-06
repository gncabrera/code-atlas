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
- missing is empty when retrieved files appear sufficient
- use short category labels, not file paths
- common categories: migration, frontend caller, repository, entity, dto, test, config

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}