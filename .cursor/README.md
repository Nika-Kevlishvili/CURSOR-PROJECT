# Cursor workspace configuration

This folder is at the **Git workspace root** (next to `Cursor-Project/`).

| Area | Entry |
|------|--------|
| **Rules** (thematic `.mdc`) | [rules/README.md](rules/README.md) — index: [rules/main/phoenix.mdc](rules/main/phoenix.mdc) |
| **Subagents** | [agents/README.md](agents/README.md) |
| **Agent paths (canonical map)** | [../Cursor-Project/docs/AGENT_SUBAGENT_MAP.md](../Cursor-Project/docs/AGENT_SUBAGENT_MAP.md) |
| **Skills** | [skills/README.md](skills/README.md) |
| **Commands** | `commands/*.md` |
| **Hooks** | `hooks.json` + `hooks/*.ps1` — Phoenix protect, EnergoTS branch/ writes, Confluence read-only, DB write confirm, git push control |
| **Validation** | `validate-cursor-rules.ps1` + `validate-cursor-consistency.ps1` (+ `validate-manual-verification-links.ps1` via consistency) |
| **Operating model** | [../Cursor-Project/docs/CURSOR_OPERATING_MODEL.md](../Cursor-Project/docs/CURSOR_OPERATING_MODEL.md) — layers, workflows, reconciliation (Phases 1–3 complete) |

From `Cursor-Project/`, link here with e.g. `../.cursor/rules/README.md`.
