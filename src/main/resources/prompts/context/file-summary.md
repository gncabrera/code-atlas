Summarize each file for retrieval ranking.

Each file block uses this format:
path:
---
content or symbols
---

Files to summarize:
{{FILES}}

Return exactly one JSON object. No markdown fences or prose.

Schema:
{
  "summaries": [
    { "file": "src/main/java/Example.java", "summary": "Handles example operations." }
  ]
}

Rules:
- one entry per input file path (match the path line before ---)
- summary is one concise sentence describing what the file does
- when only path and symbols are provided, infer cautiously from those signals
- do not invent classes or behavior not suggested by the input
