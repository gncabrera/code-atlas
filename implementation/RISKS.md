# @implementation/RISKS.md

# Security

## R1

Los prompts generados pueden incluir archivos sensibles si la estrategia de retrieval no aplica filtros adecuados.

---

## R2

La indexación puede exponer secretos presentes en código fuente.

---

# Consistency

## R3

Los índices pueden quedar desactualizados respecto al filesystem.

Mitigación pendiente:
Indexación incremental por hash.

---

## R4

El Dependency Graph puede contener relaciones inconsistentes entre lenguajes distintos.

Mitigación pendiente:
Normalización de relaciones en capa común.

---

## R5

Business Concept Index y Pattern Index pueden divergir del código real con el tiempo.

Mitigación pendiente:
Reindexación periódica.

---

# Edge Cases

## R6

Repositorios con múltiples módulos.

---

## R7

Monorepos con múltiples tecnologías.

---

## R8

Dependencias generadas por frameworks que no aparecen explícitamente en AST.

Ejemplos:

* Spring Injection
* Angular DI
* Reflection

---

# Rollback

## R9

Cambios en esquema de índices pueden requerir reindexación completa.

Estrategia pendiente:
Versionado de índices.

---

## R10

Cambios futuros en SPI de indexers pueden romper implementaciones existentes.

Estrategia pendiente:
Versionado de contratos.

---

# Indexed Context Generation

## R11 (Security)

Los pasos LLM (Intent, Missing, Summary) envían snippets de código a un proveedor externo vía `AIModelService.sendToModel`.

Mitigación pendiente:
Filtrado de archivos sensibles antes de retrieval (ver R1) y límites de tamaño.

---

## R12 (Cost / Rate limit)

El pipeline indexed hace múltiples llamadas LLM por request (3) más generación offline. Modelos Gemma seedeados tienen `tokens_per_minute=0` y `requests_per_minute=15`.

Mitigación pendiente:
Caché de Intent/Summary por hash de request+índice; estrategia `deterministic` como default; offline fuera del hot path.

---

## R13 (Consistency)

Los índices nuevos (symbol/endpoint/graph/database/frontend) pueden divergir de `project_file_index` si el hook incremental no borra filas dependientes de archivos cambiados/eliminados.

Mitigación pendiente:
Borrado dependiente por `file_path` en `IndexBuildService`; unique constraints para upsert idempotente.

---

## R14 (Edge case)

`JavaIndexer` regex-based no captura relaciones inyectadas por framework (Spring DI, JPA, Flyway naming).

Mitigación pendiente:
Heurísticas específicas en el indexer; aceptar recall parcial en v1 (ver R8).

---

## R15 (Edge case)

`AIModel` seleccionado en el front puede no ser apto para tareas estructuradas (JSON), produciendo salidas no parseables.

Mitigación pendiente:
`JsonResponseExtractor.parseResponse` tolerante + reglas "JSON only" en plantillas + fallback determinístico si el parseo falla.

---

## R16 (Migration)

`V6` agrega 8 tablas; cambios de esquema posteriores requieren nueva migración y posible reindexación.

Estrategia pendiente:
Reindexación disparable por proyecto; versionado de índices (ver R9).
