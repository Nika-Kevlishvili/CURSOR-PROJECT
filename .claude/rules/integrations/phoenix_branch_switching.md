---
description: "Phoenix branch switching (Rule PHOENIX-SWITCH.0): align Cursor-Project/Phoenix/* repos to origin/<branch> before env-sensitive code reads. Full procedure in SKILL."
paths:
  - "Cursor-Project/Phoenix/**"
  - ".claude/agents/phoenix-qa.md"
  - ".claude/agents/bug-validator.md"
  - ".claude/agents/cross-dependency-finder.md"
  - ".claude/agents/test-case-generator.md"
  - ".claude/commands/switch-phoenix-branches.md"
  - ".claude/commands/switch-phoenix-branches.ps1"
---

# Phoenix Branch Switching (Rule PHOENIX-SWITCH.0) — summary

**Full procedure:** `.claude/skills/phoenix-branch-switching/SKILL.md`

## Intent

Before reading Phoenix code for env-sensitive work (Q&A, bug validation, cross-dep, test cases), align every `Cursor-Project/Phoenix/*` repo to `origin/<branch>` via the sanctioned script. Phoenix remains **READ-ONLY** (Rule 0.8 Tier A).

## Branch mapping

| Env | `-Environment` | Branch |
|-----|---------------|--------|
| Dev / Dev2 / Test / PreProd / Prod / Experiments | lowercase | `origin/<env>` |

## Script

```powershell
pwsh -NoProfile -File .claude/commands/switch-phoenix-branches.ps1 -Environment <env>
```

Add `-ConfirmProd` for `prod` only after explicit user ack. Add `-DryRun` for planning.

## Exit codes

| Code | Agent action |
|------|-------------|
| `0` | Proceed |
| `2` | Proceed; flag mixed-state |
| `3` | **STOP** — user must fix connectivity |

## Key rules

- Resolve env first (`environment-resolver` / AskUserQuestion)
- All local Phoenix edits **discarded** on switch
- Subagent reuse: skip re-alignment when same env + exit `0` in session
- Prod: user ack before `-ConfirmProd`
- Does not apply to EnergoTS (Rule ENERGOTS.0)
- Violation is a **BLOCK**
