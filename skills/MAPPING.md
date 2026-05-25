# Mapping: current → composable

## architect

- Canonical base: `skills/agents/base/architect/SKILL.md`
- auto-mode delta absorbed into overlays:
  - mandatory questions + wait-for-answers -> `skills/agents/overlays/auto-mode/SKILL.md`
  - deep analysis protocol + consistency guardrails + alternatives -> `skills/agents/overlays/model-opus/SKILL.md`

## implement

- Canonical base: `skills/agents/base/implement/SKILL.md`
- auto-mode delta absorbed into overlays:
  - intent classifier before editing -> `skills/agents/overlays/auto-mode/SKILL.md`
  - dynamic question gate -> `skills/agents/overlays/auto-mode/SKILL.md`
  - alternatives trigger + anti-rush checklist + consistency pass + confidence gate -> `skills/agents/overlays/model-opus/SKILL.md`

## risk-scan

- Canonical base: `skills/agents/base/risk-scan/SKILL.md`
- auto-mode delta absorbed into overlays:
  - ask-first gate -> `skills/agents/overlays/auto-mode/SKILL.md`
  - evidence/alternatives/negative impact + deterministic order + full impact -> `skills/agents/overlays/model-opus/SKILL.md`

## Conflict resolution

- Fixed order: `base -> mode -> model`
- If rules collide:
  - base security/invariants are not relaxed
  - mode adjusts operation (questions, gates, format)
  - model adjusts depth and thresholds
