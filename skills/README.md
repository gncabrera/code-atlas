# Composable Skills (base -> mode -> model)

Goal: one canonical set, without duplicating full prompts per folder.

Recommended composition order:

1. Base skill (`architect` | `implement` | `risk-scan`)
2. Mode overlay (`auto-mode`)
3. Model overlay (`model-opus`)

Manual invocation:

- `/architect /auto-mode /model-opus`
- `/implement /auto-mode /model-opus`
- `/risk-scan post /auto-mode /model-opus`

Maintenance rule:

- Edit stable behavior in base.
- Edit operational behavior in mode.
- Edit model-specific tuning in model.
- Do not duplicate a full skill per variant again.

## Install on target project

Scripts copy docs to `<TargetPath>/.cursor/` and skills to `<TargetPath>/.cursor/skills/`.

**Requirement:** `-TargetPath` required (target project root). Abort if `.cursor/skills`, `STATE_FORMAT.md`, or `WORKFLOW.md` already exists under `.cursor/`.

**PowerShell:**

```powershell
.\scripts\install.ps1 -TargetPath C:\path\to\project
```

**Bash:**

```bash
./scripts/install.sh -TargetPath /path/to/project
# or
./scripts/install.sh /path/to/project
```

**Source → destination**

| Source (repo) | Destination |
|---------------|-------------|
| `skills/agents/STATE_FORMAT.md` | `.cursor/STATE_FORMAT.md` |
| `WORKFLOW.md` | `.cursor/WORKFLOW.md` |
| `skills/general/{caveman,ask}` | `.cursor/skills/caveman/`, etc |
| `skills/agents/snapshot` | `.cursor/skills/snapshot/` |
| `skills/agents/base/{architect,implement,risk-scan}` | `.cursor/skills/architect/`, etc. |
| `skills/agents/mode-overlays/{auto-mode}` | `.cursor/skills/auto-mode/`, etc |
| `skills/agents/model-overlays/{model-opus}` | `.cursor/skills/model-opus/`, etc |

**Resulting layout:**

```
<project>/.cursor/
  STATE_FORMAT.md
  WORKFLOW.md
  skills/architect/SKILL.md
  skills/implement/SKILL.md
  skills/risk-scan/SKILL.md
  skills/caveman/SKILL.md
  skills/ask/SKILL.md
  skills/snapshot/SKILL.md
  skills/auto-mode/SKILL.md
  skills/model-opus/SKILL.md
  ...
```

# combined.txt
```powershell
Get-ChildItem -File -Recurse | ? { $_.FullName -notmatch '\\(\.git|node_modules|build|dist|target|bin|obj)\\' } | % { "`n# $($_.FullName)`n" + '```' + "$($_.Extension.TrimStart('.'))"; Get-Content $_.FullName; '```' } | Set-Content combined.txt -Encoding UTF8
```
