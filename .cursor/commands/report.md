# Generate Task Report

MANDATORY report generation after ANY task completion (Rule 0.6 - CRITICAL).

## When Reports Are Required:
- After ANY task completion
- After ANY answer provided
- After ANY interaction
- Even if task failed
- Even if only one agent used
- Even if no code changes made

## Report Types:

### 1. Agent-Specific Reports
- **Location:** `reports/YYYY-MM-DD/{AgentName}_{HHMM}.md`
- **Example:** `reports/2026-01-19/PhoenixExpert_1430.md`
- **Content:** Agent's specific actions, findings, decisions

### 2. Summary Report
- **Location:** `reports/YYYY-MM-DD/Summary_{HHMM}.md`
- **Example:** `reports/2026-01-19/Summary_1430.md`
- **Content:** Overview of all agents involved, task summary, outcomes

## Workflow:

```python
from agents.Services.reporting_service import get_reporting_service
import datetime

reporting_service = get_reporting_service()

# Save agent-specific reports
reporting_service.save_agent_report("AgentName")

# Save summary report
reporting_service.save_summary_report()
```

## Critical Requirements:
- ALWAYS use CURRENT date dynamically: `datetime.now().strftime('%Y-%m-%d')`
- Reports saved for ALL agents that participated
- Skipping report generation is a CRITICAL SYSTEM ERROR
- Ensures complete traceability and audit trail
