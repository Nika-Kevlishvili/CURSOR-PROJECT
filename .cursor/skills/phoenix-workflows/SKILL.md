---
name: phoenix-workflows
description: Maps user intent to Phoenix commands and workflows -- bug validation, database queries, file organization, Jira bugs, sync, cross-deps, test cases, Playwright, HandsOff. Use when user asks how to run a workflow, which command to use, where to save files, or needs database/bug/test guidance.
---

# Phoenix Workflows and Commands

Maps user requests to the correct command or workflow. Commands live in `.cursor/commands/`.

## When to Apply

- User asks how to run a specific workflow or which command to use
- User asks where to save a file, report, user story, or test case
- User wants to query a database, validate a bug, generate test cases, or run Playwright
- User mentions a command by name (sync, bug-validate, hands-off, etc.)

## Command Map

| User intent | Command | Key detail |
|-------------|---------|------------|
| Phoenix question | `/phoenix` | Route to PhoenixExpert (Rule 0.2) |
| Consult before task | `/consult` | PhoenixExpert approval required |
| Generate report | `/report` | Save to `Cursor-Project/reports/YYYY-MM-DD/` |
| Validate a bug | `/bug-validate` | BugFinderAgent: Confluence first, then codebase. READ-ONLY |
| Jira bug (Experiments only) | `/jira-bug` | Template: Summary, Description, Steps, Expected, Actual. NOT Phoenix delivery |
| Production data analysis | `/production-data-reader` | ProductionDataReaderAgent, Prod DB readonly |
| Git sync Phoenix repos | `/sync` | Fetch/update/checkout from GitLab. READ-ONLY |
| Cross-dependencies | `/cross-dependency-finder` | What could break. Rule 35a: merge lookup first |
| Generate test cases | `/test-case-generate` | Cross-dep finder FIRST (Rule 35), then test-case-generator |
| Run Playwright tests | `/energo-ts-run` | From EnergoTS, cursor branch only |
| Full HandsOff flow | `/hands-off` | Jira -> cross-deps -> test cases -> Playwright -> run -> report -> Slack |

## File Organization (Rule 31)

| Content | Directory |
|---------|-----------|
| Agent code | `agents/` (in subdirectories) |
| Config files | `config/` |
| Documentation | `docs/` |
| User stories/flows | `User story/` (mandatory) |
| Reports | `Cursor-Project/reports/YYYY-MM-DD/` (current date) |
| Test cases | `test_cases/Objects/` or `test_cases/Flows/` |
| Postman collections | `postman/` |

## Database Queries (Rule DB.0)

Use the EXACT environment requested. Connect first via MCP, then query:
- Dev (10.236.20.21), Dev2 (10.236.20.22), Test (10.236.20.24), PreProd (10.236.20.76), Prod (10.236.20.78, readonly)
- Credentials in `database_workflow.mdc`. NEVER log passwords.
- Standard patterns for contracts by POD: see `database_workflow.mdc` Rule DB.2.

## Bug Validation (Rule 32)

Route to bug-validator subagent. Workflow: (1) Confluence via MCP, (2) codebase search, (3) combined analysis, (4) report to `reports/YYYY-MM-DD/BugValidation_*.md`. READ-ONLY, no code fixes.

Full details: `.cursor/commands/bug-validate.md`, `.cursor/rules/workflow_rules.mdc`.
