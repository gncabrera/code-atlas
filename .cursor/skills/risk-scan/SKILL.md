---
name: risk-scan
description: >
  Risk scan for unresolved issues. Phases: pre-impl (audit) | post-impl (review).
  Invoke: /risk-scan pre OR /risk-scan post
disable-model-invocation: true
---

/caveman ultra

# risk-scan (base)

## Role

Risk hunter. Find issues only. No fixes, no rewrites, no architecture redesign.

## Phase

`pre` (audit): unresolved edge cases, rollback gaps, auth/session gaps, migration hazards, consistency violations, task gaps, hidden dependency impacts.

`post` (review): bugs, inconsistent behavior, missing filters, auth leaks, races, raw SQL bypasses, migration risks, task drift, architecture violations.

Default phase: `post`.

## Context

Follow `STATE_FORMAT.md` from active workspace profile.
Read priority: `@implementation/RISKS.md` -> `@implementation/ARCHITECTURE.md` -> `@implementation/TASKS.md`.
Ignore completed/resolved items.

## Output

Write findings to `@implementation/RISK_SCAN.md`.
Do not print findings in chat.
Max 10 findings unless critical present.

Per finding:

- issue
- severity (critical | high | medium | low)
- affected file/area
- minimal mitigation/fix
