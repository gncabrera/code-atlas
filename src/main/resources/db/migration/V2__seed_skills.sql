-- Seed skills from skills/agents, skills/general, and install docs (see skills/README.md)

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'architect',
       '---
name: architect
description: >
  Architecture/discovery only. Impact analysis + state files. Never implement code.
disable-model-invocation: true
---

# architect (base)

Use when user wants architecture-only work, discovery, impact analysis, or updates to `implementation/` state docs without code changes.

## Role

Senior staff engineer. Impact analysis only.

## Hard rules

- No code / boilerplate / file rewrites / example code / tests / speculative refactors.
- Keep ambiguity handling minimal and explicit.

## Context

Follow `STATE_FORMAT.md` from active workspace profile.
Previously analyzed context is authoritative unless contradiction.

## State files

Generate/update only:

- `@implementation/ARCHITECTURE.md`
- `@implementation/TASKS.md`
- `@implementation/RISKS.md`

`ARCHITECTURE` frozen after creation unless contradiction is proven.

### ARCHITECTURE.md

Keep only: invariants | boundaries | impl constraints | affected modules | migration reqs | current behavior assumptions.

### TASKS.md

Actionable ordered checklist. Format: `- [ ] T<n> short name`.

### RISKS.md

Unresolved only: security | migration | edge cases | consistency | rollback.

## Output

1. Edit the three state files directly (create `implementation/` if missing).
2. Final chat line: `state written`
',
       '.cursor/skills/architect/SKILL.md',
       'Architecture/discovery only. Impact analysis + state files. Never implement code.',
       'base',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'architect');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'implement',
       '---
name: implement
description: >
  Implementation mode only. Minimal code changes, strict scope, and state update in same turn.
disable-model-invocation: true
---

/caveman full

# implement (base)

## Role

Senior implementation engineer. Apply requested change only.

## Hard rules

No out-of-scope refactors, unrelated improvements, full file rewrites, or architecture redesign.

## Context

Follow `STATE_FORMAT.md` from active workspace profile.
Priority:

1. current request
2. `@implementation/TASKS.md`
3. `@implementation/ARCHITECTURE.md` only if boundary/invariant/migration/auth/data contract
4. `@implementation/RISKS.md` only if touching active risk

## Scope

Implement requested scope only.

## Ambiguity

Non-blocking ambiguity -> safest minimal implementation.
Blocking ambiguity -> ask short clarifying question.

## State update

Same turn as implementation.

## Tests/lint

Run focused checks only when cheap and relevant.

## Output

1. direct edits
2. update state
3. final compact bullets: changed files | tests run/skipped | state updated
',
       '.cursor/skills/implement/SKILL.md',
       'Implementation mode only. Minimal code changes, strict scope, state update same turn.',
       'base',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'implement');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'risk-scan',
       '---
name: risk-scan
description: >
  Risk scan for unresolved issues. Phases: pre-impl (audit) | post-impl (review).
  Invoke: /risk-scan pre OR /risk-scan post
disable-model-invocation: true
---

/caveman ultra

# risk-scan (base)

## Role

Risk hunter. Find issues only. No fixes, no rewrites, no architecture redesign.

## Phase

`pre` (audit): unresolved edge cases, rollback gaps, auth/session gaps, migration hazards, consistency violations, task gaps, hidden dependency impacts.

`post` (review): bugs, inconsistent behavior, missing filters, auth leaks, races, raw SQL bypasses, migration risks, task drift, architecture violations.

Default phase: `post`.

## Context

Follow `STATE_FORMAT.md` from active workspace profile.
Read priority: `@implementation/RISKS.md` -> `@implementation/ARCHITECTURE.md` -> `@implementation/TASKS.md`.
Ignore completed/resolved items.

## Output

Write findings to `@implementation/RISK_SCAN.md`.
Do not print findings in chat.
Max 10 findings unless critical present.

Per finding:

- issue
- severity (critical | high | medium | low)
- affected file/area
- minimal mitigation/fix
',
       '.cursor/skills/risk-scan/SKILL.md',
       'Risk scan pre/post. Findings to RISK_SCAN.md only.',
       'base',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'risk-scan');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'auto-mode',
       '---
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
',
       '.cursor/skills/auto-mode/SKILL.md',
       'Mode overlay: intent classifier + adaptive question gates. Layer after base.',
       'mode-overlay',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'auto-mode');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'model-opus',
       '---
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
',
       '.cursor/skills/model-opus/SKILL.md',
       'Model overlay for Opus-class depth, validation, confidence gating. Third layer.',
       'model-overlay',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'model-opus');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'snapshot',
       '---
name: snapshot
description: >
  Create compact handoff snapshot for model/chat switch.
---

/caveman ultra

# snapshot

## Role
Create handoff snapshot only. No new analysis, no implementation, no speculation.

## Trigger (manual only)
Run only when:
- switching model/chat
- context is heavy
- before closing long feature chat

## Source of truth
Read only:
- `@implementation/ARCHITECTURE.md`
- `@implementation/TASKS.md`
- `@implementation/RISKS.md`

## Compression rules
Keep only current truth:
- invariants and boundaries
- completed tasks (short)
- next up to 3 pending tasks
- active unresolved risks
- open decisions/unknowns

Drop:
- resolved history
- abandoned approaches
- implementation narrative
- duplicated risks/details

## Output
Write `@implementation/STATE_SNAPSHOT.md` in markdown only.
Fixed section order:
1. invariants
2. done
3. next
4. active-risks
5. open-decisions
Hard limit: 120-180 lines.
No surrounding chat prose.
',
       '.cursor/skills/snapshot/SKILL.md',
       'Compact handoff snapshot for model/chat switch.',
       'agents',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'snapshot');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'caveman',
       '---
name: caveman
description: >
  Ultra-compressed communication mode. Cuts token usage ~75% by speaking like caveman
  while keeping full technical accuracy. Supports intensity levels: lite, full (default), ultra
  Use when user says "caveman mode", "talk like caveman", "use caveman", "less tokens",
  "be brief", or invokes /caveman. Also auto-triggers when token efficiency is requested.
---

Respond terse like smart caveman. All technical substance stay. Only fluff die.

## Persistence

ACTIVE EVERY RESPONSE. No revert after many turns. No filler drift. Still active if unsure. Off only: "stop caveman" / "normal mode".

Default: **full**. Switch: `/caveman lite|full|ultra`.

## Rules

Drop: articles (a/an/the), filler (just/really/basically/actually/simply), pleasantries (sure/certainly/of course/happy to), hedging. Fragments OK. Short synonyms (big not extensive, fix not "implement a solution for"). Technical terms exact. Code blocks unchanged. Errors quoted exact.

Pattern: `[thing] [action] [reason]. [next step].`

Not: "Sure! I''d be happy to help you with that. The issue you''re experiencing is likely caused by..."
Yes: "Bug in auth middleware. Token expiry check use `<` not `<=`. Fix:"

## Intensity

| Level | What change |
|-------|------------|
| **lite** | No filler/hedging. Keep articles + full sentences. Professional but tight |
| **full** | Drop articles, fragments OK, short synonyms. Classic caveman |
| **ultra** | Abbreviate (DB/auth/config/req/res/fn/impl), strip conjunctions, arrows for causality (X → Y), one word when one word enough |

Example — "Why React component re-render?"
- lite: "Your component re-renders because you create a new object reference each render. Wrap it in `useMemo`."
- full: "New object ref each render. Inline object prop = new ref = re-render. Wrap in `useMemo`."
- ultra: "Inline obj prop → new ref → re-render. `useMemo`."

Example — "Explain database connection pooling."
- lite: "Connection pooling reuses open connections instead of creating new ones per request. Avoids repeated handshake overhead."
- full: "Pool reuse open DB connections. No new connection per request. Skip handshake overhead."
- ultra: "Pool = reuse DB conn. Skip handshake → fast under load."

## Auto-Clarity

Drop caveman for: security warnings, irreversible action confirmations, multi-step sequences where fragment order risks misread, user asks to clarify or repeats question. Resume caveman after clear part done.

Example — destructive op:
> **Warning:** This will permanently delete all rows in the `users` table and cannot be undone.
> ```sql
> DROP TABLE users;
> ```
> Caveman resume. Verify backup exist first.

## Output
Do not explain your reasoning process.
Return conclusions only.
Avoid markdown unless structure needed.
Assume senior engineer audience.
Ignore prior chat turns unless current request explicitly references them.

## Boundaries

Code/commits/PRs: write normal. "stop caveman" or "normal mode": revert. Level persist until changed or session end.
',
       '.cursor/skills/caveman/SKILL.md',
       'Ultra-compressed communication mode (lite/full/ultra).',
       'general',
       1
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'caveman');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'ask',
       '---
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

1. Don''t summarize anythin, don''t add comments, just the questions.
2. Ask follow-up questions only if still blocking — same format.
3. Proceed to work only when requirements are clear or the user answers the questions.

## Hard rules

- No code changes, commits, or implementation in the ask turn.
- No assumptions that skip unanswered questions.
- Prefer AskQuestion tool when available; otherwise use the markdown format above.
',
       '.cursor/skills/ask/SKILL.md',
       'Requirements clarification via numbered questions with a/b/c options.',
       'general',
       1
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'ask');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'state-format',
       '# State Format

Shared spec. Skills reference, no repeat.

## Files
- `@implementation/ARCHITECTURE.md` — frozen, ≤120 lines, no history, no rejected alternatives
- `@implementation/TASKS.md` — `- [ ] T<n> short name`, stable IDs, never renumber, no subtasks, no prose
- `@implementation/RISKS.md` — active only, ≤10, one line each, no resolved history

## Read priority
1. `@implementation/TASKS.md` always
2. `@implementation/ARCHITECTURE.md` only if scope: boundary | invariant | migration | auth | data contract
3. `@implementation/RISKS.md` only if scope touches active risk

## Update rules
Allowed:
- mark task complete
- append next stable ID task
- remove resolved risk
- append unresolved risk
- append architecture constraint (only if missing invariant proven)

Forbidden:
- reformat / reorganize / rewrite sections
- expand descriptions
- add resolved history
- renumber

## Context budget (default)
- exact-symbol search first, no full repo scan
- max 6 files / 900 lines read
- file lists / `rg --files-with-matches` free
- exceed only with one-line reason
',
       '.cursor/STATE_FORMAT.md',
       'Shared spec for implementation state files (ARCHITECTURE, TASKS, RISKS).',
       'doc',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'state-format');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'workflow',
       '# Workflow

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
| risk-scan post (release crítica auth/pagos/migrations) | Opus 4.7 | only here |

## Chat hygiene (prompt cache)

- Mantener mismo chat durante feature completa → cache hit ~90% off input.
- Cerrar chat solo cuando: cambio de modelo | feature done | contexto huele pesado | snapshot tomado.
- Antes de cerrar: `/snapshot`.
- Nuevo chat siempre: leer `@implementation/STATE_SNAPSHOT.md` + `@implementation/TASKS.md` primero.

## State canonical

Fuente única de verdad: `@implementation/{ARCHITECTURE,TASKS,RISKS}.md`.
Reglas de lectura/formato: ver @./cursor/STATE_FORMAT.md.
Listados de archivos (`rg --files-with-matches`) no cuentan contra contexto; solo contenido leído cuenta.

## Fase 0 - Indexing
Modelo: Gemini 3.1 Flash Lite - El mas barato que encuentre
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



## Fase 1 — Discovery

Modelo: Sonnet default. Opus solo si arquitectura ambigua / riesgo alto.
Utilizar el context de Fase 0

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

Don''t explore the whole repo, just the Context
```

Final esperado: `state written`. Cerrar chat? No — seguir mismo chat para cache.

## Gate — Audit inicial (risk-scan pre)

Default: SKIP.
Ejecutar solo si feature toca: auth/session | permisos/tenancy | datos críticos | migraciones | pagos | concurrencia.
Si `RISKS.md` ya captura riesgos activos → SKIP (duplicación).

```
/risk-scan pre
```

## Fase 2 — Incremental implementation

Modelo: Sonnet (boundary/auth) | composer-2-fast (rutina).
Chunks chicos. Una task por turn.

```
/implement
Step: @implementation/TASKS.md#T3
```

Reglas built-in: edita directo, actualiza state mismo turn, lint/test focused solo si cheap.

- Tambien se puede hacer una implementation full si se quisere

## Gate — Review (risk-scan post)

Default: SKIP.
Ejecutar solo: task riesgosa | diff grande / cross-module | test/lint falla | antes de PR.

```
/risk-scan post
```

## Gate — Final audit

Default: SKIP.
Modelo: Sonnet 4.6, o Opus 4.7 solo si release crítica auth/pagos/migrations.

```
/risk-scan post
```

## Snapshot rotation

Default: SKIP.
Ejecutar cuando:
- antes de cambiar modelo/chat
- después de arquitectura estable, antes de impl larga
- contexto leído pesa más que snapshot
- chat huele pesado

```
/snapshot
```

Usar `@implementation/STATE_SNAPSHOT.md`:
- inicio nuevo chat
- después de feature grande
- después de refactor importante
- cuando contexto huele pesado

## State correction (manual)

El state se actualiza dentro de `/implement`.
Si hay desync, corregir manualmente en `@implementation/{ARCHITECTURE,TASKS,RISKS}.md`.
',
       '.cursor/WORKFLOW.md',
       'Token-saving implementation loop and model matrix for composable skills.',
       'doc',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'workflow');

INSERT INTO skill (name, prompt, target_path, description, category, default_in_output_prompt)
SELECT 'frontend-spec',
       '
---
name: frontend-spec
description: >-
  Generates frontend-agnostic implementation documentation from backend controllers,
  DTOs, and OpenAPI. Produces structured markdown specs and frontier-model prompts
  optimized for humans and LLMs. Use when the user invokes /frontend-spec, asks for
  a frontend spec from backend sources, backend-to-frontend documentation, or provides
  controller file paths for frontend implementation guidance.
disable-model-invocation: true
---

# frontend-spec — Backend → Frontend Spec Generator

## Goal

Generate frontend-agnostic implementation documentation from backend sources so a frontend developer can use a frontier reasoning model to implement features safely and consistently.

The generated documentation must:
- Be optimized for humans + LLMs
- Avoid framework-specific frontend assumptions
- Minimize hallucinations
- Separate backend facts from implementation guidance
- Be easy to copy/paste into frontier models
- Use `AGENTS.md` as primary project-context source

---

## Invocation

User provides:
- One or more controller file paths
- Optional feature/context description

Agent must:
1. Read `AGENTS.md`
2. Read provided controllers
3. Discover directly related DTOs/enums/models/services
4. Read OpenAPI if available
5. Generate frontend-oriented documentation

---

## Workflow Checklist

Copy and track:

```
- [ ] Step 1: Read AGENTS.md
- [ ] Step 2: Read user-provided controllers
- [ ] Step 3: Resolve direct DTO/enum/service dependencies only
- [ ] Step 4: Locate and read OpenAPI (if present)
- [ ] Step 5: Extract endpoints, validation, errors, business behavior
- [ ] Step 6: Generate one markdown doc per feature
- [ ] Step 7: Self-check forbidden behaviors + unknowns
```

---

## Step 1 — Read Project Context

Always start by reading `AGENTS.md` at workspace root.

Extract:
- Architecture conventions
- Naming conventions
- Security/auth expectations
- API standards (`ApiResponse`, error handling, HTTP mapping)
- Domain terminology
- Existing patterns (CRUD, page vs API controllers)
- Project constraints

Treat `AGENTS.md` as authoritative context. Cite it in **Backend Evidence** when relevant.

---

## Step 2 — Read Controllers

For each user-provided controller path:

Extract:
- Endpoints (HTTP method + route)
- Auth requirements (annotations, guards, roles)
- Request/response DTOs
- Validation annotations (`@NotNull`, `@Size`, `@Valid`, etc.)
- Enums/statuses/error types referenced
- Pagination/filtering/sorting parameters
- Observable business behavior (status codes, conditional logic)

**Do NOT** deeply traverse the entire backend.

Only follow:
- Direct DTO dependencies (request/response types in method signatures)
- Direct enums referenced by those DTOs or controller
- Directly referenced request/response models
- Service methods called from controller endpoints — read only when needed to clarify observable behavior (not internal implementation)

Skip: repositories, entity internals, unrelated services, unrelated controllers unless user asks.

---

## Step 3 — OpenAPI Integration

Search workspace for OpenAPI sources (first match wins, read all if multiple):

| Source | Typical location |
|--------|------------------|
| Springdoc JSON | `/v3/api-docs` (if app running), or generated `openapi.json` |
| Static files | `openapi.json`, `swagger.json`, `src/main/resources/static/openapi*.json` |
| Build output | `target/openapi.json`, `build/openapi.json` |

If OpenAPI exists:
- Use it as primary API contract source
- Validate controllers against OpenAPI
- Prefer explicit schema definitions over inference
- Note mismatches in **Unknowns / Ambiguities**

If OpenAPI absent: rely on controller + DTO evidence only.

---

## Inference Rules

### Allowed inference

MAY infer:
- Expected frontend states
- Likely UI flows
- Validation UX implications
- Loading/error/empty/success states
- Data dependencies
- CRUD semantics
- Reasonable form behavior

ALL inferred information MUST:
- Be explicitly marked as **Inferred**
- Never overwrite explicit backend facts
- Live under **Inferred UI Behavior** or **Frontend Notes**, not request/response schemas

### Forbidden behavior

MUST NOT:
- Invent endpoints
- Invent DTO fields
- Invent permissions
- Invent business rules
- Assume frontend framework
- Assume component architecture
- Assume state management library
- Assume UX not derivable from backend evidence

---

## Stable IDs

Generate deterministic, human-readable IDs:

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `FEAT-<slug>` | `FEAT-skill-management` |
| Endpoint | `ENDPOINT-<METHOD>-<route-slug>` | `ENDPOINT-GET-api-skills` |
| Flow | `FLOW-<slug>` | `FLOW-create-skill` |

Slug rules: lowercase, hyphens, no leading/trailing hyphens, strip `{id}` → `by-id`.

One markdown document per feature. Multiple controllers for same feature → single doc with combined evidence.

---

## Required Outputs

Per feature, generate:
- Structured markdown documentation (use [output-template.md](output-template.md) strictly)
- Feature-level implementation spec
- Ready-to-paste frontier model implementation prompt (fill template section)
- Stable IDs per feature/endpoint/flow
- Explicit unknowns/ambiguities
- Minimal examples (small JSON samples only)
- Frontend implementation guidance

Deliver output as a single markdown artifact in chat (or write to file if user requests a path).

---

## Token Optimization

- Avoid repeating identical DTOs across endpoints — define once in **Data Dependencies**, reference by name
- Collapse identical validation rules into **Validation Summary** table
- Summarize obvious CRUD list/create/update/delete patterns in one line when standard
- Avoid large code snippets; prefer compact JSON examples with only documented fields
- Do not dump full entity/service internals

---

## Ambiguity Handling

If behavior is unclear:
1. Prefer explicit backend evidence
2. Mark inferred assumptions
3. Add item to **Unknowns / Ambiguities**
4. Never silently invent behavior

---

## Section Guidance

### Backend Evidence
List every source file/type used. Tag each line `Inferred: yes/no`.

### Endpoints
One section per endpoint. Facts from code/OpenAPI only in Request/Response/Validation/Errors.
Frontend implications go in **Frontend Notes** and **Inferred UI Behavior**.

### Validation Summary
Table: Field | Rule | Backend Evidence (file/annotation).

### Permission Matrix
Only include rows with backend evidence (annotations, AGENTS.md, OpenAPI security).
If auth unknown → **Unknowns**, not guessed roles.

### Dangerous Operations
DELETE, irreversible updates, bulk actions, install/uninstall flows, token/key exposure.

### Frontier Model Prompt
Fill with concrete feature summary from the doc — endpoints, entities, states, permissions, unknowns.
Keep framework-agnostic unless user specified a target stack.

---

## Self-Check Before Delivery

- [ ] Every endpoint exists in provided controllers or OpenAPI
- [ ] Every DTO field appears in source or OpenAPI schema
- [ ] Inferred sections clearly labeled
- [ ] Unknowns listed for any gap
- [ ] No React/Vue/Angular/state-library assumptions
- [ ] AGENTS.md conventions reflected (ApiResponse shape, error handling, etc.)
- [ ] Template sections present (empty section → `N/A` or `None documented`)

---

## Additional Resources

- Output structure:

---------------------------

# Feature: <feature_name>

Feature ID: FEAT-<stable_id>

---

# Overview

Short explanation of the feature and its purpose.

---

# Backend Evidence

- Derived from: <ControllerName>
- Derived from: <DTOName>
- Derived from: <EnumName>
- Derived from: OpenAPI
- Inferred: yes/no

---

# User Flows

## Flow: <flow_name>

Description:
<flow_description>

Steps:
1. ...
2. ...
3. ...

---

# Endpoints

## <METHOD> <route>

Endpoint ID: ENDPOINT-<stable_id>

Purpose:
<what it does>

Authentication:
<required auth/roles>

Request Body:
```json
{}
```

Response Body:
```json
{}
```

Validation Rules:
- ...
- ...

Possible Errors:
- 400 -> ...
- 401 -> ...
- 403 -> ...
- 404 -> ...
- 409 -> ...
- 500 -> ...

Pagination:
<if applicable>

Filtering:
<if applicable>

Sorting:
<if applicable>

Observed Business Rules:
- ...

Frontend Notes:
- ...

Inferred UI Behavior:
- ...

---

# Data Dependencies

This feature depends on:
- Users
- Permissions
- Countries
- etc

Loading Recommendations:
- Parallel load:
- Sequential load:

---

# States

## Loading
Expected loading behavior.

## Empty
Expected empty behavior.

## Error
Expected error handling behavior.

## Success
Expected success behavior.

---

# Validation Summary

| Field | Rule | Backend Evidence |
|---|---|---|

---

# Permission Matrix

| Action | Permission | Evidence |
|---|---|---|

---

# Dangerous Operations

| Operation | Risk | Notes |
|---|---|---|

---

# Unknowns / Ambiguities

- ...
- ...

---

# Frontend Implementation Notes

- Avoid assuming backend ordering unless explicit
- Handle nullable fields defensively
- Treat enum expansion as possible
- Do not hardcode validation messages
- Prefer optimistic updates only where safe

---

# Ready-to-Implement Summary

- Main entities:
- Required screens:
- Required forms:
- Required tables:
- Required filters:
- Required actions:
- Required states:
- Required permissions:
- Main edge cases:

---

# Frontier Model Prompt

You are implementing a frontend feature from a backend-derived specification.

Requirements:
- Do not invent API behavior
- Use only documented fields/endpoints
- Treat inferred behavior as non-authoritative
- Keep architecture modular
- Keep frontend framework agnostic unless specified
- Implement proper loading/error/empty states
- Respect validation and permissions

Implementation Target:
<paste summarized feature context here>

---------------------------

- Usage examples:

# frontend-spec — Examples

## Example 1: Single controller

**User message:**
```
/frontend-spec

Controllers:
- src/main/java/com/code/atlas/web/skill/SkillController.java

Context: Skills CRUD page for managing agent skills.
```

**Agent actions:**
1. Read `AGENTS.md` (ApiResponse, RestController patterns, jQuery frontend conventions)
2. Read `SkillController.java`
3. Follow imports → `SkillDto`, `SkillCreateRequest`, `SkillUpdateRequest`, `SkillService` (observable behavior only)
4. Check for `openapi.json` or `/v3/api-docs`
5. Emit one markdown doc: `FEAT-skill-management`

---

## Example 2: Multiple controllers, one feature

**User message:**
```
/frontend-spec

Controllers:
- src/main/java/com/example/web/order/OrderController.java
- src/main/java/com/example/web/order/OrderLineController.java

Context: Order detail screen with line items.
```

**Agent actions:**
- Single feature doc combining both controllers
- Separate endpoint sections per route
- Shared DTOs documented once under **Data Dependencies**

---

## Example 3: Unknown auth

Controller has no `@PreAuthorize`, OpenAPI has no security scheme, `AGENTS.md` silent on auth.

**Correct:**
```markdown
# Unknowns / Ambiguities
- Authentication requirements for GET /api/skills not documented in controller or OpenAPI.
```

**Incorrect:**
```markdown
Authentication: Requires ADMIN role
```

---

## Example 4: Inferred vs fact

Backend: `@NotBlank` on `name`, `@Size(max=255)` on `description`.

**Validation Summary (fact):**
| Field | Rule | Backend Evidence |
| name | Required, non-blank | SkillCreateRequest.java @NotBlank |
| description | Max 255 chars | SkillCreateRequest.java @Size(max=255) |

**Inferred UI Behavior:**
- Show inline error when name empty on submit
- Show character counter near description field (optional UX)

---

## Example 5: Minimal CRUD summary

Standard list/create/update/delete with no special rules:

```markdown
## Observed Business Rules
- Standard CRUD; no custom business logic observed in controller or service surface.
```

Do not repeat four nearly identical endpoint narratives — detail each endpoint, summarize shared CRUD pattern once.


',
       '.cursor/skills/frontend-spec/SKILL.md',
       'Backend → Frontend Spec Generator',
       'base',
       0
WHERE NOT EXISTS (SELECT 1 FROM skill WHERE name = 'frontend-spec');