Summarize architecture facts for the implementation model.

User request:
{{USER_REQUEST}}

Intent:
{{INTENT}}

File summaries:
{{FILE_SUMMARIES}}

Retrieved file paths:
{{RETRIEVED_FILES}}

Produce plain text (not JSON) starting with "Current pattern:" followed by bullet facts.
Rules:
- State explicit facts only; do not invent files
- Mention layering, persistence, and gaps relevant to the request
- Keep under 20 bullet lines

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}