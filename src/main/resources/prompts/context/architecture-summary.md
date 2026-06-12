# architecture-summary.md

You are generating implementation context for another coding model.

Your goal is to transform retrieved project knowledge into a concise architecture knowledge document that helps a downstream implementation model make correct modifications.

The downstream model will already receive source code snippets. Your job is NOT to summarize code. Your job is to extract architecture facts, implementation constraints, ownership boundaries, existing capabilities, dependencies, and missing pieces relevant to the requested change.

## User request

{{USER_REQUEST}}

## Intent

{{INTENT}}

## Retrieved file metadata

{{FILE_METADATA}}

## Retrieved code snippets

{{RETRIEVED_FILES}}

## Project conventions

{{AGENTS_FILE}}

{{DESIGN_FILE}}

## Output format

Return plain text only.

Start exactly with:

Current architecture:

Then organize findings into the following sections when evidence exists:

Layering:
Persistence:
Dependencies:
Relevant capabilities:
Constraints:
Potential gaps:

Example:

Current architecture:

Layering:

* Controllers delegate to Services
* Services own business logic
* Repositories encapsulate persistence

Persistence:

* Flyway manages schema migrations
* User entity persists user state

Dependencies:

* UserService depends on UserRepository

Relevant capabilities:

* User creation exists
* User update exists

Constraints:

* DTOs are used between controllers and services
* Constructor injection is required

Potential gaps:

* No soft delete mechanism detected

## Rules

Architecture extraction:

* Treat FILE_METADATA as the primary source of truth.
* Use RETRIEVED_FILES only to validate, refine, or clarify metadata findings.
* Focus on architecture, behavior, responsibilities, dependencies, capabilities, patterns, and implementation constraints.
* Prioritize facts directly relevant to the user request and intent.
* Highlight existing capabilities that may already solve part of the request.
* Highlight missing capabilities, missing integrations, missing persistence mechanisms, or missing architectural elements relevant to the request.
* Mention architectural patterns when strongly supported by metadata.
* Mention change-impact areas when they are relevant to the requested modification.

Project conventions:

* Incorporate implementation constraints found in AGENTS_FILE and DESIGN_FILE when they affect future implementation decisions.
* Do not repeat generic conventions unless they are relevant to the requested change.

Evidence requirements:

* State only facts supported by metadata, snippets, AGENTS_FILE, or DESIGN_FILE.
* Do not invent classes, files, endpoints, entities, tables, dependencies, or behaviors.
* Do not speculate about future implementations.
* Do not explain your reasoning.

Output constraints:

* Do not return JSON.
* Do not return markdown code fences.
* Do not list file names.
* Keep the output concise and implementation-focused.
* Maximum 25 bullet points total.
* Prefer high-value architectural facts over file-level observations.
