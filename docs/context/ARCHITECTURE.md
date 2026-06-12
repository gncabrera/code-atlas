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
| Prompt Engine | `PromptEngine` → `PromptBuilder`; shared `PromptHelper` (file blocks for AI prompts) | Code (template only) |

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

**Output:** `Intent` record (search-query DTO for deterministic retrieval):

```json
{
  "action": "modify",
  "symbols": ["User"],
  "concepts": ["soft-delete"],
  "capabilities": ["delete-user"],
  "architecturalRoles": ["entity", "repository", "service", "migration"],
  "changeImpactAreas": ["database", "api"],
  "frontendImpact": false,
  "confidence": 0.92
}
```

Converts human language into structured search parameters mapped against `project_file_metadata_index`.

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
  "symbols": [],
  "concepts": [],
  "capabilities": [],
  "architecturalRoles": [],
  "changeImpactAreas": [],
  "frontendImpact": false,
  "confidence": 0.0
}

Rules:
- symbols are PascalCase class or domain names when identifiable
- concepts and capabilities use normalized kebab-case phrases
- architecturalRoles reflect likely touch points (controller, service, repository, entity, migration, frontend, …)
- changeImpactAreas list areas likely affected (database, api, frontend, security, …)
- frontendImpact true when UI or API consumers may change
- confidence is a 0.0–1.0 estimate of extraction quality (used by retrieval to widen result cap when low)

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
**Helpers:** `MetadataMatchScorer`, `TermNormalizer`, `SymbolCenteredSnippetExtractor`  
**Model:** Code (no AI, no embeddings)  

**Input:** `Intent` from step 2.

**Output:** `ContextResult` with ranked `List<RetrievedFile>` (`ProjectFileIndex` ref, `language`, `type`, `score`, `reasons`, `symbols`, `snippet`).

**Behavior:**

1. Load all `project_file_metadata_index` rows for the project (`@EntityGraph` on `file` — joins `project_file_index` for path/extension).
2. Parse each `metadata_json` once into `FileSummaryOfflineResponse.Metadata`.
3. Score every metadata row against the `Intent` via additive `MetadataMatchScorer` (see [PROJECT_INDEX.md](./PROJECT_INDEX.md#deterministic-retrieval-scoring)).
4. Discard candidates with `score <= 0`. Files without a metadata row are skipped.
5. Sort `score DESC`, then `file_path ASC` (deterministic tie-break).
6. Cap results: `codeatlas.context.indexed.max-files` (default 16); when `intent.confidence() < 0.50`, cap doubles to `max-files * 2` (wider retrieval, scores unchanged).
7. For final selected files only: read source from disk and build snippet via `SymbolCenteredSnippetExtractor` (±20 lines around each anchor symbol, merged ranges, bounded by `codeatlas.context.max-snippet-lines` and `codeatlas.context.max-snippet-chars`; fallback to leading non-empty lines when no symbol hit).

`RetrievedFile.type` is set from metadata `architecturalRole`. `frontendImpact` on `Intent` does not affect scoring.

| Index | Role |
| --- | --- |
| Project File Index | **Primary** (path, extension — via metadata FK) |
| Project File Metadata Index | **Primary** (scoring source) |

---

### Step 4 — Detect missing context

**Service:** `KnowlegeEngine` → `MissingContextDetector`  
**Helper:** `PromptHelper.formatFiles` (retrieval signals + `metadata_json` lookup)  
**Prompt template:** `src/main/resources/prompts/context/missing-context.md`  
**Model:** AI  

**Input:** user request + original `Intent` + `ContextResult` from step 3.

**Output:** delta `Intent` for step 5 — only retrieval targets not already covered by the original intent.

**Behavior:**

1. Build prompt with full retrieved-file blocks (`PromptHelper`) and project context (`AGENTS`, design).
2. Model returns a second `Intent` (`missingIntent`) — same schema as step 2, listing context that appears missing or underrepresented.
3. `MissingContextDetector.diffIntent(original, missingIntent)` subtracts the original intent from the model response:
   - **List fields** (`symbols`, `concepts`, `capabilities`, `architecturalRoles`, `changeImpactAreas`): items present in `missingIntent` but absent from `original` (order preserved from `missingIntent`).
   - **`action`:** always `original.action()` (second pass keeps the same action type).
   - **`frontendImpact`:** `true` only when `missingIntent.frontendImpact` is `true` and `original.frontendImpact` is `false`.
   - **`confidence`:** taken from `missingIntent` (confidence that an additional retrieval pass will help).

Example: original `symbols = [UserController, UserRepository, UserService]`; model returns `symbols = [UserController, UserRepository, UserEntity]` → diff `symbols = [UserEntity]`.

When retrieved files look sufficient, the model returns empty arrays and low confidence (`<= 0.30`); the diff intent is empty and step 5 adds no meaningful candidates.

**`{{RETRIEVED_FILES}}` block format** (via `PromptHelper` + `ProjectFileMetadataIndexRepository.findByFileId`):

```text
=== FILE START ===

filePath: src/.../UserService.java

language: java

type: service

score: 240

reasons:
symbol: User
architecturalRole: service

symbols:
UserService

snippet:
<disk excerpt>

metadataJson:
<raw metadata_json from project_file_metadata_index, or "(no metadata)">

=== FILE END ===
```

**Prompt placeholders**

| Placeholder | Wired in `MissingContextDetector` |
| --- | --- |
| `{{USER_REQUEST}}` | Yes |
| `{{INTENT}}` | Yes (`IntentEngine.formatIntent`) |
| `{{RETRIEVED_FILES}}` | Yes (`PromptHelper.formatFiles`) |
| `{{AGENTS_FILE}}` | Yes (`ProjectService.resolveAgentsFileContent`) |
| `{{DESIGN_FILE}}` | Yes (`ProjectService.resolveDesignFileContent`) |

**Prompt response shape:** same `Intent` record as step 2 (see [Step 2](#step-2--extract-intent)). Full template: `src/main/resources/prompts/context/missing-context.md`.

| Index | Role |
| --- | --- |
| Project File Index | Secondary (`RetrievedFile.file` ref for metadata lookup) |
| Project File Metadata Index | **Primary** (`metadata_json` injected per retrieved file) |

---

### Step 5 — Second deterministic retrieval

**Service:** `ContextEngine` → `DeterministicRetriever`  
**Model:** Code (no AI)  

**Input:** delta `Intent` from step 4 (additional retrieval targets only).

**Output:** `ContextResult` with additional `RetrievedFile` candidates.

Same retriever and scoring algorithm as step 3. When step 4 diff intent has non-empty list fields or elevated `frontendImpact`, the second pass surfaces files the first pass missed. Empty diff → step 5 returns no new candidates. `IndexedContextService.mergeFiles` deduplicates by file path in step 6.

| Index | Role |
| --- | --- |
| Project File Index | **Primary** (path, extension — via metadata FK) |
| Project File Metadata Index | **Primary** (scoring source) |

---

### Step 6 — Merge second retrieval files

**Service:** `IndexedContextService.mergeFiles`  
**Model:** Code  

Merges files from step 3 and step 5. Deduplicates by `file_path`; primary list wins on conflict.

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
| Project File Index | Step 1 (refresh); steps 3 & 5 (path/extension via metadata join) |
| Project File Metadata Index | Steps 3 & 5 (retrieval scoring); step 4 (prompt blocks); step 7 (summaries — partial) |

---

## Implementation status

| Component | Status |
| --- | --- |
| File index scan / staleness | Implemented |
| AI metadata generation (`FileSummariesIndexService`) | Implemented |
| Intent extraction | Implemented |
| Deterministic retrieval | **Implemented** — metadata scoring, ranking, disk snippets |
| Missing context → retrieval intent | **Implemented** — model `Intent` + `diffIntent` delta |
| Prompt file blocks (`PromptHelper`) | **Implemented** — retrieval signals + `metadata_json` |
| Architecture summary metadata lookup | **Partial** — `formatSummaries` TODO |
| Prompt assembly | Implemented |
