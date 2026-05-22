---
name: auto-mode
description: >
  Mode overlay for automatic workflows. Adds intent classification and adaptive
  question gates before execution. Use together with architect/implement/risk-scan.
disable-model-invocation: true
---

# auto-mode (overlay)

Use as second layer after base skill: `base -> auto-mode -> model`.

## Global constraints

- Keep output compact (`/caveman` style).
- Ask-first behavior enabled for ambiguous/high-risk requests.
- Do not continue with unresolved blocking ambiguity.
- Reuse relevant prior context by default (current request + referenced decisions + unresolved constraints).

## Intent classifier gate

Before edits, classify request intent:

- `implement` -> edits allowed
- `plan` | `analysis` | `debug` | `review` | `question-only` -> no edits

If non-implement intent, return analysis/questions only.

## Adaptive question gate

Question depth by risk:

- low risk + clear scope: 1 minimum
- medium risk/partial ambiguity: 2-4
- high risk/high ambiguity: 5-7

Blocking ambiguity unresolved -> stop and ask.

## Risk signals

Treat as high-risk triggers:

- auth/session
- migration/data integrity
- contract compatibility
- rollback uncertainty

## architect overrides

- Ask all blocking questions before writing state files.
- Stop and wait for answers; do not proceed on unanswered blockers.
- Keep state-file-only behavior (`@implementation/ARCHITECTURE.md`, `@implementation/TASKS.md`, `@implementation/RISKS.md`).
- Chat output format:
  - `decision`
  - `alternatives` (only if needed)
  - `impact`
  - `state`
  - final line: `state written.`

## implement overrides

- Run quick assumption checklist before editing.
- Keep strict scope execution; no out-of-scope expansion.
- If request is non-implement intent, no patches; analysis/questions only.

## risk-scan overrides

- Ask targeted clarifying questions before analysis when risk/context requires.
- If critical context/evidence missing, mark blocked and ask for data.
- If request implies code edits, refuse edits and continue risk analysis only.
- Keep findings file-only output in `@implementation/RISK_SCAN.md`.
- Max findings becomes 15, ordered by highest risk first.
