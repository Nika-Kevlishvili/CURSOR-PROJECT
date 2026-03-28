# Summary — Agent path alignment (whole project) (2026-03-28)

## Request

User asked to fix **agent addresses** project-wide (not only partial docs): subagents, reports, examples, and historical agent docs should point to the real locations.

## Delivered

1. **`Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`** (new) — single table: every **`.cursor/agents/*.md`**, rules/skills/commands pointers, **`Cursor-Project/reports/`**, **`test_cases/`**, **`EnergoTS/tests/cursor/`**; explicit “no bare `reports/` without `Cursor-Project/`”.
2. **`Cursor-Project/reports/README.md`** — rewritten: Rule 0.6, no Python ReportingService, link to map.
3. **`Cursor-Project/examples/README.md`** (new) — legacy scripts vs Cursor workflows.
4. **Path fixes:** `.cursor/agents/README.md`, `CURSOR_SUBAGENTS.md`, `COMMANDS_REFERENCE.md`, `.cursor/commands/phoenix.md`, `.cursor/commands/hands-off.md`, `.cursor/skills/phoenix-commands/SKILL.md` — consistent **`Cursor-Project/reports/YYYY-MM-DD/`**.
5. **Doc banners** — 15+ `docs/*.md` agent-related files: top blockquote → subagent file(s) + **`AGENT_SUBAGENT_MAP.md`**; `BUG_FINDER_AGENT.md` report paths fixed.
6. **Examples:** every `*.py` that imported `agents` — **LEGACY** notice in module docstring + pointer to map / correct `.cursor/agents` role; `query_phoenix_expert_fixed.py` notes removed `agents/` dir.
7. **Indexes:** `Cursor-Project/README.md`, `HISTORICAL_PYTHON_AGENTS_PACKAGE.md`, `AGENTS_COMPARISON_AND_ALIGNMENT.md`, `CURSOR_SUBAGENTS.md`, `.cursor/README.md`, `.cursor/agents/README.md` — link to **`AGENT_SUBAGENT_MAP.md`**.

## Agents involved

Documentation and path alignment (direct).
