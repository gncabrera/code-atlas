# Indexed context architecture

End-to-end flow that turns a user request into a structured context block for downstream prompt optimization. Orchestrated by `IndexedContextService`.

**Prerequisites:** see [PROJECT_INDEX.md](./PROJECT_INDEX.md) for the two indices and their builders.

---

## Engine modules

Four engines replace the earlier flat step list. Graph Expansion is removed; retrieval runs twice instead (initial + missing-context pass).

```
Intent Engine     = Intent extraction
       ↓
Context Engine    = Deterministic retrieval + second retrieval
       ↓
Knowledge Engine  = Missing context detection + architecture summary
       ↓
Prompt Engine     = Prompt assembly (deterministic template)
```

| Engine | Classes | Model |
| --- | --- | --- |
| Intent Engine | `IntentEngine` → `IntentExtractionService` | AI (`AIModelService.sendToModel`) |
| Context Engine | `ContextEngine` → `DeterministicRetriever` | Code (index search) |
| Knowledge Engine | `KnowlegeEngine` → `MissingContextDetector`, `ArchitectureSummarizer` | AI |
| Prompt Engine | `PromptEngine` → `PromptBuilder` | Code (template only) |

---

## Pipeline steps

Eight logged steps (`TOTAL_STEPS = 8` in `IndexedContextService`).

```
1. Ensure indices fresh          → ProjectIndexService
2. Extract intent                → IntentEngine
3. Deterministic retrieval       → ContextEngine
4. Detect missing context        → KnowlegeEngine
5. Second deterministic retrieval → ContextEngine
6. Merge second retrieval files   → mergeFiles()
7. Summarize architecture         → KnowlegeEngine
8. Assemble context               → PromptEngine
```

---

### Step 1 — Ensure indices fresh

**Service:** `ProjectIndexService`  
**Model:** Code  

Refreshes `project_file_index` when stale (`isStale` / `refreshIndex`). Does not regenerate AI metadata.

| Index | Role |
| --- | --- |
| Project File Index | **Primary** |
| Project File Metadata Index | — |

---

### Step 2 — Extract intent

**Service:** `IntentEngine` → `IntentExtractionService`  
**Prompt template:** `src/main/resources/prompts/context/intent-extraction.md`  
**Model:** AI  

**Input:** natural-language user request (+ project `AGENTS` / design context from prompt placeholders).

**Output:** `Intent` record:

```json
{
  "action": "modify",
  "entities": ["User"],
  "operations": ["soft delete"],
  "layers": ["controller", "service", "repository", "entity", "migration"],
  "frontendImpact": true
}
```

Converts human language into structured search parameters.

**Prompt placeholders**

| Placeholder | Wired in `IntentExtractionService` |
| --- | --- |
| `{{USER_REQUEST}}` | Yes |
| `{{AGENTS_FILE}}` | No (template only) |
| `{{DESIGN_FILE}}` | No (template only) |

**Prompt**

```text
Extract structured intent from the user request.

User request:
{{USER_REQUEST}}

Return exactly one JSON object. No markdown fences or prose.

Schema:
{
  "action": "create|modify|delete|investigate",
  "entities": ["EntityName"],
  "operations": ["operation phrase"],
  "layers": ["controller","service","repository","entity","migration","frontend"],
  "frontendImpact": true
}

Rules:
- entities are PascalCase class or domain names when identifiable
- layers reflect likely touch points for the change
- include migration in layers when schema, database, columns, tables, flyway, liquibase, sql, or persistence changes are implied
- include frontend in layers when UI, templates, html, js, css, pages, or client behavior may change
- frontendImpact true when UI or API consumers may change

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}
```

| Index | Role |
| --- | --- |
| Project File Index | — |
| Project File Metadata Index | — |

---

### Step 3 — Deterministic retrieval

**Service:** `ContextEngine` → `DeterministicRetriever`  
**Model:** Code (no AI)  

**Input:** `Intent` from step 2.

**Output:** `ContextResult` with `List<RetrievedFile>` (path, score, reasons, symbols, snippet).

**Target behavior (not implemented yet):** search `project_file_index` for candidate paths and `project_file_metadata_index` for semantic matches (symbols, keywords, concepts, dependencies, `searchText`). Rank, cap by `codeatlas.context.indexed.max-files`, attach snippets from disk.

**Current behavior:** `DeterministicRetriever.retrieve` returns an empty file list (stub).

| Index | Role |
| --- | --- |
| Project File Index | **Target — primary** |
| Project File Metadata Index | **Target — primary** |

---

### Step 4 — Detect missing context

**Service:** `KnowlegeEngine` → `MissingContextDetector`  
**Prompt template:** `src/main/resources/prompts/context/missing-context.md`  
**Model:** AI  

**Input:** user request + original `Intent` + files from step 3.

**Output (target):** refined `Intent` or follow-up search terms derived from missing categories.

**Prompt response shape:**

```json
{
  "missing": ["migration", "frontend"]
}
```

Categories are short labels: `migration`, `frontend`, `repository`, `entity`, `dto`, `test`, `config`. Empty array when context looks sufficient.

**Current behavior:** detector parses the model response but returns `null` (TODO — map `missing` → retrieval `Intent`).

**Prompt placeholders**

| Placeholder | Wired in `MissingContextDetector` |
| --- | --- |
| `{{USER_REQUEST}}` | Yes |
| `{{INTENT}}` | Yes |
| `{{RETRIEVED_FILES}}` | Yes (paths only) |
| `{{PATTERN_HINTS}}` | No (template only) |
| `{{AGENTS_FILE}}` | No (template only) |
| `{{DESIGN_FILE}}` | No (template only) |

**Prompt**

```text
Detect missing context categories needed to implement the user request.

User request:
{{USER_REQUEST}}

Intent:
{{INTENT}}

Retrieved files:
{{RETRIEVED_FILES}}

Pattern hints:
{{PATTERN_HINTS}}

Return exactly one JSON object. No markdown fences or prose.

Schema:
{
  "missing": ["migration","frontend caller","repository","entity","test"]
}

Rules:
- use short canonical category labels only: migration, frontend, repository, entity, dto, test, config
- missing is empty when retrieved files appear sufficient
- common categories: migration, frontend, repository, entity, dto, test, config

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}
```

| Index | Role |
| --- | --- |
| Project File Index | — |
| Project File Metadata Index | Secondary (target — summaries in prompt) |

---

### Step 5 — Second deterministic retrieval

**Service:** `ContextEngine` → `DeterministicRetriever`  
**Model:** Code (no AI)  

**Input:** `Intent` from step 4 (missing-context search intent).

**Output:** `ContextResult` with additional `RetrievedFile` candidates.

Same retriever as step 3, driven by the missing-context `Intent`.

**Target behavior (not implemented yet):** same index search as step 3, scoped to gaps reported in step 4.

**Current behavior:** stub — returns an empty file list.

| Index | Role |
| --- | --- |
| Project File Index | **Target — primary** |
| Project File Metadata Index | **Target — primary** |

---

### Step 6 — Merge second retrieval files

**Service:** `IndexedContextService.mergeFiles`  
**Model:** Code  

Merges files from step 3 and step 5. Deduplicates by `relativePath`; primary list wins on conflict.

| Index | Role |
| --- | --- |
| Project File Index | — (operates on in-memory `RetrievedFile` lists) |
| Project File Metadata Index | — |

---

### Step 7 — Summarize architecture

**Service:** `KnowlegeEngine` → `ArchitectureSummarizer`  
**Prompt template:** `src/main/resources/prompts/context/architecture-summary.md`  
**Model:** AI  

**Input:** user request + `Intent` + merged `RetrievedFile` list.

**Output:** prose architecture facts, e.g.:

```text
Current pattern:
- Controllers delegate to Services
- Services own business logic
- Repositories use Spring Data JPA
- User entity maps USERS table
- No soft delete mechanism exists
- Migrations managed by Flyway
```

Gives the downstream model explicit facts instead of raw code only.

**Target:** inject per-file summaries from `project_file_metadata_index` into the prompt (`formatSummaries` — TODO).

**Prompt placeholders**

| Placeholder | Wired in `ArchitectureSummarizer` |
| --- | --- |
| `{{USER_REQUEST}}` | Yes |
| `{{INTENT}}` | Yes |
| `{{FILE_SUMMARIES}}` | Yes (currently empty — TODO) |
| `{{RETRIEVED_FILES}}` | Yes |
| `{{AGENTS_FILE}}` | No (template only) |
| `{{DESIGN_FILE}}` | No (template only) |

**Prompt**

```text
Summarize architecture facts for the implementation model.

User request:
{{USER_REQUEST}}

Intent:
{{INTENT}}

File summaries (offline index):
{{FILE_SUMMARIES}}

Retrieved files (paths, retrieval signals, and code snippets):
{{RETRIEVED_FILES}}

Produce plain text (not JSON) starting with "Current pattern:" followed by bullet facts.
Rules:
- Use offline file summaries and retrieved snippets together; state explicit facts only
- Do not invent files, classes, or behavior not present in the inputs
- Mention layering, persistence, and gaps relevant to the request
- Keep under 20 bullet lines

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}
```

| Index | Role |
| --- | --- |
| Project File Index | — |
| Project File Metadata Index | **Target — primary** |

---

### Step 8 — Assemble context

**Service:** `PromptEngine` → `PromptBuilder`  
**Model:** Code (pure template, no AI prompt file)  

**Input:** `Intent` + `KnowledgeResult` (architecture facts + merged files).

**Output sections:**

```text
# User Request Context
# Architecture Facts
# Relevant Files
# Code Snippets
```

Deterministic formatting only. Size limiting via `codeatlas.context.indexed.max-total-chars` exists but is currently disabled in code.

| Index | Role |
| --- | --- |
| Project File Index | — |
| Project File Metadata Index | — |

---

## Index summary

| Index | Primary steps |
| --- | --- |
| Project File Index | Step 1 (refresh); steps 3 & 5 (retrieval — target) |
| Project File Metadata Index | Steps 3, 4, 5, 7 (search & summaries — target) |

---

## Implementation status

| Component | Status |
| --- | --- |
| File index scan / staleness | Implemented |
| AI metadata generation (`FileSummariesIndexService`) | Implemented |
| Intent extraction | Implemented |
| Deterministic retrieval | **Stub** — returns empty list |
| Missing context → retrieval intent | **Stub** — returns `null` |
| Architecture summary metadata lookup | **Partial** — `formatSummaries` TODO |
| Prompt assembly | Implemented |
