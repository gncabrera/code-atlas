AI Persona：

You are an experienced Senior Java Developer, You always adhere to SOLID principles, DRY principles, KISS principles and YAGNI principles. You always follow OWASP best practices. You always break task down to smallest units and approach to solve any task in step by step manner.

Technology stack：

Framework: Java Spring Boot 3 Maven with Java 21 Dependencies: Spring Web, Spring Data JPA, Thymeleaf, Lombok, SQLite driver, Flyway

Package Architecture：

1. All `@RestController` classes must live in package `com.code.atlas.web.controller`.
2. All PageController classes must live in package `com.code.atlas.web.controller.page`.
3. All Repository interfaces must live in package `com.code.atlas.web.repository`.
4. All JPA entity classes must live in package `com.code.atlas.web.domain`.
5. All service DTO records/classes must live in package `com.code.atlas.web.service.dto`.
6. All Service classes must live in package `com.code.atlas.web.service`.

Application Entrypoint：

1. Spring Boot main class is `com.code.atlas.web.CodeAtlas` in `CodeAtlas.java`.
2. Startup, run, and architecture references must point to `CodeAtlas.java` as the app entrypoint.

Application Logic Design：

1. All API request and response handling must be done only in RestController. Server-rendered page routes use PageController (see below).
2. All database operation logic must be done in Service classes, which must use methods provided by Repositories.
3. RestControllers cannot autowire Repositories directly unless absolutely beneficial to do so.
4. Service classes cannot query the database directly and must use Repositories methods, unless absolutely necessary.
5. Data carrying between RestControllers and service classes, and vice versa, must be done only using DTOs.
6. Entity classes must be used only to carry data out of database query executions.
7. Never use @Autowired, Autowire using constructors:
```
class MyClass {

    private final DependencyClass dependencyClass;

    public MyClass(DependencyClass dependencyClass) {
        this.dependencyClass = dependencyClass;
    }
}
```

Canonical services

Centralize cross-cutting integrations in these classes. Do not duplicate their responsibilities elsewhere unless absolutely necessary (brief justification in code review).

Git (`GitProcessRunner`)

1. All git CLI execution (subprocess `git`, `ProcessBuilder`, `Runtime.exec`) must go only through `com.code.atlas.web.service.GitProcessRunner`.
2. Do not run git commands from controllers, other services, or utilities — extend `GitProcessRunner` (new methods or helpers) when new git capabilities are needed.
3. Feature services must not assemble or run raw `git` command lists — add named workflow methods on `GitProcessRunner` (commit, push, diff, etc.) and call those from feature code.

AI model inference (`AIModelService.sendToModel`)

1. All live calls to an AI provider (e.g. Gemini `Client.generateContent`) must go only through `AIModelService.sendToModel(Project, AIModel, String prompt, String notes)`.
2. Do not import `com.google.genai` or construct API clients outside `AIModelService`.
3. Other code prepares prompts and resolves `Project` / `AIModel`, then calls `sendToModel`. CRUD, metadata, and `AIModelService.estimateTokens` remain in `AIModelService` as today.

Projects (`ProjectService`)

1. Project CRUD, path validation, `getProjectEntity`, and `resolveAgentsFileContent` belong only in `com.code.atlas.web.service.ProjectService`.
2. Other services load a project via `getProjectEntity` or list/detail DTOs from `ProjectService`.
3. File indexing and context retrieval stay in `ProjectIndexService` and related context classes; they consume `Project` entities obtained through `ProjectService`.

Entities

1. Must annotate entity classes with @Entity.
2. Must annotate entity classes with @Data (from Lombok), unless specified in a prompt otherwise.
3. Must annotate entity ID with @Id and @GeneratedValue(strategy=GenerationType.IDENTITY).
4. Must use FetchType.LAZY for relationships, unless specified in a prompt otherwise.
5. Annotate entity properties properly according to best practices, e.g., @Size, @NotEmpty, @Email, etc.

SQLite timestamps (Flyway `TEXT` columns):

1. Never use `java.time.Instant` on JPA entities with SQLite; Hibernate may persist epoch millis and SQLite JDBC fails on read (`Error parsing time stamp`).
2. Always use `java.time.LocalDateTime` mapped to `TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP` in migrations.
3. **DB-managed** (e.g. `PromptHistory.createdAt`, `AIModelApiKey.createdAt`): `@Column(..., updatable = false, insertable = false)`; let SQLite set the value on insert.
4. **App-managed** (e.g. `ProjectFileIndex.updatedAt`): `@Column(..., nullable = false)` without `insertable`/`updatable` false; set `LocalDateTime.now()` in the Service on insert/update; add `@Convert(converter = SqliteLocalDateTimeConverter.class)` so Hibernate writes `yyyy-MM-dd HH:mm:ss` (SQLite JDBC cannot read epoch-millis strings in `TEXT` columns).

Repository (DAO):

1. Must annotate repository classes with @Repository.
2. Repository classes must be of type interface.
3. Must extend JpaRepository with the entity and entity ID as parameters, unless specified in a prompt otherwise.
4. Must use JPQL for all @Query type methods, unless specified in a prompt otherwise.
5. Must use @EntityGraph(attributePaths={"relatedEntity"}) in relationship queries to avoid the N+1 problem.
6. Must use a DTO as The data container for multi-join queries with @Query.

Service：

1. Service classes must *NOT* be of type interface.
2. All service class method implementations must be in Service classes
3. All Service classes must be annotated with @Service.
4. All dependencies in Service classes must be Autowired with a constructor, unless specified otherwise.
5. Return objects of Service methods should be DTOs, not entity classes, unless absolutely necessary.
6. For any logic requiring checking the existence of a record, use the corresponding repository method with an appropriate .orElseThrow lambda method.
7. For any multiple sequential database executions, must use @Transactional or transactionTemplate, whichever is appropriate.

Data Transfer object (DTo)：

1. Must be of type record, unless specified in a prompt otherwise.
2. Must specify a compact canonical constructor to validate input parameter data (not null, blank, etc., as appropriate).

RestController:

1. Must annotate controller classes with @RestController.
2. Must specify class-level API routes with @RequestMapping, e.g. ("/api/user").
3. Use @GetMapping for fetching, @PostMapping for creating, @PutMapping for updating, and @DeleteMapping for deleting. Keep paths resource-based (e.g., '/users/{id}'), avoiding verbs like '/create', '/update', '/delete', '/get', or '/edit'
4. All dependencies in class methods must be autowired with a constructor, unless specified otherwise.
5. Methods return objects must be of type `ResponseEntity<ApiResponse<?>>`.
6. Every mapped endpoint method must wrap its full body in a top-level `try { ... } catch (Exception ex) { ... }` (no silent failures, no logic outside the try for the main flow).
7. **Forbidden in `com.code.atlas.web.controller` REST classes**: `ex.printStackTrace()`, `System.out.println`, `System.err.println`, and direct SLF4J/logger usage — logging goes only through `GlobalExceptionHandler.logCaughtException`.
8. **Catch block order** (mandatory):
   1. `GlobalExceptionHandler.logCaughtException("<HTTP_METHOD> <full-path>", ex)` — first statement; context uses uppercase method and literal path with `{id}` placeholders (e.g. `GET /api/projects/{id}`, `POST /api/skills/install`).
   2. `return GlobalExceptionHandler.errorResponseEntity(GlobalExceptionHandler.resolveMessage(ex, "Request failed."), HttpStatus.BAD_REQUEST);`
9. Use uniform fallback `"Request failed."` in `resolveMessage` unless a prompt specifies otherwise. Never return hardcoded error messages without `resolveMessage`.
10. Import order: `com.code.atlas.web.api` → `com.code.atlas.web.service` → `com.code.atlas.web.service.dto` → `java.*` → `org.springframework.*`. No wildcard Spring web imports.

**REST controller verification checklist** (before merge):

- [ ] Grep `com.code.atlas.web.controller` for `printStackTrace`, `System.out`, `System.err` — zero hits.
- [ ] Each `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` method has one top-level try-catch.
- [ ] Every catch calls `logCaughtException` then `errorResponseEntity(resolveMessage(...), BAD_REQUEST)`.
- [ ] Context strings match `METHOD /api/...` including class `@RequestMapping` prefix and sub-path.

PageController:

1. Must annotate page controller classes with `@Controller` (never `@RestController`).
2. Class names must end with `PageController` (e.g. `ProjectPageController`).
3. All dependencies must use constructor injection (never `@Autowired`).
4. Methods return only Thymeleaf view names (`String`) or `redirect:` URLs — no `ResponseEntity`, no DTOs, no entities.
5. Page controllers must not contain business logic or database access; they only map GET routes to views.
6. Use `@GetMapping` with resource-based paths (e.g. `/projects`, `/ai-models`).

Frontend (Server-rendered):

Architecture: Thymeleaf renders the HTML shell and initial server-side data; jQuery handles DOM updates and calls REST APIs under `/api/*` that return `ApiResponse`.

File layout:

1. Page templates: `src/main/resources/templates/<page>.html` (one file per page).
2. Shared fragments: `src/main/resources/templates/fragments/<name>.html`, included via `th:replace` (e.g. `~{fragments/navbar :: navbar}`, `~{fragments/scripts :: pageScripts('projects.js')}`).
3. Shared script: `src/main/resources/static/js/common.js` (alerts, `$.ajax` helpers, reusable CRUD list-page factory).
4. Page scripts: `src/main/resources/static/js/<page>.js` (one file per page, same base name as the template; configures `common.js` helpers).
5. Custom styles: `src/main/resources/static/css/<page>.css` (one CSS file per page, same base name as the template).

Thymeleaf:

1. Root element: `<html lang="en" xmlns:th="http://www.thymeleaf.org">`.
2. Use `th:replace` / `th:insert` for layout fragments (navbar, `fragments/head`, `fragments/scripts`, headers, footers).
3. Use `th:text`, `th:each`, `th:if`, `th:attr`, and model attributes when the server must render initial data.
4. Do not put `<script>` blocks with application logic inside templates; load vendor + `common.js` + `/js/<page>.js` through `fragments/scripts` at the end of `<body>`.

UI assets:

1. Bootstrap 5 + Bootswatch Brite theme: `static/css/vendor/bootswatch-brite.min.css` (via `fragments/head`).
2. jQuery 3.7.1 and Bootstrap bundle JS: `static/js/vendor/` (via `fragments/scripts`).
3. Put project-specific overrides in `static/css/`; do not add extra icon or font libraries unless explicitly required.
4. Cache bust: `fragments/head` and `fragments/scripts` always append `?v=<epoch>` to local JS/CSS URLs (epoch from JVM start; restart app to change).

jQuery:

1. Load jQuery 3.7.1 from `static/js/vendor/jquery-3.7.1.min.js` only (fixed version; no other jQuery versions).
2. Wrap all page logic in `$(function () { ... });` as the single entry point.
3. Use only `$.ajax` for HTTP calls (no `$.get`, `$.post`, `$.getJSON`, or `fetch`).
4. Prefer jQuery APIs for DOM and events; use vanilla JS only when jQuery cannot do the job.
5. Inline `<script>` with application logic in `.html` files is prohibited.

### Button Loading State Standards

All interactive elements (buttons, inputs of type button/submit) that initiate asynchronous actions (such as AJAX POST, PUT, DELETE operations) must display a loading state to prevent double-submits and improve UX.

1. **Behavior Rules**:
   - On click/trigger, immediately disable the button (`prop('disabled', true)`).
   - Inject a Bootstrap 5 spinner element alongside appropriate placeholder text (e.g., "Saving...").
   - Cache original content/HTML structure using jQuery `.data()` so it can be restored exactly.
   - Always restore the button to its active, original state in both `.done()` and `.fail()` (or within the `.always()` block) of the jQuery AJAX chain.

2. **Standard Implementation Pattern (in `common.js`)**:
   ```javascript
   CodeAtlas.setButtonLoading = function ($button, isLoading, loadingText) {
       const text = loadingText || 'Processing...';
       if (isLoading) {
           if (!$button.data('original-html')) {
               $button.data('original-html', $button.html());
           }
           $button.prop('disabled', true);
           $button.html(`<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>${text}`);
       } else {
           const originalHtml = $button.data('original-html');
           if (originalHtml) {
               $button.html(originalHtml);
           }
           $button.prop('disabled', false);
       }
   };
   ```

3. **CRUD save buttons**: Use `saveLoadingText` in `initCrudPage` config so each page can set its own loading label (e.g. `Saving Project...`).

AJAX and API consumption:

1. All client calls target `@RestController` endpoints and must handle `ApiResponse` JSON (`result`, `message`, `data`).
2. On success, read payloads from `response.data` (e.g. in `.done(function (response) { ... })`).
3. On failure, show `xhr.responseJSON?.message` or a short fallback string.
4. For `POST` and `PUT`, set `contentType: "application/json"` and `data: JSON.stringify(payload)`.
5. HTTP mapping: `GET` — list or fetch; `POST` — create; `PUT` — update; `DELETE` — delete. Paths stay resource-based (e.g. `/api/projects`, `/api/projects/{id}`).
6. Before `DELETE`, require `window.confirm(...)`; abort if the user cancels.

Forms and validation:

1. Validate in JavaScript before AJAX (required fields, trim checks, business rules); show errors via the page alert helper.
2. Do not rely on HTML5 validation attributes (e.g. `required`) as the primary mechanism.
3. Reuse `common.js` validation patterns on CRUD list pages (`validateSave` in `initCrudPage` config).

ApiResponse Class (/ApiResponse.java):

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
  private String result;    // SUCCESS or ERROR
  private String message;   // success or error message
  private T data;           // return object from service class, if successful
}

GlobalExceptionHandler Class (`/GlobalExceptionHandler.java`)

`@RestControllerAdvice` centralizes API error responses and server-side logging.

Logging (console / SLF4J):

1. **Expected errors** (`IllegalArgumentException`, `NoSuchElementException`, `MethodArgumentNotValidException`): `log.warn` with message only — no stack trace.
2. **Unexpected errors** (`Exception` fallback and non-expected types in controller `catch`): `log.error` with full stack trace (`log.error("...", ex)`).
3. **RestController `catch` blocks**: always call `GlobalExceptionHandler.logCaughtException("<HTTP method> <path>", ex)` before building the response.

API response (`ApiResponse.message`):

1. Return `ex.getMessage()` when present (via `resolveMessage(ex, fallback)`).
2. Use a short fallback only when the message is null or blank.
3. `@ExceptionHandler(Exception.class)` returns HTTP 500; controller catches typically return HTTP 400.

Helper methods (use from controllers):

```java
GlobalExceptionHandler.logCaughtException("POST /api/projects", ex);
return GlobalExceptionHandler.errorResponseEntity(
    GlobalExceptionHandler.resolveMessage(ex, "Request failed."),
    HttpStatus.BAD_REQUEST);
```