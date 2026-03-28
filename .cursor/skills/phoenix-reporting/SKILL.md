---
name: phoenix-reporting
description: Generates and saves agent reports and summary reports after every task to Cursor-Project/reports/YYYY-MM-DD with correct naming. Use when a task completes, when the user asks for a report, or when applying Rule 0.6.
---

# Phoenix Reporting

Ensures reports are generated after every task (Rule 0.6): agent-specific report + summary, in the correct folder with current date and naming.

## When to Apply

- After any task completion (success or failure).
- After any answer or interaction that involved agents or tools.
- User asks to generate or save a report.
- Rule 0.6 applies: reports required even for a single role, no code changes, or Q&A only.

## Report Types

1. **Agent-specific:** `Cursor-Project/reports/YYYY-MM-DD/{AgentName}_{HHMM}.md`
2. **Summary:** `Cursor-Project/reports/YYYY-MM-DD/Summary_{HHMM}.md`

Use **today’s date** for `YYYY-MM-DD` (project root: `Cursor-Project/reports/`, not the git workspace root alone).

## Workflow (this workspace)

There is **no** Python `ReportingService`. Use editor/file tools:

1. Create the date folder if needed: `Cursor-Project/reports/YYYY-MM-DD/`.
2. Write one `.md` per participating agent/role (`{AgentName}_{HHMM}.md`).
3. Write `Summary_{HHMM}.md`.
4. Content in **English** for on-disk files (Rule 0.7).

## Requirements

- Skipping report generation when Rule 0.6 applies is a critical violation.
- Command reference: `.cursor/commands/report.md`.

## Naming Summary

| Type | Pattern | Example |
|------|---------|---------|
| Agent report | `{AgentName}_{HHMM}.md` | `PhoenixExpert_1430.md` |
| Summary | `Summary_{HHMM}.md` | `Summary_1430.md` |
| Bug validation | `BugValidation_[DescriptiveName].md` | `BugValidation_LoginFlow.md` |
