Summarize architecture facts for the implementation model.

User request:
{{USER_REQUEST}}

Intent:
{{INTENT}}

File summaries (offline index):
{{FILE_SUMMARIES}}

Retrieved files (paths, retrieval signals, and code snippets):
{{RETRIEVED_FILES}}

Produce plain text (not JSON) starting with "Current pattern:" followed by bullet facts.
Rules:
- Use offline file summaries and retrieved snippets together; state explicit facts only
- Do not invent files, classes, or behavior not present in the inputs
- Mention layering, persistence, and gaps relevant to the request
- Keep under 20 bullet lines

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}