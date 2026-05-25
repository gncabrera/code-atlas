# Workflow

Token-saving impl loop. Skills assume @./cursor/STATE_FORMAT.md format.

## Model matrix

| Phase | Model | Reason |
|-------|-------|--------|
| architect (ambiguous arch / high risk) | Opus 4.7 | reasoning depth |
| architect (clear scope) | Sonnet 4.6 | cheap enough |
| implement (boundary/auth/migration) | Sonnet 4.6 | safety |
| implement (routine edits, scope clear) | composer-2-fast or gpt-5.5-medium | -80% cost |
| snapshot | composer-2-fast | mechanical |
| risk-scan pre/post (normal) | Sonnet 4.6 | reasoning |
| risk-scan post (critical release auth/payments/migrations) | Opus 4.7 | only here |

## Chat hygiene (prompt cache)

- Keep the same chat for the full feature → cache hit ~90% off input.
- Close chat only when: model change | feature done | context feels heavy | snapshot taken.
- Before closing: `/snapshot`.
- New chat always: read `@implementation/STATE_SNAPSHOT.md` + `@implementation/TASKS.md` first.

## State canonical

Single source of truth: `@implementation/{ARCHITECTURE,TASKS,RISKS}.md`.
Read/format rules: see @./cursor/STATE_FORMAT.md.
File listings (`rg --files-with-matches`) do not count against context; only read content counts.

## Phase 0 - Indexing
Model: Gemini 3.1 Flash Lite - Cheapest you can find
Input:
```
Feature: soft delete users.
Endpoint: DELETE /api/profile

Need:
- schema changes
- affected queries
- auth/session implications
- cascade risks
```

Output:
```
@path/to/file1 L1-L50
@path/to/file2 L80-L90
@path/to/file3 L563-L612

```



## Phase 1 — Discovery

Model: Sonnet default. Opus only if architecture ambiguous / high risk.
Use Phase 0 context

```
/architect

Feature: soft delete users.
Endpoint: DELETE /api/profile
Write: @implementation/{ARCHITECTURE,TASKS,RISKS}.md

Need:
- schema changes
- affected queries
- auth/session implications
- cascade risks

Context:
@path/to/file1 L1-L50
@path/to/file2 L80-L90
@path/to/file3 L563-L612

Don't explore the whole repo, just the Context
```

Expected final: `state written`. Close chat? No — keep same chat for cache.

## Gate — Initial audit (risk-scan pre)

Default: SKIP.
Run only if feature touches: auth/session | permissions/tenancy | critical data | migrations | payments | concurrency.
If `RISKS.md` already captures active risks → SKIP (duplication).

```
/risk-scan pre
```

## Phase 2 — Incremental implementation

Model: Sonnet (boundary/auth) | composer-2-fast (routine).
Small chunks. One task per turn.

```
/implement
Step: @implementation/TASKS.md#T3
```

Built-in rules: edit directly, update state same turn, lint/test focused only if cheap.

- You can also do a full implementation if desired

## Gate — Review (risk-scan post)

Default: SKIP.
Run only: risky task | large diff / cross-module | test/lint fails | before PR.

```
/risk-scan post
```

## Gate — Final audit

Default: SKIP.
Model: Sonnet 4.6, or Opus 4.7 only if critical release auth/payments/migrations.

```
/risk-scan post
```

## Snapshot rotation

Default: SKIP.
Run when:
- before changing model/chat
- after architecture stable, before long impl
- read context weighs more than snapshot
- chat feels heavy

```
/snapshot
```

Use `@implementation/STATE_SNAPSHOT.md`:
- new chat start
- after large feature
- after important refactor
- when context feels heavy

## State correction (manual)

State is updated inside `/implement`.
If desync, fix manually in `@implementation/{ARCHITECTURE,TASKS,RISKS}.md`.
