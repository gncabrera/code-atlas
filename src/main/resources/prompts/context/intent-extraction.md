Extract a structured search query from the user request.

The output will be consumed by a deterministic retrieval engine that searches a project metadata index.

Your goal is NOT to explain the request.

Your goal is to translate natural language into search parameters that can be mapped directly against indexed metadata.

User request:

{{USER_REQUEST}}

Return exactly one JSON object.

Do not use markdown.

Do not add explanations.

Do not add comments.

Schema:
```
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
```

Field definitions:

* action:
  Primary intent of the request.

* symbols:
  Important class names, entity names, DTO names, service names, repository names, table names, endpoint names, or domain objects explicitly mentioned or strongly implied.

* concepts:
  Business or technical concepts relevant to the request.

* capabilities:
  Behaviors or actions that need to exist, be modified, or be investigated.

* architecturalRoles:
  Architectural layers likely involved.

  Allowed values:

    * controller
    * service
    * repository
    * entity
    * dto
    * migration
    * ui-component
    * configuration
    * test
    * utility

* changeImpactAreas:
  Areas likely affected.

  Allowed values:

    * database
    * api
    * frontend
    * security
    * configuration
    * infrastructure
    * testing

* frontendImpact:
  true when UI, pages, JavaScript, CSS, templates, frontend routes, or API consumers may need changes.

* confidence:
  Decimal value between 0.0 and 1.0 representing confidence that the extracted search query accurately reflects the request.

Rules:

* Return every field.
* Never return null.
* Use empty arrays when information cannot be inferred.
* Use concise normalized values.
* Prefer semantic concepts over literal phrases.
* Prefer domain symbols when identifiable.
* Infer architecturalRoles from the requested change.
* Infer changeImpactAreas from the requested change.
* Confidence must reflect actual certainty.
* Do not invent symbols not reasonably implied by the request.

Examples:

Input:
```
"Add soft delete support for users"
```
Output:
```
{
    "action": "modify",
    "symbols": [
     "User"
    ],
    "concepts": [
       "soft-delete"
    ],
    "capabilities": [
       "delete-user"
    ],
    "architecturalRoles": [
        "entity",
        "repository",
        "service",
        "migration"
    ],
    "changeImpactAreas": [
        "database",
        "api"
    ],
    "frontendImpact": false,
    "confidence": 0.93
}
```
Input:
```
"Create a page to manage AI models"
```
Output:
```
{
    "action": "create",
    "symbols": [
        "AIModel"
    ],
    "concepts": [
        "model-management"
    ],
    "capabilities": [
        "manage-models"
    ],
    "architecturalRoles": [
        "controller",
        "service",
        "ui-component"
    ],
    "changeImpactAreas": [
        "frontend",
        "api"
    ],
    "frontendImpact": true,
    "confidence": 0.91
}
```
General project context:

{{AGENTS_FILE}}

{{DESIGN_FILE}}
