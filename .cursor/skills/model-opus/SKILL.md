---
name: model-opus
description: >
  Model overlay tuned for Opus-class reasoning. Increases analysis depth,
  validation strictness, and confidence gating. Use as third layer after base and mode.
disable-model-invocation: true
---

# model-opus (overlay)

Use as third layer: `base -> mode -> model-opus`.

## Reasoning depth

- Prefer deeper internal analysis before conclusions.
- Keep external output compact.
- Validate consistency against prior approved state.

## Validation gates

Before final answer, verify:

1. assumptions explicit
2. risk axes checked (security, data integrity, compatibility/contracts, performance)
3. no scope drift
4. no unintended edits
5. state read priority respected

## Confidence gate

- If confidence < 0.80, ask follow-up instead of forcing conclusion.
- If invariant or contract safety cannot be shown, stop and ask.

## architect depth protocol

Before writing state files:

1. reconstruct baseline from prior context + existing state files
2. generate candidate approaches when impact is non-trivial
3. compare at least 2 alternatives when uncertainty exists
4. run impact checklist (modules/boundaries, regressions, migration compatibility, rollback, consistency, security/perf/ops)
5. select recommendation only after checklist coverage

Consistency guardrails:

- keep stable response structure/order
- avoid contradiction with approved architecture unless explicitly documented
- if contradiction exists, record it in `@implementation/RISKS.md` and justify in `@implementation/ARCHITECTURE.md`

## implement strictness

Alternative exploration is mandatory when any trigger appears:

- unclear requirements
- uncertain tradeoff
- touched boundary/auth/migration/data contract
- non-trivial performance impact
- likely side effects

Anti-rush checklist before conclude:

1. intent classification done
2. assumptions explicit
3. impact checked on 4 axes
4. alternatives reviewed when triggered
5. state/context priority respected
6. requested scope unchanged
7. no unintended edits
8. state updated if implementation happened

Final consistency pass:

- output/actions match current request, state files, assumptions, selected approach

## Risk-scan strictness

For critical/high findings:

- require evidence tuple (file/source, flow, trigger)
- compare at least 2 mitigation alternatives
- include one negative-path test idea (`what breaks`)

For every finding, require explicit impact fields:

- security
- data integrity/migration
- compatibility/contracts
- performance/cost
- rollback/recovery
- test coverage gaps

Use deterministic evaluation order:

1. security/auth
2. data loss/migration
3. consistency/contracts
4. concurrency/races
5. rollback/operability
6. performance/cost
