Detect architectural patterns present in the project file inventory.

File inventory:
{{FILE_INVENTORY}}

Return exactly one JSON object. No markdown fences or prose.

Schema:
{
  "patterns": [
    { "pattern": "crud-resource", "files": ["UserController", "UserService"] }
  ]
}

Rules:
- pattern is kebab-case label (crud-resource, soft-delete, audit, rest-controller)
- files lists exemplar class names from inventory only
