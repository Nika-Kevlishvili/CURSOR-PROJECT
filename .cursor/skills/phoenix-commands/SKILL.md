---
name: phoenix-commands
description: Maps user intent to Cursor commands and workflows (Phoenix query, consult, report, bug-validate, sync). Use when the user asks how to run a workflow or which command to use for Phoenix, consultation, reports, bug validation, or Git sync.
---

# Phoenix Commands and Workflows

Helps choose the right command or workflow for Phoenix-related tasks. Commands live in `.cursor/commands/`; rules in `.cursor/rules/`.

## When to Apply

- User asks how to ask Phoenix a question, consult, generate a report, validate a bug, or sync from GitLab.
- User mentions a command by name (phoenix, consult, report, bug-validate, sync).
- Need to align a request with the correct workflow.

## Command → Workflow

| User intent | Command / workflow | Rule / reference |
|-------------|--------------------|-------------------|
| Phoenix question (how/what/why, endpoints, logic) | **Phoenix** command → Route to PhoenixExpert (Rule 0.2) | phoenix.md |
| Before doing a task (validation, approval) | **Consult** → PhoenixExpert consultation (Rule 8) | consult.md |
| After any task | **Report** → Save agent + summary reports (Rule 0.6) | report.md |
| Validate a bug report | **Bug-validate** → BugFinderAgent (Rule 32) | bug-validate.md |
| Production data analysis (liability offsets, receivable history) | **Production-data-reader** → ProductionDataReaderAgent (Rule PDR.0) | production-data-reader.md, production_data_reader.mdc |
| Fetch/update/checkout Phoenix repos from GitLab | **Sync** → git_sync_workflow (read-only) | sync.md, git_sync_workflow.mdc |

## Phoenix (phoenix.md)

- **When:** Any Phoenix-related question.
- **Flow:** IntegrationService → Confluence (MCP, fresh) → Codebase → PhoenixExpert answer.
- **Output:** Start with "**Expert:** PhoenixExpert", end with "Agents involved: PhoenixExpert", save reports.

## Consult (consult.md)

- **When:** Before any task that affects Phoenix (tests, Postman, GitLab, env access, etc.).
- **Flow:** IntegrationService → Describe task → PhoenixExpert review → Approval required.
- **Output:** "Agents involved: PhoenixExpert, [others]". Consultation is binding (Rule 27).

## Report (report.md)

- **When:** After every task (Rule 0.6).
- **Flow:** `get_reporting_service().save_agent_report(...)` + `save_summary_report()`.
- **Location:** `Cursor-Project/reports/YYYY-MM-DD/` with current date.

## Bug-validate (bug-validate.md)

- **When:** User wants to validate or verify a bug report.
- **Flow:** IntegrationService → BugFinderAgent → Confluence → Codebase → analysis → report file.
- **Output:** Structured Bug Validation Analysis; "Agents involved: BugFinderAgent, PhoenixExpert".

## Production-data-reader (production-data-reader.md)

- **When:** User asks about production database data, liability offsets, receivable history, or how an entity was created.
- **Flow:** IntegrationService → Connect to PostgreSQLProd → Parse entity → Analyze → Generate step-by-step explanation.
- **Output:** Detailed analysis with offset sequence and creation process; "Agents involved: ProductionDataReaderAgent".

## Sync (sync.md)

- **When:** User wants to fetch/update/checkout Phoenix projects from GitLab.
- **Triggers:** `!sync`, `!update <branch>`, `!checkout <branch>`.
- **Rule:** Follow `.cursor/rules/git_sync_workflow.mdc` exactly; use git commands (no Python agent for sync). Read-only (fetch/checkout/merge only; no push).

## Summary

- Phoenix questions → Phoenix command + PhoenixExpert.
- Before task → Consult + PhoenixExpert approval.
- After task → Report (agent + summary).
- Bug check → Bug-validate + BugFinderAgent.
- Production data → Production-data-reader + ProductionDataReaderAgent.
- Git sync → Sync command + git_sync_workflow.mdc.

All commands assume rules are loaded first (Rule 0.0) from `.cursor/rules/`.
