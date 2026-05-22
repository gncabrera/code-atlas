# Mapping actual -> composable

## architect

- Base canónico: `skills/agents/base/architect/SKILL.md`
- Delta auto-mode absorbido en overlays:
  - preguntas obligatorias + wait-for-answers -> `skills/agents/overlays/auto-mode/SKILL.md`
  - protocolo de análisis profundo + consistency guardrails + alternativas -> `skills/agents/overlays/model-opus/SKILL.md`

## implement

- Base canónico: `skills/agents/base/implement/SKILL.md`
- Delta auto-mode absorbido en overlays:
  - intent classifier antes de editar -> `skills/agents/overlays/auto-mode/SKILL.md`
  - question gate dinámica -> `skills/agents/overlays/auto-mode/SKILL.md`
  - alternatives trigger + anti-rush checklist + consistency pass + confidence gate -> `skills/agents/overlays/model-opus/SKILL.md`

## risk-scan

- Base canónico: `skills/agents/base/risk-scan/SKILL.md`
- Delta auto-mode absorbido en overlays:
  - ask-first gate -> `skills/agents/overlays/auto-mode/SKILL.md`
  - evidencia/alternativas/impacto negativo + orden determinístico + impacto completo -> `skills/agents/overlays/model-opus/SKILL.md`

## Resolución de conflictos

- Orden fijo: `base -> mode -> model`
- Si regla colisiona:
  - seguridad/invariantes base no se relaja
  - mode ajusta operación (preguntas, gates, formato)
  - model ajusta profundidad y umbrales
