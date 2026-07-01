# Agents in this workspace: Cursor-first model

**Full file/output paths:** [`AGENT_SUBAGENT_MAP.md`](AGENT_SUBAGENT_MAP.md).

## Current state (authoritative)

| Layer | Location | Role |
|-------|-----------|------|
| **Rules** | `.cursor/rules/**/*.mdc` | Mandatory constraints, workflows (Rule 32, 35, 35a, DB, Git sync, HandsOff, etc.) |
| **Subagents** | `.cursor/agents/*.md` | Delegation specs: how to run a role in Cursor (Phoenix Q&A, bug validation, git-sync, …) |
| **Skills** | `.cursor/skills/**/SKILL.md` | When/how to trigger workflows from the main agent |
| **Commands** | `.cursor/commands/*.md` (+ `.ps1` where present) | Slash / `!` command procedures |
| **MCP** | Jira, Confluence, PostgreSQL*, Slack, … | External read/query integrations (with safety allowlists) |
| **Reports** | `Cursor-Project/reports/<area>/YYYY/<english-month>/<DD>/*.md` per **`reports/README.md`** | Rule 0.6 (markdown files, not a Python service) |

The **`Cursor-Project/agents/`** Python package (Main/Support/Core/Adapters/Services/Utils, `AgentRouter`, `IntegrationService`, `ReportingService`, `get_*_agent()`) is **removed** from this workspace. Do **not** import `agents.*` in Cursor chat workflows.

## Rule 0.3 (split)

- **Automation elsewhere** that still defines **IntegrationService**: call `update_before_task()` where required by that codebase.
- **This workspace (Cursor chat):** follow MCP/Jira/GitLab steps from rules; no Python IntegrationService.

## Historical docs

Files such as **`AGENTS_README.md`**, **`BUG_FINDER_AGENT.md`**, **`EnergoTSTestAgent_README.md`**, **`GITLAB_UPDATE_AGENT.md`**, and similar describe the **old Python layout** where applicable. Behavior for day-to-day Cursor use is defined by **`.cursor/`**, **`Cursor-Project/reports/README.md`**, and this section.

## Adding a new “agent” today

1. Add **`.cursor/agents/<name>.md`** with role, triggers, and governing rules.
2. Register in **`.cursor/agents/README.md`** and **`docs/CURSOR_SUBAGENTS.md`** if user-facing.
3. Add or extend a **skill** under `.cursor/skills/` when discovery/trigger text helps.
4. Cite rules under `.cursor/rules/` (commands registry Rule 38 if a new command is added).

This keeps Phoenix-centric consultation, read-only safety, and reporting aligned without a Python agent tree.
