# ARCHITECTURE.md

# Prompt Optimizer

## Invariants

* El sistema debe ser language-agnostic.
* Toda recuperación de contexto debe ser index-driven.
* Los motores de recuperación no deben parsear código fuente directamente.
* Los LLMs no participan en búsquedas primarias.
* Los LLMs solo participan en:

    * Intent Extraction
    * Missing Context Detection
    * Architecture Summary
    * Final Implementation
* La indexación debe ser incremental mediante hash.
* Cada índice debe persistirse en tablas independientes.
* Graph Expansion debe utilizar exclusivamente Dependency Graph.
* Prompt Builder debe ser completamente determinístico.
* Debe ser posible habilitar/deshabilitar lenguajes por proyecto.

---

## Architectural Boundaries

### Intent Engine

Responsable únicamente de:

* Intent Extraction

Produce:

* Intent

No realiza búsquedas.

---

### Context Engine

Responsable únicamente de:

* Deterministic Retrieval
* Graph Expansion
* Second Retrieval

Produce:

* ContextResult

No realiza llamadas a LLM.

---

### Knowledge Engine

Responsable únicamente de:

* Missing Context Detector
* Architecture Summary

Produce:

* KnowledgeResult

No realiza indexación.

---

### Prompt Engine

Responsable únicamente de:

* Prompt Builder
* Envío a modelo final

Produce:

* AIModelPrompt

No realiza retrieval.

---

### Indexing Subsystem

Responsable únicamente de:

* Descubrimiento de archivos
* Detección de lenguaje
* Ejecución de indexers
* Persistencia de índices

No participa en generación de contexto.

---

## Required Indices

### FILE_INDEX

Inventario de archivos.

---

### SYMBOL_INDEX

Clases
Interfaces
Records
Enums
Métodos

---

### ENDPOINT_INDEX

Endpoints backend.

---

### GRAPH_EDGE

Dependency Graph.

Relaciones:

* CALLS
* USES
* EXTENDS
* IMPLEMENTS
* MAPS_TO
* CONSUMES
* QUERIES
* EXPOSES

---

### DATABASE_INDEX

Tabla
Entity
Repository
Migration

---

### FRONTEND_INDEX

Frontend callers.

---

### BUSINESS_CONCEPT_INDEX

Conceptos de negocio.

Generación offline.

---

### PATTERN_INDEX

Patrones arquitectónicos.

Generación offline.

---

### FILE_SUMMARY_INDEX

Resumen IA por archivo.

Generación offline.

---

## Parser Architecture

La plataforma debe implementar un SPI.

Cada lenguaje provee:

* Detección de archivos
* Extracción de símbolos
* Extracción de relaciones
* Extracción de endpoints
* Extracción de metadata

Ejemplos:

* JavaIndexer
* TypeScriptIndexer
* AngularIndexer
* PythonIndexer

No debe existir lógica específica de lenguaje fuera de los indexers.

---

## Persistence Constraints

* Java 21
* SQLite
* Flyway
* Una tabla por índice
* Sin Neo4j
* Sin Elasticsearch obligatorio
* Sin vector database obligatoria

---

## Current Assumptions

* Repositorio único por proyecto.
* Escala objetivo inicial: 100k–1M LOC.
* Java y Angular son los principales lenguajes soportados.
* Gemma puede ejecutarse localmente para tareas offline.
* Gemini será utilizado para generación final.

---

## Migration Requirements

La arquitectura debe permitir agregar:

* Nuevos lenguajes
* Nuevos índices
* Nuevos tipos de relaciones del grafo

sin modificar motores existentes.
