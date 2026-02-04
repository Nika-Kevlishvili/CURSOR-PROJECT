---
name: phoenix-reporting
description: Generates and saves agent reports and summary reports after every task to reports/YYYY-MM-DD with correct naming. Use when a task completes, when the user asks for a report, or when applying Rule 0.6.
---

# Phoenix Reporting

Ensures reports are generated after every task (Rule 0.6): agent-specific report + summary, in the correct folder with current date and naming.

## When to Apply

- After any task completion (success or failure).
- After any answer or interaction that involved agents or tools.
- User asks to generate or save a report.
- Rule 0.6 applies: reports required even for single agent, no code changes, or Q&A only.

## Report Types

1. **Agent-specific:** `reports/YYYY-MM-DD/{AgentName}_{HHMM}.md`
   - Example: `PhoenixExpert_1430.md`, `BugFinderAgent_1600.md`
   - Content: that agent's actions, findings, decisions.

2. **Summary:** `reports/YYYY-MM-DD/Summary_{HHMM}.md`
   - Content: all agents involved, task summary, outcomes.

Base path is project root: `Cursor-Project/reports/YYYY-MM-DD/`. Use **current** date: `datetime.now().strftime('%Y-%m-%d')`.

## Workflow

```python
from agents.Services.reporting_service import get_reporting_service
import datetime

reporting_service = get_reporting_service()
reporting_service.save_agent_report("AgentName")  # for each agent involved
reporting_service.save_summary_report()
```

- Call after task completion.
- Save one report per participating agent, then one summary.

## Requirements

- Date: always today (`datetime.now().strftime('%Y-%m-%d')`).
- Location: `Cursor-Project/reports/...` (project root), not workspace root.
- All participating agents must have a report; then add summary.
- Reports in English (Rule 0.7).
- Skipping report generation is a critical violation.

## Naming Summary

| Type | Pattern | Example |
|------|---------|---------|
| Agent report | `{AgentName}_{HHMM}.md` | `PhoenixExpert_1430.md` |
| Summary | `Summary_{HHMM}.md` | `Summary_1430.md` |
| Bug validation | `BugValidation_[DescriptiveName].md` | `BugValidation_LoginFlow.md` |

Command reference: `.cursor/commands/report.md`.
