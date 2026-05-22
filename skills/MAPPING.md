# Mapping actual -> composable

## architect

- Base canónico: `agent-skills/base/architect/SKILL.md`
- Delta auto-mode absorbido en overlays:
  - preguntas obligatorias + wait-for-answers -> `agent-skills/overlays/auto-mode/SKILL.md`
  - protocolo de análisis profundo + consistency guardrails + alternativas -> `agent-skills/overlays/model-opus/SKILL.md`

## implement

- Base canónico: `agent-skills/base/implement/SKILL.md`
- Delta auto-mode absorbido en overlays:
  - intent classifier antes de editar -> `agent-skills/overlays/auto-mode/SKILL.md`
  - question gate dinámica -> `agent-skills/overlays/auto-mode/SKILL.md`
  - alternatives trigger + anti-rush checklist + consistency pass + confidence gate -> `agent-skills/overlays/model-opus/SKILL.md`

## risk-scan

- Base canónico: `agent-skills/base/risk-scan/SKILL.md`
- Delta auto-mode absorbido en overlays:
  - ask-first gate -> `agent-skills/overlays/auto-mode/SKILL.md`
  - evidencia/alternativas/impacto negativo + orden determinístico + impacto completo -> `agent-skills/overlays/model-opus/SKILL.md`

## Resolución de conflictos

- Orden fijo: `base -> mode -> model`
- Si regla colisiona:
  - seguridad/invariantes base no se relaja
  - mode ajusta operación (preguntas, gates, formato)
  - model ajusta profundidad y umbrales
