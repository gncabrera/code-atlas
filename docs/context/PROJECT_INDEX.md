# Indices

## 1. File Index

Base.
```
{
    "id": 123,
    "path": "...",
    "language": "java",
    "type": "controller",
    "hash": "...",
    "lastIndexedAt": "..."
}
```
Tabla:
```
FILE_INDEX
```

## 2. Symbol Index

Para buscar clases, métodos, records, interfaces.
```
{
    "symbol": "UserService",
    "kind": "class",
    "fileId": 123,
    "line": 50
}
```
Tabla:
```
SYMBOL_INDEX
```
Muy útil para:
```
Find UserService
Find UserDto
Find UserMapper
```

## 3. Endpoint Index

Para backend.
```
{
    "method": "GET",
    "path": "/api/users/{id}",
    "controller": "UserController",
    "service": "UserService"
}
```
Tabla:
```
ENDPOINT_INDEX
```
Permite:
```
GET /api/users
```
↓
```
Controller
Service
```

## 4. Dependency Graph

Tu segundo índice.

Más importante de todos.
```
{
    "source": "UserController",
    "target": "UserService",
    "relation": "CALLS"
}
```
```
{
    "source": "UserService",
    "target": "UserRepository",
    "relation": "CALLS"
}
```
Tabla:
```
GRAPH_EDGE
```
Tipos:
```
CALLS
IMPLEMENTS
EXTENDS
USES
MAPS_TO
EXPOSES
QUERIES
CONSUMES
```
Todo grafo sale de acá.

## 5. Database Index

Fundamental.
```
{
    "table": "users",
    "entity": "User",
    "repository": "UserRepository",
    "migration": "V15__users.sql"
}
```
Tabla:
```
DATABASE_INDEX
```
Cuando usuario dice:
```
add column deleted
```
encontrás:
```
Entity
Repository
Migration
```
sin IA.

## 6. Frontend Route Index

Para Angular.
```
{
    "component": "UserListComponent",
    "service": "UserApiService",
    "endpoint": "GET /api/users"
}
```
Tabla:
```
FRONTEND_INDEX
```
Permite:
```
endpoint
↓
frontend callers
```

## 7. Business Concept Index

Muy importante para búsqueda semántica barata.
```
{
    "concept": "user",
    "files": [
        "UserController",
        "UserService",
        "UserRepository"
    ]
}
```
o
```
{
    "concept": "authentication",
    "files": [...]
}
```
Tabla:
```
BUSINESS_CONCEPT_INDEX
```
Esto puede generarlo Gemma offline. Una sola vez.

## 8. Pattern Index

Guardar patrones arquitectónicos.
```
{
    "pattern": "crud-resource",
    "files": [
        "UserController",
        "UserService",
        "UserRepository"
    ]
}
```
o
```
{
"pattern": "soft-delete"
}
```
Tabla:
```
PATTERN_INDEX
```
Después podés preguntar:
```
Find existing soft delete implementation
```
sin embeddings.

## 9. AI Summary Index

Generado por Gemma.

Por archivo.
```
{
    "file":"UserService.java",
    "summary":"Handles user CRUD operations..."
}
```
Tabla:
```
FILE_SUMMARY_INDEX
```
Muy útil para reranking.


# Uso de indices en context generation

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