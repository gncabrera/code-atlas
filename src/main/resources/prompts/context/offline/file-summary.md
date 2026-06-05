Summarize each file for retrieval ranking.

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
- one entry per input file path
- summary is one concise sentence
