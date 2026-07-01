# Cursor workspace configuration (Qa AI Toolkit template)

This folder is at the **Git workspace root** (next to your project data folder, default `Project/`).

| Area | Entry |
|------|--------|
| **Rules** (thematic `.mdc`) | [rules/README.md](rules/README.md) |
| **Subagents** | [agents/README.md](agents/README.md) |
| **Skills** | [skills/README.md](skills/README.md) |
| **Commands** | `commands/*.md` |
| **Hooks** | `hooks.json` + `hooks/*.ps1` — app code protect, Confluence read-only, DB write confirm |
| **Validation** | `Project/scripts/validate-cursor-rules.ps1` |
| **Operating model** | [../Project/docs/CURSOR_OPERATING_MODEL.md](../Project/docs/CURSOR_OPERATING_MODEL.md) |

Deployed by **`Qa AI Toolkit/setup.ps1`**. Customize `QA_APP_CODE_GLOB` in `.env` for Tier A protection.
