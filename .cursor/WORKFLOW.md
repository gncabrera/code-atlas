# Workflow

Token-saving impl loop. Skills assume @./cursor/STATE_FORMAT.md format.

## Model matrix

| Phase | Model | Reason |
|-------|-------|--------|
| architect (ambiguous arch / high risk) | Opus 4.7 | reasoning depth |
| architect (clear scope) | Sonnet 4.6 | cheap enough |
| implement (boundary/auth/migration) | Sonnet 4.6 | safety |
| implement (routine edits, scope clear) | composer-2-fast or gpt-5.5-medium | -80% cost |
| snapshot | composer-2-fast | mechanical |
| risk-scan pre/post (normal) | Sonnet 4.6 | reasoning |
| risk-scan post (release crítica auth/pagos/migrations) | Opus 4.7 | only here |

## Chat hygiene (prompt cache)

- Mantener mismo chat durante feature completa → cache hit ~90% off input.
- Cerrar chat solo cuando: cambio de modelo | feature done | contexto huele pesado | snapshot tomado.
- Antes de cerrar: `/snapshot`.
- Nuevo chat siempre: leer `@implementation/STATE_SNAPSHOT.md` + `@implementation/TASKS.md` primero.

## State canonical

Fuente única de verdad: `@implementation/{ARCHITECTURE,TASKS,RISKS}.md`.
Reglas de lectura/formato: ver @./cursor/STATE_FORMAT.md.
Listados de archivos (`rg --files-with-matches`) no cuentan contra contexto; solo contenido leído cuenta.

## Fase 0 - Indexing
Modelo: Gemini 3.1 Flash Lite - El mas barato que encuentre
Input:
```
Feature: soft delete users.
Endpoint: DELETE /api/profile

Need:
- schema changes
- affected queries
- auth/session implications
- cascade risks
```

Output:
```
@path/to/file1 L1-L50
@path/to/file2 L80-L90
@path/to/file3 L563-L612

```



## Fase 1 — Discovery

Modelo: Sonnet default. Opus solo si arquitectura ambigua / riesgo alto.
Utilizar el context de Fase 0

```
/architect

Feature: soft delete users.
Endpoint: DELETE /api/profile
Write: @implementation/{ARCHITECTURE,TASKS,RISKS}.md

Need:
- schema changes
- affected queries
- auth/session implications
- cascade risks

Context:
@path/to/file1 L1-L50
@path/to/file2 L80-L90
@path/to/file3 L563-L612

Don't explore the whole repo, just the Context
```

Final esperado: `state written`. Cerrar chat? No — seguir mismo chat para cache.

## Gate — Audit inicial (risk-scan pre)

Default: SKIP.
Ejecutar solo si feature toca: auth/session | permisos/tenancy | datos críticos | migraciones | pagos | concurrencia.
Si `RISKS.md` ya captura riesgos activos → SKIP (duplicación).

```
/risk-scan pre
```

## Fase 2 — Incremental implementation

Modelo: Sonnet (boundary/auth) | composer-2-fast (rutina).
Chunks chicos. Una task por turn.

```
/implement
Step: @implementation/TASKS.md#T3
```

Reglas built-in: edita directo, actualiza state mismo turn, lint/test focused solo si cheap.

- Tambien se puede hacer una implementation full si se quisere

## Gate — Review (risk-scan post)

Default: SKIP.
Ejecutar solo: task riesgosa | diff grande / cross-module | test/lint falla | antes de PR.

```
/risk-scan post
```

## Gate — Final audit

Default: SKIP.
Modelo: Sonnet 4.6, o Opus 4.7 solo si release crítica auth/pagos/migrations.

```
/risk-scan post
```

## Snapshot rotation

Default: SKIP.
Ejecutar cuando:
- antes de cambiar modelo/chat
- después de arquitectura estable, antes de impl larga
- contexto leído pesa más que snapshot
- chat huele pesado

```
/snapshot
```

Usar `@implementation/STATE_SNAPSHOT.md`:
- inicio nuevo chat
- después de feature grande
- después de refactor importante
- cuando contexto huele pesado

## State correction (manual)

El state se actualiza dentro de `/implement`.
Si hay desync, corregir manualmente en `@implementation/{ARCHITECTURE,TASKS,RISKS}.md`.
