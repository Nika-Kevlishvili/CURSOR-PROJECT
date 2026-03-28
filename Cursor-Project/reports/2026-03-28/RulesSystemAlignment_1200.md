# Rules vs system alignment audit

**Date:** 2026-03-28  
**Scope:** `.cursor/rules/**/*.mdc`, `.cursor/commands/*.md`, `.cursor/hooks.json`, cross-links to agents/skills.

## Summary scorecard

| Area | Fit | Notes |
|------|-----|--------|
| Command registry (Rule 38) | Strong | Registry rows match existing `.cursor/commands/*.md` files checked. |
| Hooks (safety / EnergoTS / Phoenix / Confluence / DB / push) | Strong | `hooks.json` aligns with `safety_rules`, `energots_branch_lock`, JIRA.0 intent. |
| Integrations (DB, Git sync, prod reader, Jira bug) | Strong | Dedicated `.mdc` files; MCP server names in workspace match workflow text. |
| Path tiers (0.8 / 0.8.1) | Strong | Consistent with hook-based Phoenix protection and EnergoTS test-only write rule. |
| Folder naming & canonical paths | Weak | Docs describe `01-main/` … `07-integrations/` but files live under `main/`, `safety/`, etc. |
| Rule 0.0 “read all subfolders” | Medium | Subfolder list in `core_rules.mdc` uses numbered names that do not match disk layout. |
| Rule 0.2 (Phoenix + tools) | Tension | Forbids direct tool use for Phoenix; other rules require code + Confluence evidence—executor must interpret “as PhoenixExpert” not “no tools.” |
| Legacy references | Medium | `safety_rules` IntegrationService for GitLab/Jira; `agent_rules` AgentRouter patterns; `workflow_rules` `codebase_search()` tool name. |
| Skills vs rules | Weak (one skill) | `phoenix-reporting` SKILL still shows Python `ReportingService` though package was removed. |

## Detailed findings

### 1. Broken or misleading paths

- `.cursor/rules/README.md` line 15: **Start here** points to `.cursor/rules/01-main/phoenix.mdc` — **that path does not exist** (actual index: `.cursor/rules/main/phoenix.mdc`).
- `.cursor/agents/README.md` and `.cursor/skills/README.md` reference `01-main/phoenix.mdc` — same issue.
- `main/phoenix.mdc` thematic table lists folders as `01-main/`, `02-safety/`, … while **canonical file paths** in the same file correctly use `main/`, `agents/`, etc. Mixed messaging.

### 2. Rule 0.0 vs repository layout

- `main/core_rules.mdc` Rule 0.0 tells the model to load subfolders including `01-main/`, `02-safety/`, … — **those directory names are not present**; only `main/`, `safety/`, `workflows/`, etc. exist. Functionally Cursor still loads all `.mdc` files recursively, but the instruction is inaccurate and may confuse maintainers.

### 3. Internal consistency

- **Command registry** in `commands_rules.mdc` is aligned with on-disk commands and notes agents without commands (database-query, postman-collection, etc.).
- **Git sync** scope (Phoenix only, not EnergoTS) is repeated consistently in `git_sync_workflow` and `energots_branch_lock`.
- **No `Cursor-Project/agents/`** is stated in `core_rules`, `commands_rules`, and `agent_rules` Rule 34 — consistent with git status.

### 4. Security / secrets in rules (recommendation only)

- Database and Git readonly token strings appear inside integration rules that are loaded into context. **Risk:** broad exposure in logs, screenshots, or shared chats. Prefer env/MCP-only configuration and rule text that references variable names, not literal secrets.

### 5. Skill drift

- `.cursor/skills/phoenix-reporting/SKILL.md` workflow block still imports `agents.Services.reporting_service` — **incompatible** with current workspace (use markdown files under `Cursor-Project/reports/YYYY-MM-DD/` per Rule 0.6, as stated later in the same skill).

## Recommendations (priority)

1. **Unify folder narrative:** Either rename dirs to `01-main/` … or update `README.md`, `phoenix.mdc` table, `core_rules` Rule 0.0, and agent/skill READMEs to **only** use real paths (`main/`, `safety/`, …).
2. **Clarify Rule 0.2:** Add one sentence that Phoenix answers must be **produced under PhoenixExpert responsibility** using allowed read-only tools (codebase + Confluence MCP), not “no tools.”
3. **Trim legacy lines** in `safety_rules` / `agent_rules` collaboration table (AgentRouter) or mark them “historical / if Python stack restored.”
4. **Refresh `phoenix-reporting` SKILL** to match Rule 0.6 (no Python reporting service).

## Verdict

Rules **functionally fit** the current Cursor + hooks + commands + MCP setup. Main gaps are **documentation/path drift**, **minor legacy text**, and **one outdated skill**—not a fundamental mismatch of workflows.
