# Step 4 — Detect Missing Context

Analyze whether the retrieved files provide sufficient implementation context for the user request.

The goal is to identify additional retrieval targets that may still be needed.

The output must be compatible with the same `Intent` DTO used by Step 2.

User request:

{{USER_REQUEST}}

Original intent:

{{INTENT}}

Retrieved file metadata

{{FILE_METADATA}}

Retrieved files:

{{RETRIEVED_FILES}}

Return exactly one JSON object.

Do not use markdown.

Do not add explanations.

Do not add comments.

Schema:

```json
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

Purpose:

Generate a SECOND retrieval intent containing only context that appears missing or underrepresented in the retrieved files.

This intent will be used for an additional deterministic retrieval pass.

Field definitions:

* action:
  Usually copied from the original intent.
  Change only if clearly necessary.

* symbols:
  Important symbols likely needed but not sufficiently represented in retrieved files.

* concepts:
  Concepts from the request that appear missing, incomplete, or weakly represented.

* capabilities:
  Capabilities from the request that appear missing, incomplete, or weakly represented.

* architecturalRoles:
  Architectural layers likely required but not sufficiently represented.

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
  Impact areas implied by the request that appear underrepresented.

  Allowed values:

  * database
  * api
  * frontend
  * security
  * configuration
  * infrastructure
  * testing

* frontendImpact:
  true only when additional frontend-related retrieval appears necessary.

* confidence:
  Confidence that performing an additional retrieval using this intent will provide useful implementation context.

Rules:

* Return every field.
* Never return null.
* Use empty arrays when nothing appears missing.
* Do not repeat information already well represented by retrieved files.
* Return only missing or underrepresented context.
* Prefer precision over recall.
* Confidence must reflect actual certainty.
* If retrieved files appear sufficient:

  * return empty arrays
  * frontendImpact = false
  * confidence <= 0.30
* Do not blindly copy the original intent.
* The returned intent should represent only additional retrieval targets.
* Symbols should be returned only when strongly implied.
* Architectural roles are the primary mechanism for expanding retrieval coverage.

Example:

Original intent:

```json
{
    "action": "modify",
    "symbols": ["User"],
    "concepts": ["soft-delete"],
    "capabilities": ["delete-user"],
    "architecturalRoles": ["entity","repository","service","migration"],
    "changeImpactAreas": ["database","api"],
    "frontendImpact": false,
    "confidence": 0.93
}
```

Retrieved files:

* UserController.java
* UserService.java

Output:

```json
{
    "action": "modify",
    "symbols": ["User"],
    "concepts": ["soft-delete"],
    "capabilities": ["delete-user"],
    "architecturalRoles": ["repository","entity","migration"],
    "changeImpactAreas": ["database"],
    "frontendImpact": false,
    "confidence": 0.88
}
```

Example when no additional context is needed:

```json
{
    "action": "modify",
    "symbols": [],
    "concepts": [],
    "capabilities": [],
    "architecturalRoles": [],
    "changeImpactAreas": [],
    "frontendImpact": false,
    "confidence": 0.12
}
```

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}
