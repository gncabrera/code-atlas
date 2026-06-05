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
