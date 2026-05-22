# Composable Skills (base -> mode -> model)

Objetivo: un solo set canónico, sin duplicar prompts completos por carpeta.

Orden de composición recomendado:

1. Base skill (`architect` | `implement` | `risk-scan`)
2. Mode overlay (`auto-mode`)
3. Model overlay (`model-opus`)

Invocación manual:

- `/architect /auto-mode /model-opus`
- `/implement /auto-mode /model-opus`
- `/risk-scan post /auto-mode /model-opus`

Regla de mantenimiento:

- Editar comportamiento estable en base.
- Editar comportamiento operativo en mode.
- Editar tuning específico del modelo en model.
- No volver a duplicar skill completo por variante.

## Install en proyecto destino

Scripts copian docs a `<TargetPath>/.cursor/` y skills a `<TargetPath>/.cursor/skills/`.

**Requisito:** `-TargetPath` obligatorio (raíz del proyecto destino). Abort si ya existe `.cursor/skills`, `STATE_FORMAT.md` o `WORKFLOW.md` bajo `.cursor/`.

**PowerShell:**

```powershell
.\scripts\install.ps1 -TargetPath C:\path\to\proyecto
```

**Bash:**

```bash
./scripts/install.sh -TargetPath /path/to/proyecto
# o
./scripts/install.sh /path/to/proyecto
```

**Origen → destino**

| Fuente (repo) | Destino |
|---------------|---------|
| `skills/agents/STATE_FORMAT.md` | `.cursor/STATE_FORMAT.md` |
| `WORKFLOW.md` | `.cursor/WORKFLOW.md` |
| `skills/general/{caveman,ask}` | `.cursor/skills/caveman/`, etc |
| `skills/agents/snapshot` | `.cursor/skills/snapshot/` |
| `skills/agents/base/{architect,implement,risk-scan}` | `.cursor/skills/architect/`, etc. |
| `skills/agents/mode-overlays/{auto-mode}` | `.cursor/skills/auto-mode/`, etc |
| `skills/agents/model-overlays/{model-opus}` | `.cursor/skills/model-opus/`, etc |

**Layout resultante:**

```
<proyecto>/.cursor/
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