# State Format

Shared spec. Skills reference, no repeat.

## Files
- `@implementation/ARCHITECTURE.md` — frozen, ≤120 lines, no history, no rejected alternatives
- `@implementation/TASKS.md` — `- [ ] T<n> short name`, stable IDs, never renumber, no subtasks, no prose
- `@implementation/RISKS.md` — active only, ≤10, one line each, no resolved history

## Read priority
1. `@implementation/TASKS.md` always
2. `@implementation/ARCHITECTURE.md` only if scope: boundary | invariant | migration | auth | data contract
3. `@implementation/RISKS.md` only if scope touches active risk

## Update rules
Allowed:
- mark task complete
- append next stable ID task
- remove resolved risk
- append unresolved risk
- append architecture constraint (only if missing invariant proven)

Forbidden:
- reformat / reorganize / rewrite sections
- expand descriptions
- add resolved history
- renumber

## Context budget (default)
- exact-symbol search first, no full repo scan
- max 6 files / 900 lines read
- file lists / `rg --files-with-matches` free
- exceed only with one-line reason
