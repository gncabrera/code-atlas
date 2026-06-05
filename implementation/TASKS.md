# TASKS.md

* [ ] T1 Definir modelo de dominio del Prompt Optimizer
* [ ] T2 Diseñar contratos entre Engines
* [ ] T3 Diseñar SPI de Language Indexers
* [ ] T4 Diseñar modelo de persistencia de índices
* [ ] T5 Diseñar pipeline de indexación incremental
* [ ] T6 Diseñar estrategia de descubrimiento de archivos
* [ ] T7 Diseñar FILE_INDEX
* [ ] T8 Diseñar SYMBOL_INDEX
* [ ] T9 Diseñar ENDPOINT_INDEX
* [ ] T10 Diseñar GRAPH_EDGE
* [ ] T11 Diseñar DATABASE_INDEX
* [ ] T12 Diseñar FRONTEND_INDEX
* [ ] T13 Diseñar BUSINESS_CONCEPT_INDEX
* [ ] T14 Diseñar PATTERN_INDEX
* [ ] T15 Diseñar FILE_SUMMARY_INDEX
* [ ] T16 Diseñar Intent Engine
* [ ] T17 Diseñar Context Engine
* [ ] T18 Diseñar Graph Expansion Algorithm
* [ ] T19 Diseñar Missing Context Detector
* [ ] T20 Diseñar Second Retrieval Strategy
* [ ] T21 Diseñar Architecture Summary Pipeline
* [ ] T22 Diseñar Prompt Builder
* [ ] T23 Diseñar configuración por proyecto
* [ ] T24 Diseñar flujo completo end-to-end
* [ ] T25 Validar arquitectura completa

# Indexed Context Generation (implementación)

Orden de implementación para `com.code.atlas.web.service.context.indexed`. Auto Mode.

* [ ] T26 Flyway `V6__create_context_indices.sql`: symbol_index, endpoint_index, graph_edge, database_index, frontend_index, business_concept_index, pattern_index, file_summary_index (FK project_id, unique upsert, índices)
* [ ] T27 Entidades `@Entity @Data` en `web.domain` por tabla (offline con `@Convert(SqliteLocalDateTimeConverter)` + `LocalDateTime updatedAt`)
* [ ] T28 Repositorios `@Repository extends JpaRepository` por entidad (findByProjectId, deleteByProjectId, deleteByProjectIdAndFilePathNotIn / upsert helpers)
* [ ] T29 Records de contrato en `...context.indexed`: Intent, RetrievedFile, GraphEdgeView, ContextResult, MissingContext, KnowledgeResult (constructores canónicos validan no-null)
* [ ] T30 SPI: `LanguageIndexer`, `IndexFileInput`, `IndexerOutput` + filas (`SymbolRow`, `EndpointRow`, `GraphEdgeRow`, `DatabaseRow`, `FrontendRow`) en `...context.indexed.indexer`
* [ ] T31 `JavaIndexer implements LanguageIndexer` (regex, reutiliza `ContextSymbolExtractor`)
* [ ] T32 `IndexBuildService`: walk (`ContextFileSupport`) → despacho `List<LanguageIndexer>` → persistencia incremental por `content_hash`; borrar filas dependientes de archivos cambiados/eliminados
* [ ] T33 Hook en `ProjectIndexService.refreshIndex` para invocar `IndexBuildService` cuando stale
* [ ] T34 `IntentExtractionService` (LLM) + plantilla `prompts/context/intent-extraction.md` (JSON) → `Intent` vía `JsonResponseExtractor.parseResponse`
* [ ] T35 `DeterministicRetriever`: SYMBOL/BUSINESS_CONCEPT/ENDPOINT → candidatos
* [ ] T36 `GraphExpander`: expandir desde `graph_edge` con profundidad máxima configurable
* [ ] T37 `MissingContextDetector` (LLM) + plantilla `prompts/context/missing-context.md` (JSON) → `MissingContext`
* [ ] T38 `SecondRetriever`: resolver faltantes con DATABASE/FRONTEND/SYMBOL/GRAPH
* [ ] T39 `ArchitectureSummarizer` (LLM) + plantilla `prompts/context/architecture-summary.md` (usa FILE_SUMMARY_INDEX) → architectureFacts
* [ ] T40 `IndexedContextAssembler`: markdown determinístico (Architecture Facts, Dependency Graph, Relevant Files, Code Snippets) con presupuesto de caracteres
* [ ] T41 `IndexedContextService`: orquestar los 7 steps en orden; sin dependencias circulares entre engines
* [ ] T42 Implementar `PromptContextService.buildIndexedContext(Project, String, AIModel)` delegando en `IndexedContextService`
* [ ] T43 `OfflineIndexService` + indexers offline (BusinessConcept, Pattern, FileSummary) vía Gemma + plantillas `prompts/context/offline/`
* [ ] T44 Wiring preview: agregar `aiModelId` a `BuildPreviewRequestDto`; `PromptService.buildPreview` resuelve `AIModel` y selecciona estrategia `codeatlas.context.strategy`; actualizar `PromptController`, `prompt-optimizer.html`, `prompt-optimizer.js`
* [ ] T45 Propiedades en `application.properties` (strategy, max-files, max-graph-depth, max-total-chars)
* [ ] T46 Tests: SPI/JavaIndexer, retrievers, assembler, orquestador (mock LLM), buildPreview con estrategia indexed
