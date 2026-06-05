Generate business concept index entries for the project file inventory.

File inventory (path: symbols):
{{FILE_INVENTORY}}

Return exactly one JSON object. No markdown fences or prose.

Schema:
{
  "concepts": [
    { "concept": "user", "files": ["UserService", "UserController"] }
  ]
}

Rules:
- concept is lowercase singular or domain phrase
- files lists related class or file stem names from the inventory only
