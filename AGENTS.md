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

Prompt template substitution (`PromptFormatService.formatPrompt`)

1. All `{{…}}` placeholder substitution in prompt templates must go only through `com.code.atlas.web.service.PromptFormatService.formatPrompt(String template, Map<String, String> parameters)`.
2. Do not use `String.replace` on `{{KEY}}` literals in feature services — inject `PromptFormatService` and pass a parameter map (e.g. `USER_REQUEST`, `CONTEXT`, `AGENTS_FILE`, `DIFF`).
3. Placeholders match `\{\{\s*KEY\s*\}\}` (flexible whitespace); unknown placeholders stay literal; map values null become empty string.

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

Frontend UI rules moved to `DESIGN.md`.
Use `DESIGN.md` as single source for template/js/css constraints, AJAX/UI behavior, loading state, and dismissible alert standards.

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