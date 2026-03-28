# Cursor project health assessment (rules, subagents, skills)

**Date:** 2026-03-28  
**Scope:** `.cursor/rules`, `.cursor/agents`, `.cursor/skills`, `.cursor/commands`, hooks; impact of removed `Cursor-Project/agents/`.

## Summary verdict

| Area | Status | Notes |
|------|--------|--------|
| **Chat / Composer workflows** | **Good** | `alwaysApply` rules were aligned with a **no-Python-agents** workspace (`core_rules`, `workflow_rules` Rule 32, `agent_rules`, `production_data_reader`, `safety_rules`, `phoenix` index, `file_organization`, `hands-off` command). |
| **Structural coverage** | **Strong** | 13 rule files, 17 subagent specs, 12 skills, many commands + PowerShell helpers, hooks for Phoenix/EnergoTS/Confluence safety. |
| **Consistency after agent removal** | **Mixed** | No runtime break for Cursor itself; **documentation drift** in subagents, skills, commands, and `Cursor-Project/docs/` still cites Python paths and `from agents...`. |
| **Python examples in repo** | **Broken** | `Cursor-Project/examples/*.py` that imported `agents` will **ImportError** if executed. |

**Overall:** The setup remains **fit for purpose** for **chat-driven** work. Nothing “crashes” Cursor. What changed is **single source of truth**: authoritative behavior is now **`.cursor/rules` + MCP + tools**, not `Cursor-Project/agents/`. Stale references are a **maintainability / confusion** issue, not a hard failure.

## What is in good shape

1. **Rules layering:** Critical workflows (bug validation, DB envs, HandsOff lessons, EnergoTS branch lock, test case layout, git sync doc) are explicit and mostly self-contained in `.mdc`.
2. **Subagent catalog:** Broad coverage (cross-deps, test cases, Playwright, Jira bug, prod DB reader, git-sync, hands-off, etc.) gives clear delegation targets for the Task tool / orchestration.
3. **Skills:** Map common triggers (reporting, database, bug validation, EnergoTS run) to the right rules and subagents.
4. **Hooks:** Focused safety (no Confluence writes, Phoenix edit warnings, EnergoTS branch lock) — no dependency on the deleted Python package.
5. **Post-removal rule edits:** Core contradictions (mandatory `rules_loader`, `reporting_service`, `AgentRouter` as Python-only) were reduced in **always-applied** files so the model is not instructed to run non-existent code for every turn.

## What did **not** break (functionally)

- Opening the workspace, applying rules, using slash commands (`.md` content).
- MCP usage (Jira, Confluence, Postgres, Slack) as described in rules.
- EnergoTS / Phoenix path tiers (Rule 0.8) and branch-lock rules.
- PowerShell command scripts under `.cursor/commands/*.ps1` (independent of Python agents).

## Where things are **weaker** or misleading now

1. **`.cursor/agents/*.md`** — Several files still say “Map to … `Cursor-Project/agents/.../*.py`” and show `from agents...`. The **workflow text** is still usable; the **file paths and Python snippets** are **obsolete**.
2. **`.cursor/skills/*.md`** — `phoenix-agent-workflow`, `phoenix-bug-validation`, `phoenix-reporting`, `production-data-reader`, `phoenix-commands` still embed Python import examples; skill **intent** is fine, **examples** are stale.
3. **`.cursor/commands/`** — e.g. `energo-ts-test.md`, `report.md` still show Python `from agents...`.
4. **`Cursor-Project/docs/`** — AGENTS_README, GLOBAL_RULES_AND_ROUTING, RULES_LOADING_SYSTEM, etc. describe the **old** two-layer Python + Cursor architecture.
5. **Historical reports** under `Cursor-Project/reports/**` — mention removed files; harmless archive noise.

## Risk level

- **Low** for day-to-day Cursor AI use if the model follows updated **alwaysApply** rules.
- **Medium** if you or CI **run** old Python examples or copy-paste snippets from stale docs/skills expecting imports to work.
- **Medium** for onboarding: new readers may think Python agents are still required.

## Recommended follow-ups (optional, prioritized)

1. **P1 — Subagents:** Strip or replace Python path/import lines in `.cursor/agents/*.md` and `.cursor/agents/README.md` with “chat/MCP implementation only.”
2. **P2 — Skills + commands:** Same for `phoenix-agent-workflow`, `phoenix-bug-validation`, `phoenix-reporting`, `production-data-reader`, `phoenix-commands`, `energo-ts-test.md`, `report.md`.
3. **P3 — Docs:** Add a short banner to `AGENTS_README.md` / `CURSOR_SUBAGENTS.md`: Python package removed; Cursor layer is authoritative — or archive old docs.
4. **P3 — Examples:** Delete, quarantine, or rewrite `Cursor-Project/examples/*.py` that import `agents`.

## Conclusion

The project’s **Cursor configuration is still solid and usable**. Removing `Cursor-Project/agents/` did **not** invalidate the **intent** of rules and subagents; it removed **only** the programmatic Python implementation. The main gap is **consistency of secondary docs** (subagents, skills, commands, `docs/`, examples). Cleaning those removes confusion and completes the migration to a **chat-first** setup.
