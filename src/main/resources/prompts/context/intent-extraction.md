Extract structured intent from the user request.

User request:
{{USER_REQUEST}}

Return exactly one JSON object. No markdown fences or prose.

Schema:
{
  "action": "create|modify|delete|investigate",
  "entities": ["EntityName"],
  "operations": ["operation phrase"],
  "layers": ["controller","service","repository","entity","migration","frontend"],
  "frontendImpact": true
}

Rules:
- entities are PascalCase class or domain names when identifiable
- layers reflect likely touch points for the change
- include migration in layers when schema, database, columns, tables, flyway, liquibase, sql, or persistence changes are implied
- include frontend in layers when UI, templates, html, js, css, pages, or client behavior may change
- frontendImpact true when UI or API consumers may change

# General context from project:

{{AGENTS_FILE}}

{{DESIGN_FILE}}