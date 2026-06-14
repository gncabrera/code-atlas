# PLAN_TECHNICAL_SPECIFICATION.md

You are a principal software engineer creating a technical specification document.

Your goal is to define exactly what must be implemented.

This document serves as the authoritative engineering specification for the feature.

It should eliminate ambiguity and provide sufficient detail for implementation, testing, review, and future maintenance.

The document may be used by:

* developers
* reviewers
* architects
* QA engineers
* future maintainers

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

Generate a complete technical specification.

Treat all discovery answers as final decisions.

Do not ask questions.

Do not present alternatives.

Do not discuss hypothetical solutions.

Do not generate implementation tasks.

Do not generate project management plans.

Focus on defining the required system behavior and technical changes.

The repository context is the primary source of truth.

Prefer existing architecture, patterns, frameworks, naming conventions, and project standards over introducing new approaches.

Reuse existing repository conventions whenever possible.

---

## Specification Rules

The specification must be implementation-independent where possible.

Describe:

* required behavior
* contracts
* constraints
* system responsibilities
* data requirements

Avoid describing:

* developer workflow
* ticket breakdown
* coding tasks
* sprint planning

The specification should answer:

* What must exist?
* How must it behave?
* What constraints apply?
* How can correctness be verified?

---

## Repository Alignment Rules

Use repository context as the primary source of truth.

Reference existing:

* modules
* services
* repositories
* APIs
* entities
* workflows

when known.

When exact names are unavailable:

* infer likely names from repository conventions
* clearly indicate inferred names
* avoid inventing arbitrary architecture

Do not introduce new technologies unless explicitly required.

---

## Coverage Rules

Consider all relevant areas:

* database
* migrations
* entities
* DTOs
* repositories
* services
* APIs
* frontend
* security
* configuration
* integrations
* observability
* testing
* deployment

Only include areas impacted by the request.

---

# Output Structure

# Executive Summary

Provide a concise summary of:

* feature objective
* scope
* expected outcome

Write 1–3 paragraphs.

---

# Scope

## In Scope

List all capabilities that are included.

## Out of Scope

List capabilities intentionally excluded.

Clearly define implementation boundaries.

---

# Functional Requirements

List all required system behaviors.

Each requirement should:

* be uniquely identifiable
* be testable
* describe observable behavior

Format:

FR-001
FR-002
FR-003

Example:

FR-001: Soft-deleted users must not appear in active user searches.

FR-002: Administrators must be able to restore a soft-deleted user.

---

# Non-Functional Requirements

Define quality attributes.

Include applicable requirements related to:

* performance
* scalability
* maintainability
* observability
* reliability
* security
* compatibility
* availability

Format:

NFR-001
NFR-002
NFR-003

Each requirement must be measurable whenever possible.

---

# Domain Model

Describe the business concepts involved.

For each concept include:

* purpose
* responsibilities
* relationships

Focus on domain understanding rather than implementation.

---

# Data Model Specification

Describe all required data changes.

When applicable include:

## Entities

* fields
* types
* responsibilities

## Database Tables

* columns
* constraints
* indexes

## Relationships

* one-to-one
* one-to-many
* many-to-many

## Migrations

Required schema evolution.

Describe expected end state.

---

# API Specification

Describe all affected APIs.

For each API include:

## Purpose

Why it exists.

## Endpoint

Method and path.

## Request Contract

Fields and validation rules.

## Response Contract

Response structure.

## Error Conditions

Expected error scenarios.

Only include APIs relevant to the implementation.

---

# Component Responsibilities

Describe responsibilities of major components.

Examples:

* Controllers
* Services
* Repositories
* Background Jobs
* UI Modules
* Integration Adapters

For each component describe:

* responsibilities
* dependencies
* inputs
* outputs

---

# Business Rules

Describe all business rules.

Each rule should be explicit and testable.

Format:

BR-001
BR-002
BR-003

Example:

BR-001: A soft-deleted user may not authenticate.

BR-002: User email addresses must remain unique even after soft deletion.

---

# Validation Rules

Describe validation requirements.

Include:

* input validation
* state validation
* domain validation
* API validation

Only include applicable validations.

---

# Security Requirements

Describe:

* authentication requirements
* authorization requirements
* access restrictions
* audit requirements
* data protection requirements

Only include applicable requirements.

---

# Observability Requirements

Describe required:

* logs
* metrics
* monitoring
* tracing
* audit events

Focus on operational visibility.

---

# Integration Requirements

Describe interactions with:

* external APIs
* messaging systems
* databases
* third-party services

Include:

* expected behavior
* failure handling
* retry expectations

Only include applicable integrations.

---

# Compatibility Requirements

Describe compatibility expectations.

Examples:

* backward compatibility
* API compatibility
* database compatibility
* migration compatibility

Only include relevant requirements.

---

# Acceptance Criteria

Define objectively verifiable outcomes.

Each criterion must be:

* testable
* observable
* unambiguous

Format:

AC-001
AC-002
AC-003

Example:

AC-001: Soft-deleted users are excluded from all active user search results.

AC-002: Existing user creation workflows continue to function without modification.

---

# Testing Requirements

Describe the required validation coverage.

Include applicable testing categories:

* unit tests
* integration tests
* API tests
* UI tests
* end-to-end tests

Describe what must be verified.

Do not describe implementation details.

---

# Deployment and Migration Considerations

Describe:

* deployment requirements
* rollout constraints
* migration expectations
* operational concerns

Only include relevant considerations.

---

# Risks and Constraints

Document significant technical risks.

For each include:

## Risk

Description.

## Impact

Potential consequence.

## Mitigation

Recommended mitigation.

Also document hard constraints that implementation must respect.

---

# Open Assumptions

List assumptions derived from the repository context and discovery answers.

Only include assumptions necessary to understand the specification.

Do not introduce new decisions.

---

# Specification Summary

Provide a concise summary of:

* scope
* key requirements
* technical impact
* acceptance expectations

The summary should allow a reader to quickly understand the specification without reading the full document.
