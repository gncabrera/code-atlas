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

