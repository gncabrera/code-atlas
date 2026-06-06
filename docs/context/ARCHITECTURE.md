# Final architecture
Pasos de obtención de contexto:

```
| Arquitectura             | Responsabilidad               | Modelo |
| ------------------------ | ----------------------------- | ------ |
| Intent Extraction        | Entender qué quiere usuario   | Gemma  |
| Deterministic Retrieval  | Encontrar archivos candidatos | Código |
| Graph Expansion          | Expandir dependencias         | Código |
| Missing Context Detector | Detectar huecos               | Gemma  |
| Second Retrieval         | Buscar faltantes              | Código |
| Architecture Summary     | Comprimir conocimiento        | Gemma  |
| Prompt Builder           | Construir prompt final        | Código |
| Implementation           | Generar solución              | Gemini |

```
Crear 4 módulos:
```
Intent Engine = Intent Extraction
    ↓
Context Engine = Retrieval + Graph Expansion + Second Retrieval
    ↓
Knowledge Engine = Missing Context Detector + Architecture Summary
    ↓
Prompt Engine = Prompt Builder + Gemini
```


# Prerequisites
Ver PROJECT_INDEX for index information

# Steps
## 1. Intent extraction
Input:
```
Agregar soft delete a usuarios
```
Output:
```
{
    "action":"modify",
    "entities":["User"],
    "operations":["soft delete"],
    "layers":["controller","service","repository","entity","migration"],
    "frontendImpact":true
}
```
Objetivo: Convertir lenguaje humano → búsqueda estructurada.
Herramienta: Gemma

### Índices usados

#### Principal

Ninguno.

#### Secundarios

##### Business Concept Index

Puede ayudar a mapear:

```text
usuarios
↓
user
```

o

```text
login
↓
authentication
```

Pero step está definido como:

```text
Herramienta: Gemma
```

Por lo tanto índice no es requisito.

### Conclusión

| Índice                 | Uso        |
| ---------------------- | ---------- |
| Business Concept Index | Secundario |
| Resto                  | No usado   |


## 2. Deterministic Retrieval

Input:
```
{
    "entities":["User"],
    "operations":["soft delete"]
}
```
Busca:
```
UserController
UserService
UserRepository
User.java
```
Todavía sin IA. Sólo índice.

### Índices principales

#### Symbol Index

Permite resolver:

```text
User
↓
UserService
UserRepository
UserController
```

#### Business Concept Index

Permite búsquedas conceptuales:

```text
user
↓
archivos relacionados
```

### Índices secundarios

#### Endpoint Index

Si request menciona:

```text
GET /api/users
```

permite encontrar controlador inicial.

#### Database Index

Si request menciona:

```text
users table
deleted column
```

permite encontrar entidad y repositorio.

### Conclusión

| Índice                 | Uso        |
| ---------------------- | ---------- |
| Symbol Index           | Principal  |
| Business Concept Index | Principal  |
| Endpoint Index         | Secundario |
| Database Index         | Secundario |

## 3. Graph Expansion
Expandir dependencias:
Input:
```
UserController
UserService
UserRepository
```
Expande:
```
UserDto
UserMapper
UserListComponent
UserApiService
V12__user_table.sql
```
Usando grafo.

Ejemplo:
```
UserController
↓
UserService
↓
UserRepository
↓
User
```
y
```
UserController
↑
UserApiService
↑
UserListComponent
```
Acá suele aparecer contexto que keyword search nunca encuentra.

### Índice principal

#### Dependency Graph

Este step existe específicamente para recorrer relaciones.

Ejemplos:

```text
CALLS
USES
IMPLEMENTS
EXTENDS
MAPS_TO
CONSUMES
```

Todo sale del grafo.

### Índices secundarios

#### Frontend Route Index

Permite saltar:

```text
endpoint
↓
frontend callers
```

Más eficiente que recorrer parte del grafo.

#### Database Index

Permite conectar:

```text
Entity
↓
Migration
```

sin recorrer demasiados nodos.

### Conclusión

| Índice               | Uso        |
| -------------------- | ---------- |
| Dependency Graph     | Principal  |
| Frontend Route Index | Secundario |
| Database Index       | Secundario |

## 4. Missing Context Detector

Gemma recibe:
```
Request
+
Retrieved Files
```
Pregunta:
```
¿Qué falta para implementar esto?
```
Respuesta:
```
{
    "missing":[
        "migration",
        "frontend caller"
    ]
}
```
o
```
{
    "missing":[]
}
```

### Índices principales

Ninguno.

Step definido explícitamente como:

```text
Gemma recibe:
Request + Retrieved Files
```

Analiza contexto ya recuperado.

### Índices secundarios

#### Pattern Index

Puede ayudar a detectar faltantes típicos.

Ejemplo:

```text
soft-delete
```

requiere:

```text
migration
repository
entity
```

Pero no es obligatorio según arquitectura.

#### AI Summary Index

Puede facilitar análisis rápido de archivos.

Tampoco obligatorio.

### Conclusión

| Índice           | Uso        |
| ---------------- | ---------- |
| Pattern Index    | Secundario |
| AI Summary Index | Secundario |
| Resto            | No usado   |


## 5. Second Retrieval

Input:
```
{
    "missing":[
        "migration",
        "frontend caller"
    ]
}
```
Busca:
```
V15__add_deleted_flag.sql
UserApiService.ts
```
Ahora contexto queda completo.

### Índices principales

#### Database Index

Para:

```text
migration
table
entity
repository
```

#### Frontend Route Index

Para:

```text
frontend caller
component
service
endpoint
```

#### Symbol Index

Para buscar clases concretas faltantes.

### Índices secundarios

#### Dependency Graph

Para expandir desde archivos encontrados.

#### Pattern Index

Para localizar implementaciones similares.

### Conclusión

| Índice               | Uso        |
| -------------------- | ---------- |
| Database Index       | Principal  |
| Frontend Route Index | Principal  |
| Symbol Index         | Principal  |
| Dependency Graph     | Secundario |
| Pattern Index        | Secundario |

## 6. Architecture Summary

Gemma recibe:
```
Request
+
Todos archivos recuperados
```
Produce:
```
Current pattern:

- Controllers delegate to Services
- Services own business logic
- Repositories use Spring Data JPA
- User entity maps USERS table
- No soft delete mechanism exists
- Migrations managed by Flyway
```
LLM recibe hechos explícitos. No tiene que deducirlos.

### Índices principales

#### AI Summary Index

Fue creado exactamente para resumir archivos.

Permite reducir tokens antes de enviar a Gemma.

### Índices secundarios

#### Pattern Index

Permite detectar patrones repetidos:

```text
crud-resource
soft-delete
audit
```

#### Dependency Graph

Permite entender estructura general.

#### Database Index

Aporta relaciones entidad-tabla-migration.

### Conclusión

| Índice           | Uso        |
| ---------------- | ---------- |
| AI Summary Index | Principal  |
| Pattern Index    | Secundario |
| Dependency Graph | Secundario |
| Database Index   | Secundario |

## 7. Prompt Builder (What Will Be Sent To AIModel (AIModelPrompt))

Acá no usar IA.

Template puro.
```
# User Request

# Architecture Facts

# Dependency Graph

# Relevant Files

# Code Snippets

# Agents

# Design

# Instructions
```
Todo determinístico.

### Índices principales

Ninguno.

### Conclusión

| Índice  | Uso       |
| ------- | --------- |
| Ninguno | Principal |

# Resumen final

| Índice                 | Step principal                                           |
| ---------------------- | -------------------------------------------------------- |
| File Index             | Infraestructura base. No asociado directamente a un step |
| Symbol Index           | Deterministic Retrieval, Second Retrieval                |
| Endpoint Index         | Deterministic Retrieval                                  |
| Dependency Graph       | Graph Expansion                                          |
| Database Index         | Second Retrieval                                         |
| Frontend Route Index   | Second Retrieval                                         |
| Business Concept Index | Deterministic Retrieval                                  |
| Pattern Index          | Support de Missing Context Detector                      |
| AI Summary Index       | Architecture Summary                                     |