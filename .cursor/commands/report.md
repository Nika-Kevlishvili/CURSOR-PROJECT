# Generate Task Report

MANDATORY report generation after ANY task completion (Rule 0.6 — CRITICAL).

## When reports are required

- After ANY task completion, answer, or interaction
- Even if the task failed, only one role was used, or no code changed

## Report types

### 1. Agent-specific reports

- **Path:** `Cursor-Project/reports/YYYY-MM-DD/{AgentName}_{HHMM}.md`
- **Example:** `Cursor-Project/reports/2026-03-28/PhoenixExpert_1430.md`
- **Content:** Actions, findings, decisions for that agent/role

### 2. Summary report

- **Path:** `Cursor-Project/reports/YYYY-MM-DD/Summary_{HHMM}.md`
- **Content:** Overview, participants, outcomes

## Workflow (this workspace)

There is **no** Python `ReportingService`. Use editor/file tools:

1. Use **today’s date** for the folder name (`YYYY-MM-DD`).
2. Write one markdown file per agent involved + one summary.
3. Do **not** skip reports — Rule 0.6.

See also: **phoenix-reporting** skill and `.cursor/agents/report-generator.md`.
