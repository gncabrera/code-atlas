# Code Atlas

Internal web application to transform raw implementation requests into deterministic, implementation-ready prompts, then send those prompts to frontier coding models.

## Why This Project Exists

Teams often lose time and tokens because raw requests are:
- incomplete,
- inconsistent in structure,
- hard to validate before model execution.

Code Atlas exists to fix that.

Project goal:
- convert noisy requests into structured prompts,
- keep prompt generation deterministic (template-based, no hidden AI rewrite),
- reduce unnecessary token spend,
- make model calls predictable and auditable.

## Core Principles

- Deterministic prompt building (`buildPreview`) from external templates.
- Exact prompt sending (`sendToModel`) with zero backend rewrite.
- Lightweight token control (`estimatedTokens = characters / 4`).
- Clean architecture: Controller -> Service -> Repository.
- Hybrid app: Thymeleaf UI + REST API.
- Minimal dependencies, simple extensible design.

## Tech Stack

Backend:
- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Flyway
- SQLite
- Lombok
- Google GenAI Java SDK (Gemini)

Frontend:
- Thymeleaf
- jQuery
- Bootstrap + Bootswatch Brite theme

## Implemented Features

### 1) Prompt Optimizer

Page: `/prompt-optimizer`

- **Your Prompt (UserRequest)** textarea.
- **Build Preview** button (deterministic template concatenation).
- **AIModelPrompt** editable textarea.
- **AI Model selector** (enabled models only).
- **Token estimation** display and validation.
- **Send To AIModel** with confirmation alert.
- **OutputPrompt** editable textarea + copy button.

Behavior:
- `buildPreview`: replaces template vars (`USER_REQUEST`, `CONTEXT`, `AGENTS_FILE`).
- `sendToModel`: sends exact textarea content as-is.
- sending blocked when estimated tokens exceed selected model `tokensPerMinute`.

### 2) Projects CRUD

Page: `/projects`  
API base: `/api/projects`

Entity fields:
- `id`
- `path`
- `name`
- `description`
- `useAgentsFile`

Rules:
- path required,
- path must exist on disk,
- `useAgentsFile` controls AGENTS.md behavior for that project.

### 3) AI Models CRUD

Page: `/ai-models`  
API base: `/api/ai-models`

Entity fields:
- `id`
- `name`
- `enabled`
- `tokensPerMinute`
- `requestsPerMinute` (informational)
- `requestsPerDay` (informational)
- `apiKey` (stored plaintext in SQLite by design)

Rules:
- duplicates API keys allowed,
- only `tokensPerMinute` enforced.

### 4) Prompt History Persistence

Prompt requests/responses stored in SQLite (`prompt_history`) with:
- project/model relation,
- prompt mode,
- token estimate,
- status (`PENDING`, `SUCCESS`, `ERROR`),
- error message,
- timestamp.

### 5) Prompt Modes Ready (No CRUD Yet)

Prepared modes:
- cheap
- balanced
- architect
- implementation
- reviewer
- refactor
- security

Templates are external and editable:
- `prompts/cheap.md`
- `prompts/balanced.md`
- `prompts/architect.md`
- `prompts/implementation.md`
- `prompts/reviewer.md`
- `prompts/refactor.md`
- `prompts/security.md`

## AGENTS.md Integration

When building preview:
- if `shouldSendAgentsFile = true` and selected project has `useAgentsFile = true`,
- backend tries reading `<Project.path>/AGENTS.md`.

If missing:
- uses fallback text: `No AGENTS.md found`.

## API Endpoints (Main)

Prompt:
- `GET /api/prompts/metadata`
- `POST /api/prompts/build-preview`
- `POST /api/prompts/send`

Projects:
- `GET /api/projects`
- `GET /api/projects/{id}`
- `POST /api/projects`
- `PUT /api/projects/{id}`
- `DELETE /api/projects/{id}`

AI Models:
- `GET /api/ai-models?enabledOnly=true|false`
- `GET /api/ai-models/{id}`
- `POST /api/ai-models`
- `PUT /api/ai-models/{id}`
- `DELETE /api/ai-models/{id}`

## Architecture Snapshot

```
Browser (Thymeleaf + jQuery)
   -> Thymeleaf Page Controllers (views)
   -> REST Controllers (JSON API)
      -> Services (business logic)
         -> Repositories (JPA)
            -> SQLite (Flyway-managed schema)
```

## Database

Flyway migration:
- `src/main/resources/db/migration/V1__init_schema.sql`

Tables:
- `projects`
- `ai_models`
- `prompt_history`
- `flyway_schema_history`

## Run Locally

### Requirements

- JDK 21
- Maven 3.9+

### Start

```bash
mvn -DskipTests spring-boot:run
```

Open:
- `http://localhost:8080/prompt-optimizer`

## Configuration

Main config file:
- `src/main/resources/application.properties`

Current defaults:
- SQLite file: `./code-atlas.db`
- server port: `8080`
- Flyway enabled
- Gemini timeout: `codeatlas.gemini.timeout-seconds=60`

## Troubleshooting

### Error: `KotlinModule not found`

Cause:
- Jackson module auto-loading expects Kotlin module when using current dependency graph.

Fix:
- include `com.fasterxml.jackson.module:jackson-module-kotlin` (already added).

### JPA schema validation mismatch with SQLite INTEGER/BIGINT

Cause:
- strict Hibernate validation conflicts with SQLite type behavior.

Fix:
- use Flyway as schema authority,
- keep `spring.jpa.hibernate.ddl-auto=none` (already set).

## Security Notes

- Internal app; no authentication by project decision.
- API keys stored plaintext intentionally for this internal MVP.
- Avoid exposing this app publicly without adding auth and secret management.

## Current Scope and Non-Goals

In scope:
- deterministic preview,
- direct Gemini send,
- CRUD for Projects/AI Models,
- token-per-minute guard.

Not in scope yet:
- auth/roles,
- real tokenizer integration,
- prompt mode CRUD,
- automated tests.

## GitHub Actions Release

Workflow file:
- `.github/workflows/release.yml`

### What it does

- Trigger on push tag `v*` (example `v1.2.3`).
- Build distribution in matrix:
  - `ubuntu-latest` -> `code-atlas-bin-linux.zip`, `code-atlas-update-linux.zip`
  - `windows-latest` -> `code-atlas-bin-windows.zip`, `code-atlas-update-windows.zip`
  - `macos-latest` -> `code-atlas-bin-macos.zip`, `code-atlas-update-macos.zip`
- Create GitHub Release with same tag and attach all zip assets.
- Release policy: fail if release already exists for tag.

### Required setup (one time)

1. Repository -> Settings -> Actions -> General.
2. In **Workflow permissions**, set **Read and write permissions**.
3. Ensure Actions are enabled for repository.

No extra PAT secret needed; workflow uses built-in `GITHUB_TOKEN`.

### Execute release

1. Commit and push changes to default branch.
2. Create tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

3. Open Actions tab -> workflow `Release`.
4. Wait for `build` matrix and `publish` jobs to finish.
5. Open GitHub Releases and verify attached 6 zip files (3 bin + 3 update).

### Re-run behavior

- If same tag already has release, workflow fails by design.
- To retry with same version:
  - delete existing GitHub Release and tag, then push tag again, or
  - bump version tag (`v1.0.1`) and push new tag.