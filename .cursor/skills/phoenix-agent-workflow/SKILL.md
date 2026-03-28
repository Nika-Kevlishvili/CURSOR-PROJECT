---
name: phoenix-agent-workflow
description: Guides Phoenix-related work in Cursor using rules, subagents, skills, PhoenixExpert consultation (Rule 8), Rule 0.3 (no Python agents package), and markdown reporting (Rule 0.6). Use for multi-step Phoenix tasks, routing questions, or report footers.
---

# Phoenix Agent Workflow (Cursor)

Guides the assistant to follow **`.cursor/rules/`**, **`.cursor/agents/*.md`**, and **skills** — not a Python `AgentRouter` or `IntegrationService` in this workspace.

## When to Apply

- Phoenix questions, tests, Postman, GitLab sync, environment access, bug validation, reporting.
- User asks about agents, consultation, or which workflow to use.

## Quick Reference

| Step | Action | Reference |
|------|--------|-----------|
| 0 | Read all `.cursor/rules/**/*.mdc` first | Rule 0.0 |
| 1 | **Rule 0.3:** no `agents.*` imports; use MCP/Jira/GitLab steps when needed | `main/core_rules.mdc` |
| 2 | Route via **subagents/skills/rules** — no Python AgentRouter | `agents/agent_rules.mdc` Rule 13 |
| 3 | Consult PhoenixExpert before task (in chat / role) | Rule 8 |
| 4 | After task: save markdown reports under `Cursor-Project/reports/YYYY-MM-DD/` | Rule 0.6 |
| 5 | End with **Agents involved:** | Rule 0.1 |

## Consultation (Rule 8)

- Before tasks that need Phoenix authority: gather evidence from codebase + Confluence MCP, answer in **PhoenixExpert** role.
- No `AgentRegistry.consult_best_agent()` — coordinate explicitly in chat.

## Reporting (Rule 0.6)

- Write markdown files with file tools; no `get_reporting_service()`.

## Collaboration Patterns (chat)

- **Tests:** consult PhoenixExpert → follow test-runner / energo-ts workflows.
- **Postman:** PostmanCollectionGenerator role → PhoenixExpert for APIs.
- **GitLab:** read-only sync per `integrations/git_sync_workflow.mdc`.
- **Environment:** `.cursor/agents/environment-access.md` + browser/MCP.
- **Bug validation:** Rule 32 in `workflows/workflow_rules.mdc` (no `get_bug_finder_agent`).

## Agent layout (Rule 34)

- **`Cursor-Project/agents/`** Python package is **not** in this workspace.
- Cursor roles live in **`.cursor/agents/*.md`** plus rules and skills.

## Output

- State expert at start when applicable.
- End with **Agents involved:** (Rule 0.1).
- On-disk text in English (Rule 0.7).

## Rules index

- **`.cursor/rules/main/phoenix.mdc`** — start here for file paths.
