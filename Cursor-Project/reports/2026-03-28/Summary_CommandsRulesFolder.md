# Summary — Command rules in `commands/` folder

**Date:** 2026-03-28  
**Request:** Extract command-related rules to a separate place.

## What changed

- **New:** `.cursor/rules/commands/commands_rules.mdc` (`alwaysApply: true`) — **Rule 36** (EnergoTS Playwright **run** / energo-ts-run) and **Rule 37** (**HandsOff** / `/HandsOff` / `!HandsOff`).
- **Updated:** `.cursor/rules/workflows/workflow_rules.mdc` — Rules 36–37 **removed** from this file; replaced with a short cross-reference to `commands_rules.mdc`. Rules **3, 32, 33, 35, 35a** stay here.
- **Updated:** `.cursor/rules/main/core_rules.mdc` Rule 0.0 — thematic list includes **`commands/`**.
- **Updated:** `.cursor/rules/main/phoenix.mdc`, `.cursor/rules/README.md`, `RULES_LOADING_SYSTEM.md`, `hands-off.md` (command + agent), `hands-off` command intro, `handsoff_playwright_report.mdc`, `phoenix-commands` skill, `energo-ts-run` skill + command, `energo-ts-run.md`.

Rule numbers **36** and **37** are **unchanged** for external references; only the **file path** moved.

**Agents involved:** PhoenixExpert, Reporting (Rule 0.6)
