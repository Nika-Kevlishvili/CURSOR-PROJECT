# Project path and link verification

**Date:** 2026-03-28  
**Scope:** Absolute/relative links and references across `.cursor/`, `Cursor-Project/README.md`, and key `Cursor-Project/docs/` entry points.

## Checks performed

1. **Stale folder aliases:** `01-main`, `07-integrations` — only remain in historical reports (`reports/2026-03-28/*_1200.md`); active `.md`/`.mdc` cleaned in prior sync.
2. **Broken relative link:** `Cursor-Project/README.md` pointed to `.cursor/README.md` (wrong base — file is not under `Cursor-Project/`). **Fixed:** link to **`../.cursor/README.md`**; added **`.cursor/README.md`** hub at workspace root.
3. **Removed Python package:** Scanned `.cursor` for `from agents.`, `Cursor-Project/agents/*.py`, `get_reporting_service`, etc. **Updated** all active subagents, commands (`report.md`, `energo-ts-test.md`), and skills that still assumed Python APIs.
4. **Docs:** `CURSOR_SUBAGENTS.md` — corrected rules path from `Cursor-Project/docs/` (`../../.cursor/rules/...`), removed “Python agents live in Cursor-Project/agents”, DB range DB.6. `RULES_LOADING_SYSTEM.md` — banner that Python loader is legacy.

## Intentionally unchanged (historical)

- **`Cursor-Project/docs/AGENTS_README.md`**, **`BUG_FINDER_AGENT.md`**, **`EnergoTSTestAgent_README.md`**, **`AGENT_REPORTING_GUIDE.md`**, **`GLOBAL_RULES_AND_ROUTING.md`**, **`Q&A_MODE_ASSESSMENT.md`**, etc. — still describe the old Python layout; they are **archival** unless you want a repo-wide doc purge.
- **Dated reports** under `Cursor-Project/reports/**` — snapshots; not rewritten.

## New canonical entry points

| From | Link / path |
|------|----------------|
| Workspace root | `.cursor/README.md` |
| `Cursor-Project/README.md` | `../.cursor/README.md` |
| `Cursor-Project/docs/CURSOR_SUBAGENTS.md` | `../../.cursor/rules/main/phoenix.mdc` for rules index |

## Residual risk

- Any **new** markdown under `docs/` or `examples/` may reintroduce `from agents...`; periodic grep on `Cursor-Project` (excluding `reports/`) recommended.
