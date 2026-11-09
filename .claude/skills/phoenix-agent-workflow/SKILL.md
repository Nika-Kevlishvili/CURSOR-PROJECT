---
name: phoenix-agent-workflow
description: Guides Phoenix-related work in Claude Code using rules, subagents, skills, PhoenixExpert consultation (Rule 8), Rule 0.3 (no Python agents package). Persisted reports only for `/report` / `/feedback` / explicit save (Rule 0.6). Use for multi-step Phoenix tasks, routing questions, or report footers.
---

# Phoenix Agent Workflow (Claude Code)

Guides the assistant to follow **`.claude/rules/`**, **`.claude/agents/*.md`**, and **skills** — not a Python `AgentRouter` or `IntegrationService` in this workspace.

## When to Apply

- Phoenix questions, tests, Postman, environment access, bug validation, reporting.
- User asks about agents, consultation, or which workflow to use.

## Quick Reference

| Step | Action | Reference |
|------|--------|-----------|
| 0 | Read all `.claude/rules/**/*.md` first | Rule 0.0 |
| 1 | **Rule 0.3:** no `agents.*` imports; use MCP/Jira/GitLab steps when needed | `main/core_rules.md` |
| 2 | Route via **subagents/skills/rules** — no Python AgentRouter | `agents/agent_rules.md` Rule 13 |
| 3 | Consult PhoenixExpert before task (in chat / role) | Rule 8 |
| 3a | Align Phoenix repos to `origin/<env-branch>` via `.claude/commands/switch-phoenix-branches.ps1` before environment-sensitive Phoenix code reading (Q&A, bug validation, cross-dep, test cases). Local Phoenix edits are discarded; Phoenix files stay READ-ONLY (Rule 0.8 Tier A) | Rule PHOENIX-SWITCH.0 (`integrations/phoenix_branch_switching.md`) |
| 3b | **Jira ticket analysis** (analysis/triage/summary): if the issue links to Confluence, read those pages in the same response (MCP or Rule 43 REST; set `CONFLUENCE_WIKI_BASE` when the wiki host differs from `JIRA_BASE_URL`) | `evidence_only_project_answers.md` (*Jira ticket analysis — linked Confluence*); **Rule 44** `workflow_rules.md` |
| 3c | **PostgreSQL / live DB data:** read **`.claude/skills/phoenix-database/SKILL.md` Step 0** — user must name Dev/Dev2/Test/PreProd/Prod/Experiments **before** `psql-phoenix.py`; never infer from PDT/EnergoTS tests. No PostgreSQL MCP | Rule DB.0a, CONF.0, `integrations/database_workflow.md` |
| 4 | Save markdown under `Cursor-Project/reports/` only if the user runs **`/report`**, **`/feedback`**, or explicitly requests a file — layout **`reports/README.md`** (Rule 0.6) | Rule 0.6 |
| 5 | End with **Agents involved:** | Rule 0.1 |

## Consultation (Rule 8)

- Before tasks that need Phoenix authority: gather evidence from codebase + Confluence REST GET, answer in **PhoenixExpert** role.
- No `AgentRegistry.consult_best_agent()` — coordinate explicitly in chat.

## Reporting (Rule 0.6)

- Optional by default; write markdown with file tools only when the user runs **`/report`**, **`/feedback`**, or explicitly requests a save. Bug validation (Rule 32) is **chat-only** unless they ask to persist. No `get_reporting_service()`.

## Collaboration Patterns (chat)

- **Tests:** consult PhoenixExpert → follow test-runner / energo-ts workflows.
- **Postman:** PostmanCollectionGenerator role → PhoenixExpert for APIs.
- **Environment:** `.claude/agents/environment-access.md` + browser/MCP.
- **Bug validation:** Rule 32 in `workflows/workflow_rules.md` (no `get_bug_finder_agent`).

## Agent layout (Rule 34)

- **`Cursor-Project/agents/`** Python package is **not** in this workspace.
- Claude Code roles live in **`.claude/agents/*.md`** plus rules and skills.

## Output

- State expert at start when applicable.
- End with **Agents involved:** (Rule 0.1).
- On-disk text in English (Rule 0.7).

## Rules index

- **`.claude/rules/main/phoenix.md`** — start here for file paths.
