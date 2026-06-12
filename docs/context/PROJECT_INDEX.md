# Indices

The indexed context pipeline uses **two** SQLite-backed indices. All former separate indices (symbol, endpoint, dependency graph, database, frontend route, business concept, pattern, AI summary) are consolidated into these two.

See [ARCHITECTURE.md](./ARCHITECTURE.md) for how each pipeline step consumes them.

---

## 1. Project File Index (deterministic)

**Entity:** `ProjectFileIndex`  
**Table:** `project_file_index`  
**Builder:** `ProjectIndexService`

Scans the project workspace and records one row per relevant source file. No AI.

| Column | Purpose |
| --- | --- |
| `file_path` | Path relative to project root |
| `file_extension` | Normalized extension |
| `last_modified_epoch` | Filesystem last-modified time |
| `content_hash` | SHA-256 of file content (change detection) |
| `token_count` | Rough token estimate `(length + 3) / 4` |
| `updated_at` | Last index refresh timestamp |

**Used for:**

- Staleness checks before context build (`ProjectIndexService.isStale`)
- File inventory for offline metadata generation
- Deterministic retrieval: joined from `project_file_metadata_index` (`@EntityGraph` on `file`) to supply `file_path`, `file_extension`, and the `ProjectFileIndex` reference on `RetrievedFile`

---

## 2. Project File Metadata Index (AI-generated)

**Entity:** `ProjectFileMetadataIndex`  
**Table:** `project_file_metadata_index`  
**Builder:** `FileSummariesIndexService` + `OfflineFileSummaryChunkBuilder`  
**Prompt template:** `src/main/resources/prompts/context/file-summary.md`  
**DTO:** `FileSummaryOfflineResponse` (stored in `metadata_json`)

One metadata document per indexed file. Generated offline by the configured AI model in batched chunks.

| Column | Purpose |
| --- | --- |
| `project_file_index_id` | FK to `project_file_index` (1:1) |
| `metadata_json` | JSON metadata document for the file |
| `content_hash` | Hash of source content when metadata was generated |
| `updated_at` | Last metadata generation timestamp |

### File metadata extraction prompt

Source code analysis engine. Analyzes one or more source files and generates a language-agnostic semantic metadata document for each file independently.

**Metadata used for:**

- source code retrieval
- context generation
- dependency discovery
- impact analysis
- prompt optimization

**Important rules**

- Analyze ALL provided files independently.
- Never merge information between files.
- Never use symbols from one file when generating metadata for another file.
- Return metadata for every input file.
- Output order must match input order.
- Return JSON only.
- Do not wrap JSON in markdown.
- Do not add explanations.
- Do not add comments.
- Every field from the schema must exist.
- Never return null.
- Use empty arrays (`[]`) when information is not available.
- Use empty strings (`""`) when information is not available.
- Keep values concise.
- Prefer normalized concepts over literal code identifiers.
- Be language agnostic.
- Infer business concepts when possible.
- Use information from architecture, neighboring files and project conventions when helpful.
- Do not invent functionality that is not reasonably implied by the file.

**Prompt placeholders**

| Placeholder | Source |
| --- | --- |
| `{{PROJECT_ARCHITECTURE}}` | `Project.description` |
| `{{AGENTS_FILE}}` | `ProjectService.resolveAgentsFileContent` |
| `{{FILES}}` | Batch block built by `OfflineFileSummaryChunkBuilder` |

**Input file format**

Each file is provided as:

```text
=== FILE START ===

filePath: src/main/java/com/example/UserService.java

relativeDirectory: src/main/java/com/example

neighborFiles:
UserController.java
UserRepository.java
UserDto.java

content:
<file content>

=== FILE END ===
```

**Output schema**

Return a JSON array. One entry per input file.

```json
[
  {
    "filePath": "",
    "metadata": {
      "summary": "",
      "keywords": [],
      "concepts": [],
      "responsibilities": [],
      "capabilities": [],
      "symbols": [],
      "dependencies": [],
      "externalReferences": [],
      "dataStructures": [],
      "contracts": [],
      "entryPoints": [],
      "outputs": [],
      "sideEffects": [],
      "patterns": [],
      "frameworks": [],
      "technologies": [],
      "architecturalRole": "",
      "executionContext": "",
      "businessDomains": [],
      "relatedTopics": [],
      "changeImpactAreas": [],
      "implementationHints": [],
      "searchText": "",
      "confidence": {
        "overall": 0.73,
        "summary": 0.95,
        "concepts": 0.80,
        "responsibilities": 0.85,
        "capabilities": 0.82,
        "dependencies": 0.40,
        "architecturalRole": 0.98
      }
    }
  }
]
```

**Field definitions**

#### summary

One short paragraph describing the purpose of the file. Maximum 100 words.

#### keywords

Important literal terms found or strongly implied by the file.

Example: `["user", "authentication", "repository"]`

#### concepts

Business or technical concepts represented by the file.

Example: `["user-management", "authentication", "authorization"]`

#### responsibilities

Primary responsibilities owned by the file.

Example: `["manage users", "validate user state"]`

#### capabilities

Actions or behaviors provided by the file.

Example: `["create user", "delete user"]`

#### symbols

Important named symbols declared by the file.

Example: `["UserService", "createUser", "UserDto"]`

#### dependencies

Important internal or external symbols referenced by the file.

Example: `["UserRepository", "NotificationService"]`

#### externalReferences

External systems, APIs, databases, libraries, cloud services or technologies.

Example: `["Oracle", "Redis", "AWS S3"]`

#### dataStructures

Important domain structures used or defined.

Example: `["User", "UserDto", "UserRequest"]`

#### contracts

Public contracts exposed by the file.

Example: `["createUser", "findById", "GET /api/users"]`

#### entryPoints

Ways execution can enter this file.

Example: `["REST endpoint", "event handler", "scheduled task", "CLI command"]`

#### outputs

Artifacts produced by the file.

Example: `["UserDto", "JSON response", "CSV file"]`

#### sideEffects

Observable effects beyond returning values.

Example: `["writes database", "sends email", "publishes event"]`

#### patterns

Recognized architectural or implementation patterns.

Example: `["repository", "crud-resource", "adapter", "factory"]`

#### frameworks

Detected frameworks.

Example: `["spring-boot", "angular", "react"]`

#### technologies

Detected technologies.

Example: `["java", "oracle", "redis"]`

#### architecturalRole

Single best architectural role.

Examples: `controller`, `service`, `repository`, `entity`, `dto`, `ui-component`, `configuration`, `migration`, `test`, `utility`

#### executionContext

Single best execution context.

Examples: `backend`, `frontend`, `database`, `infrastructure`, `build`, `test`, `shared`

#### businessDomains

Business areas represented by the file.

Example: `["users", "authentication"]`

#### relatedTopics

Topics frequently associated with the file.

Example: `["security", "account-management"]`

#### changeImpactAreas

Areas likely impacted when the file changes.

Example: `["database", "api", "frontend", "security"]`

#### implementationHints

Short implementation observations useful during future modifications.

Example: `["changes usually require repository updates", "modifications may require dto updates"]`

#### searchText

Dense retrieval string. Combines summary, concepts, responsibilities, capabilities, symbols, patterns, business domains, and technologies. Single space-separated string optimized for search and ranking.

#### confidence

Confidence scores indicating how accurately the generated metadata represents the file. All values are decimals between `0.0` and `1.0`.

Interpretation:

- `1.0` = extremely high confidence
- `0.8` = high confidence
- `0.6` = moderate confidence
- `0.4` = low confidence
- `0.2` = very low confidence
- `0.0` = unable to determine

Factors that **increase** confidence: clear naming, explicit responsibilities, self-contained implementation, strong architectural conventions, well-defined dependencies, rich comments or documentation.

Factors that **decrease** confidence: missing context, dynamic behavior, reflection, generated code, obfuscated code, minified files, framework magic, indirect dependencies, ambiguous naming.

Schema:

```json
{
  "confidence": {
    "overall": 0.0,
    "summary": 0.0,
    "concepts": 0.0,
    "responsibilities": 0.0,
    "capabilities": 0.0,
    "dependencies": 0.0,
    "architecturalRole": 0.0
  }
}
```

| Sub-field | Meaning |
| --- | --- |
| `overall` | Confidence the metadata document accurately describes the file as a whole |
| `summary` | Confidence the summary correctly describes file purpose |
| `concepts` | Confidence extracted business/technical concepts are accurate |
| `responsibilities` | Confidence identified responsibilities are correct |
| `capabilities` | Confidence identified capabilities and behaviors are accurate |
| `dependencies` | Confidence extracted dependencies are complete and accurate |
| `architecturalRole` | Confidence the selected architectural role is correct |

Confidence validation:

- Every confidence field must exist.
- Every confidence value must be a decimal number between `0.0` and `1.0`.
- Confidence values should reflect actual certainty, not optimism.
- Do not assign high confidence without supporting evidence.
- Use lower confidence when additional files or project context would be required for accurate analysis.

**Output validation rules**

- Return a JSON array.
- Array size MUST equal the number of input files.
- Output order MUST match input order.
- Every file MUST generate exactly one metadata entry.
- `filePath` MUST exactly match the input `filePath`.
- Do not omit files.
- Do not merge files.
- Do not create additional files.
- Return exactly one JSON array and nothing else.

**Used for:**

- **Primary** — deterministic retrieval scoring (`DeterministicRetriever` → `MetadataMatchScorer`)
- **Primary** — missing-context prompt blocks (`PromptHelper` → `findByFileId` loads raw `metadata_json` per retrieved file)
- Architecture summary input (partial — `formatSummaries` TODO in `ArchitectureSummarizer`)

---

## Prompt file blocks (`PromptHelper`)

**Class:** `PromptHelper`  
**Repository:** `ProjectFileMetadataIndexRepository.findByFileId`

Shared formatter for AI prompts that need rich retrieved-file context (step 4 today; reusable by other Knowledge Engine steps).

For each `RetrievedFile`, emits one block:

```text
=== FILE START ===

filePath: <project_file_index.file_path>
language: <RetrievedFile.language>
type: <RetrievedFile.type — metadata architecturalRole>
score: <RetrievedFile.score>
reasons:
<one reason per line>
symbols:
<one symbol per line>
snippet:
<disk excerpt or "(no snippet)">
metadataJson:
<raw metadata_json row, or "(no metadata)">

=== FILE END ===
```

Metadata lookup uses `project_file_index.id` from `RetrievedFile.file`. Missing rows render `(no metadata)`; the block is still included.

---

## Deterministic retrieval scoring

**Service:** `DeterministicRetriever` (steps 3 and 5)  
**Scorer:** `MetadataMatchScorer`  
**Normalizer:** `TermNormalizer`

Retrieval is fully deterministic: no AI, embeddings, or external search. Only rows in `project_file_metadata_index` are scored; indexed files without metadata are skipped.

### Intent as search query

`Intent` fields drive matching:

| Intent field | Typical metadata targets |
| --- | --- |
| `symbols` | `symbols` (+100) |
| `symbols` + `concepts` + `capabilities` (search term set) | `dataStructures` (+90), `dependencies` (+40), `businessDomains` (+30), `keywords` (+20), `summary` (+20), `searchText` (+10) |
| `concepts` | `concepts` (+80) |
| `capabilities` | `capabilities` (+70) |
| `architecturalRoles` | `architecturalRole` (+60) |
| `changeImpactAreas` | `changeImpactAreas` (+50) |

`frontendImpact` does not affect scoring. `action` is not scored.

### Additive weights

| Match type | Score |
| --- | --- |
| symbol exact match | +100 |
| dataStructure exact match | +90 |
| concept match | +80 |
| capability match | +70 |
| architecturalRole match | +60 |
| changeImpactArea match | +50 |
| dependency match | +40 |
| businessDomain match | +30 |
| keyword match | +20 |
| summary contains term | +20 |
| searchText contains term | +10 |

Weights are constants in `MetadataMatchScorer`. Multiple intent terms can each contribute; duplicate reasons for the same match type are deduplicated.

### String normalization

`TermNormalizer` applies before all comparisons:

- ignore case
- trim whitespace
- collapse separators — `soft delete`, `soft-delete`, and `soft_delete` are equal

List fields use exact normalized equality. `summary` and `searchText` use normalized substring containment.

### Ranking and limits

1. Score every metadata row; discard `score <= 0`.
2. Sort `score DESC`, then `file_path ASC`.
3. Return top N where N = `codeatlas.context.indexed.max-files` (default 16), or `max-files * 2` when `intent.confidence() < 0.50`.

### Snippet extraction

After ranking, source files are read from disk for final picks only.

`SymbolCenteredSnippetExtractor`:

- anchor symbols = matched metadata symbols + intent `symbols`
- ±20 lines around each symbol occurrence in file content
- merge overlapping ranges; cap by `codeatlas.context.max-snippet-lines` and `codeatlas.context.max-snippet-chars`
- fallback: first non-empty lines when no symbol anchor found

### RetrievedFile population

| Field | Source |
| --- | --- |
| `file` | `ProjectFileIndex` (via metadata FK) |
| `language` | `ContextFileSupport.languageByExtension` |
| `type` | metadata `architecturalRole` |
| `score` | additive match total |
| `reasons` | e.g. `symbol: User`, `concept: soft-delete`, `architecturalRole: repository` |
| `symbols` | matched metadata symbols (or intent symbols if none matched) |
| `snippet` | disk excerpt (symbol-centered or fallback) |

---

## Index usage in context generation

| Step | Project File Index | Project File Metadata Index |
| --- | --- | --- |
| 1 — Ensure indices fresh | **Primary** (scan / refresh) | — |
| 2 — Extract intent | — | — |
| 3 — Deterministic retrieval | **Primary** (path via metadata join) | **Primary** (scoring) |
| 4 — Detect missing context | Secondary (`RetrievedFile.file` ref) | **Primary** (`metadata_json` in prompt via `PromptHelper`) |
| 5 — Second deterministic retrieval | **Primary** (path via metadata join) | **Primary** (scoring) |
| 6 — Merge second retrieval files | — | — |
| 7 — Summarize architecture | — | **Partial** (`formatSummaries` TODO) |
| 8 — Assemble context | — | — |

**Legend:** **Primary** = actively used. **Partial** = wired but incomplete. **Secondary** = designed usage not fully wired.
