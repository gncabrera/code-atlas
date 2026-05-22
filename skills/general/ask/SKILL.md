---
name: ask
description: >
  Clarifies requirements before work starts via numbered questions with three
  labeled options each. Use when the user invokes /ask, says "ask mode",
  "ask questions first", or wants requirements clarified before implementation.
disable-model-invocation: true
---

# ask

Discovery-only mode. Gather requirements; do not implement, edit files, or run destructive commands until the user answers.

## Questions

- Ask all needed questions before starting
- Enumerate them: 1,2,3...
- Offer 3 Options: a,b,c
- Mark one option as default

## Workflow

1. Read the request and infer what is unclear or blocking.
2. List every question needed in one message (do not drip-feed).
3. Stop. Wait for answers before planning or coding.

## Question format

Use this structure for each question:

```markdown
**N. [Short question]**

- **a)** [Option text]
- **b)** [Option text] *(default)*
- **c)** [Option text]
```

Rules:

- Exactly three options per question: **a**, **b**, **c** only.
- Mark the default with `*(default)*` on that line (usually the safest or most common choice).
- Keep option text concrete; avoid overlap between a/b/c.
- If a question truly needs free text, still offer a/b/c and add: "Or reply with your own wording."

## After answers

1. Don't summarize anythin, don't add comments, just the questions.
2. Ask follow-up questions only if still blocking — same format.
3. Proceed to work only when requirements are clear or the user answers the questions.

## Hard rules

- No code changes, commits, or implementation in the ask turn.
- No assumptions that skip unanswered questions.
- Prefer AskQuestion tool when available; otherwise use the markdown format above.
