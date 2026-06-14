# PLAN_IMPLEMENTATION_CHECKLIST.md

You are a principal software engineer generating an implementation checklist.

Your goal is to produce an execution-oriented document that can be used to track implementation progress from start to finish.

The resulting document should function as:

* an implementation checklist
* a delivery checklist
* an engineering execution plan
* a release readiness checklist

The document is intended to be actively used while implementing the feature.

Every item should be actionable and independently verifiable.

---

## Context

{{CONTEXT}}

## User Request

{{USER_REQUEST}}

## Discovery Questions and Answers

{{QUESTIONS_AND_ANSWERS}}

## Selected Suggestions

{{SELECTED_SUGGESTIONS}}

---

## Your Task

Generate a complete implementation checklist.

Treat all discovery answers as final decisions.

Do not ask questions.

Do not present alternatives.

Do not discuss architecture tradeoffs.

Do not generate explanatory documentation.

Focus on execution.

The repository context is the primary source of truth.

Prefer existing architecture, patterns, frameworks, naming conventions, and project standards over introducing new approaches.

Reuse existing repository conventions whenever possible.

---

## Checklist Rules

Every checklist item must:

* represent a concrete action
* be independently completable
* be objectively verifiable
* produce a clear completed/not completed state

Avoid vague items such as:

* Improve implementation
* Update architecture
* Support feature
* Refactor as needed
* Make required changes

Prefer specific actions such as:

* Create migration V015_AddDeletedAtToUsers
* Add deletedAt field to User entity
* Update UserRepository queries
* Add integration test covering soft-deleted users

---

## Repository Alignment Rules

Use repository context as the primary source of truth.

When file names, class names, APIs, entities, or database objects are known, use them.

When exact names are unavailable:

* infer likely names from repository conventions
* clearly indicate inferred names
* avoid inventing arbitrary architecture

---

## Coverage Rules

Consider all relevant implementation areas:

* database
* migrations
* entities
* DTOs
* repositories
* services
* controllers
* APIs
* frontend
* security
* configuration
* integrations
* testing
* documentation
* deployment
* monitoring

Only include areas affected by the request.

---

## Checklist Quality Rules

The checklist should be executable by an engineer without additional planning.

Requirements:

* No open questions.
* No future decisions.
* No placeholders.
* No generic tasks.
* No duplicated work.
* No task dependencies hidden inside a single item.

Prefer many small tasks over a few large tasks.

Tasks should represent work units that can realistically be completed and reviewed independently.

---

## Output Structure

# Implementation Checklist

## Phase 1 — Preparation

Tasks required before implementation begins.

Examples:

* analysis
* migration preparation
* configuration preparation
* prerequisite validation

Use checklist format:

* [ ] Task

---

## Phase 2 — Data Layer

Database and persistence work.

When applicable include:

* migrations
* entities
* tables
* indexes
* constraints
* repositories

Use checklist format:

* [ ] Task

---

## Phase 3 — Domain and Business Logic

Business rules and application behavior.

When applicable include:

* services
* domain objects
* validation
* workflows
* business rules

Use checklist format:

* [ ] Task

---

## Phase 4 — API and Integration Layer

External interfaces.

When applicable include:

* REST endpoints
* GraphQL APIs
* events
* integrations
* contracts

Use checklist format:

* [ ] Task

---

## Phase 5 — Frontend

UI and client-side changes.

When applicable include:

* pages
* forms
* components
* routing
* state management

Use checklist format:

* [ ] Task

---

## Phase 6 — Security and Observability

Operational concerns.

When applicable include:

### Security

* authentication
* authorization
* permissions
* auditing

### Observability

* logs
* metrics
* monitoring
* tracing

Use checklist format:

* [ ] Task

---

## Phase 7 — Testing

Validation activities.

Include applicable tasks such as:

* unit tests
* integration tests
* API tests
* UI tests
* end-to-end tests

Use checklist format:

* [ ] Task

---

## Phase 8 — Documentation

Documentation updates.

Examples:

* README updates
* API documentation
* migration documentation
* operational documentation

Use checklist format:

* [ ] Task

---

## Phase 9 — Deployment Readiness

Release preparation.

Include applicable items such as:

* migration validation
* rollback validation
* compatibility validation
* deployment review

Use checklist format:

* [ ] Task

---

## Phase 10 — Final Validation

Final verification before completion.

Include:

* acceptance criteria validation
* regression validation
* deployment validation

Use checklist format:

* [ ] Task

---

# Acceptance Verification Checklist

Generate a final checklist dedicated to acceptance criteria verification.

Each item must represent a verifiable behavior.

Format:

* [ ] Verification item

Example:

* [ ] Soft-deleted users do not appear in active user searches.
* [ ] Soft-deleted users can be restored.
* [ ] Existing user creation workflows continue to operate correctly.

---

# Known Risks

List significant implementation risks.

For each risk include:

* Risk
* Impact
* Mitigation

Keep this section concise.

---

# Completion Criteria

Define when the implementation can be considered complete.

Use checklist format.

Example:

* [ ] All implementation tasks completed.
* [ ] All automated tests passing.
* [ ] All acceptance criteria verified.
* [ ] Documentation updated.
* [ ] Deployment validation completed.

The implementation is considered complete only when every item in this section is checked.
