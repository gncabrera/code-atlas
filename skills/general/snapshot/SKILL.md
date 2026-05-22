---
name: snapshot
description: >
  Create compact handoff snapshot for model/chat switch.
---

/caveman ultra

# snapshot

## Role
Create handoff snapshot only. No new analysis, no implementation, no speculation.

## Trigger (manual only)
Run only when:
- switching model/chat
- context is heavy
- before closing long feature chat

## Source of truth
Read only:
- `@implementation/ARCHITECTURE.md`
- `@implementation/TASKS.md`
- `@implementation/RISKS.md`

## Compression rules
Keep only current truth:
- invariants and boundaries
- completed tasks (short)
- next up to 3 pending tasks
- active unresolved risks
- open decisions/unknowns

Drop:
- resolved history
- abandoned approaches
- implementation narrative
- duplicated risks/details

## Output
Write `@implementation/STATE_SNAPSHOT.md` in markdown only.
Fixed section order:
1. invariants
2. done
3. next
4. active-risks
5. open-decisions
Hard limit: 120-180 lines.
No surrounding chat prose.
