# Code Atlas UI Design Constraints (Model Reference)

## 0) Migration Note

- Moved from `AGENTS.md` to `DESIGN.md` on 2026-05-26.
- Reason: isolate frontend design constraints into one dedicated, model-friendly reference.
- `AGENTS.md` now keeps architecture/backend/API/cross-cutting constraints; `DESIGN.md` owns UI/frontend constraints.

## 1) Scope and Purpose

This document defines frontend design and implementation constraints for server-rendered pages in Code Atlas.

Use this document as primary reference when generating or reviewing UI code for:

- Thymeleaf templates under `src/main/resources/templates/`
- Shared template fragments under `src/main/resources/templates/fragments/`
- Page scripts under `src/main/resources/static/js/`
- Page styles under `src/main/resources/static/css/`
- REST API consumption from frontend (`/api/*`)

Out of scope: backend package structure and non-UI architecture rules.

## 2) Visual Theme and Baseline

- Theme: Bootswatch Brite (Bootstrap 5.3) via `static/css/vendor/bootswatch-brite.min.css`.
- Reference: [Bootswatch Brite](https://bootswatch.com/brite/).
- Shared base fragments: `fragments/head.html`, `fragments/navbar.html`, `fragments/scripts.html`.
- Keep visual language consistent with:
  - Good step-by-step flow: `templates/prompt-optimizer.html`
  - Good CRUD layout: `templates/projects.html`

Do not use `templates/code-review.html` as layout/style reference.

## 3) Non-Negotiable Rules (MUST)

### 3.1 Template Structure

- Root tag MUST be `<html lang="en" xmlns:th="http://www.thymeleaf.org">`.
- File layout MUST follow:
  - page templates: `src/main/resources/templates/<page>.html`
  - shared fragments: `src/main/resources/templates/fragments/<name>.html`
  - shared script helpers: `src/main/resources/static/js/common.js`
  - page scripts: `src/main/resources/static/js/<page>.js`
  - page styles: `src/main/resources/static/css/<page>.css`
- Every page MUST include:
  - `fragments/head :: pageHead('<page>.css')`
  - `fragments/navbar :: navbar`
  - `fragments/scripts :: pageScripts('<page>.js')` or `pageScriptsWithVendor(...)` when extra vendor JS required.
- One page = one template file (`<page>.html`) + one page script (`<page>.js`) + one page css (`<page>.css`).
- Thymeleaf server-rendered bindings MUST use standard attributes (`th:text`, `th:each`, `th:if`, `th:attr`) when initial data render required.

### 3.2 JavaScript Execution Model

- All page logic MUST start in single `$(function () { ... });`.
- HTTP calls MUST use `$.ajax` only. Never `fetch`, `$.get`, `$.post`, `$.getJSON`.
- Prefer jQuery APIs for DOM/event work. Use vanilla JS only when jQuery cannot satisfy need.
- Put application logic in page JS files, not inline `<script>` in templates.
- Reuse `common.js` helpers before creating new utility logic.

### 3.3 API Contract Usage

- Client MUST consume REST endpoints under `/api/*` returning `ApiResponse`.
- Read success payload from `response.data`.
- Error messaging MUST use `xhr.responseJSON?.message` with short fallback.
- `POST`/`PUT` MUST send JSON:
  - `contentType: "application/json"`
  - `data: JSON.stringify(payload)`
- HTTP semantics:
  - `GET` list/fetch
  - `POST` create
  - `PUT` update
  - `DELETE` delete
- `DELETE` actions MUST require `window.confirm(...)` before request.

### 3.4 Loading and Async Safety

- Async action buttons MUST show loading state.
- During async run:
  - disable button
  - show spinner + loading text
  - preserve original html in jQuery `.data(...)`
- After completion/failure:
  - restore exact original html
  - re-enable button
- Preferred helper: `CodeAtlas.setButtonLoading(...)`.
- Required helper behavior for `CodeAtlas.setButtonLoading(...)`:
  - cache original html with jQuery `.data('original-html', ...)` once
  - set disabled state via `$button.prop('disabled', true/false)`
  - render spinner markup using Bootstrap `spinner-border spinner-border-sm`
  - restore original html in both success and failure paths (`.always(...)` recommended)
- CRUD save flows using `CodeAtlas.initCrudPage(...)` MUST provide `saveLoadingText` (`Saving Project...`, etc.).

### 3.5 Dismissible Informational Alerts

- Static page hints/warnings MUST use dismissible Bootstrap alerts.
- Persistent dismissal MUST use stable `data-alert-id` (example: `projects.git-repo-hint`).
- Non-persistent contextual alerts MUST use `data-alert-persist="false"`.
- Dismissible lifecycle handled by `dismissible-alerts.js` + `CodeAtlas.initDismissibleAlerts()`.
- Standard markup MUST include `alert-dismissible fade show`, close button `data-bs-dismiss="alert"`, and `role="alert"`.
- Persistence storage key is `codeAtlas.dismissedAlerts` in `localStorage`.
- No-flash requirement: `fragments/head` MUST load `static/js/dismissible-alerts.js` synchronously so dismissed `[data-alert-id]` elements hide before first paint.
- Exclusions: transient toasts, AJAX validation feedback, dynamic result alerts do not use dismissible-page-alert persistence pattern.

### 3.6 Validation

- Validate form input in JS before AJAX submission.
- Do not rely on HTML5 `required` as primary validation mechanism.
- Show validation feedback using shared page alert helpers.
- CRUD list pages using `CodeAtlas.initCrudPage(...)` SHOULD implement validation in `validateSave`.

## 4) Page Composition Patterns

### 4.1 Step-by-Step Operational Screen Pattern

Use `prompt-optimizer.html` as reference when workflow is sequential and stateful.

Pattern:

1. Header + short intent text.
2. Multi-card flow, each card = one stage with clear title.
3. Top controls for context selection (project/model/mode).
4. Textareas for editable inputs and outputs.
5. Action row with primary/secondary actions and status indicator.

When to use:

- Prompt pipelines
- Input -> preview -> output generators
- Multi-stage AI-assisted flows

### 4.2 CRUD Admin Screen Pattern

Use `projects.html` as reference pattern for entity management pages.

Pattern:

1. Header + optional dismissible guidance alert.
2. `Form` card for create/update (hidden id + grouped fields).
3. Save/Reset action row (`btn-primary` + `btn-outline-secondary`).
4. `List` card with responsive striped table and actions column.
5. Checkbox/switch controls for booleans.

Corroborated by related templates:

- `ai-models.html`
- `api-keys.html`
- `admin/prompt-optimizer-modes.html`

## 5) Component-Level Conventions

- Titles: `h1.mb-3` at top of `<main class="container py-4">`.
- Use Bootstrap cards to segment concerns (`card-header` + `card-body`).
- Use responsive grid (`row g-3`, `col-md-*`, `col-12`) for forms.
- Use `.table.table-striped.align-middle` in list tables.
- Keep button semantics consistent:
  - primary save/create
  - outline secondary reset/cancel
  - warning/danger for risky ops
- External links MUST include `target="_blank"` + `rel="noopener noreferrer"`.

## 6) Asset and Fragment Constraints

- CSS/JS local assets MUST use cache-busted URLs with `v=${assetVersion}` through fragments.
- jQuery version fixed to `3.7.1` from local vendor path.
- Bootstrap bundle loaded from local vendor path.
- If extra vendor JS needed for one page, use `pageScriptsWithVendor(...)`.
- Do not add extra icon/font libraries unless explicitly required.

## 7) Decision Policy for Model-Generated UI

When model must create or modify a page:

1. Pick page type:
   - workflow/steps -> Step-by-Step pattern
   - entity management -> CRUD pattern
2. Compose page shell with standard fragments.
3. Define IDs for interactive fields/buttons first.
4. Implement page JS with:
   - `$(function () { ... })`
   - `$.ajax` only
   - `ApiResponse` handling (`response.data`)
   - loading state for async actions
   - pre-submit validation
5. Add dismissible static alerts only when page-level guidance needed.
6. Keep style in Bootstrap/Brite vocabulary; avoid custom visual system.

## 8) Prohibited Patterns

- Inline application logic scripts in `.html` page template.
- `fetch` API or jQuery shortcuts (`$.get`, `$.post`, `$.getJSON`).
- New third-party UI frameworks or icon/font packs without explicit requirement.
- Non-resource REST path verbs from frontend (`/create`, `/update`, `/edit`, etc.).

## 9) Acceptance Checklist

- [ ] Template uses standard fragments (`head`, `navbar`, `scripts`).
- [ ] Page has matching `<page>.html`, `<page>.js`, `<page>.css`.
- [ ] JS entrypoint is single `$(function () { ... });`.
- [ ] API calls use `$.ajax` and `ApiResponse` contract.
- [ ] Async buttons show/restore loading state.
- [ ] `DELETE` actions require `window.confirm(...)`.
- [ ] Form validation runs before AJAX.
- [ ] Static hints use dismissible alert standard.
- [ ] Visual layout matches Brite + existing good pages.
- [ ] No dependency on `code-review.html` pattern.
