# Rules and docs synchronization

**Date:** 2026-03-28  
**Request:** User asked to synchronize everything (follow-up to rules alignment audit).

## Changes applied

| Area | Files updated |
|------|----------------|
| Rule 0.0 folder list | `.cursor/rules/main/core_rules.mdc` — subfolders now match disk: `main/`, `safety/`, `agents/`, `workflows/`, `commands/`, `workspace/`, `integrations/` |
| Rule 0.2 Phoenix | `core_rules.mdc` — PhoenixExpert role + read-only evidence gathering; explicit that `read_file` / grep / codebase search are allowed for reading |
| Thematic index table | `.cursor/rules/main/phoenix.mdc` — folder names aligned with actual paths |
| Entry README | `.cursor/rules/README.md` — Start here → `main/phoenix.mdc` |
| Agents README | `.cursor/agents/README.md` — `integrations/` rule paths, index link, report-generator row, DB rule range DB.0–DB.6 |
| Skills README | `.cursor/skills/README.md` — index path + phoenix-agent-workflow row |
| Reporting skill | `.cursor/skills/phoenix-reporting/SKILL.md` — markdown workflow only; removed Python `ReportingService` |
| Safety | `.cursor/rules/safety/safety_rules.mdc` — IntegrationService scoped to external automation; Rule 26 without AgentRouter |
| Agent patterns | `.cursor/rules/agents/agent_rules.mdc` — Patterns 3–5 aligned with no Python package |
| Bug validation Step 2 | `.cursor/rules/workflows/workflow_rules.mdc` — generic read-only search tools |
| Docs | `Cursor-Project/docs/RULES_LOADING_SYSTEM.md` — `main/` paths instead of `01-main/` |

## Not changed

- **Git `!sync` / Phoenix repos:** not executed (user message referred to documentation/rules sync in conversation context). Run `/sync` or `!sync` separately if GitLab alignment is required.
- **Historical audit reports** under `reports/2026-03-28/*_1200.md` left as-is (snapshot).

## Verification

- Grep: no remaining `01-main` or `07-integrations` in active `.md`/`.mdc` except prior audit markdown reports.
