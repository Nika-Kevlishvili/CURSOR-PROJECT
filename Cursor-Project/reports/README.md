# Reports Folder

This folder contains activity reports for all agents.

## Report Structure

Reports are stored in separate folders by date:
- **Folder format:** `YYYY-MM-DD` (e.g., `2025-12-09`)
- **File format:** `{agent_name}_{hour}{minutes}.md` (e.g., `PhoenixExpert_1830.md`)

## Report Types

1. **Summary Report** (`Summary_{HHMM}.md`) - Overview of all agents
2. **Agent Reports** (`{agent_name}_{HHMM}.md`) - Detailed report for a specific agent

### Example Structure:
```
reports/
├── 2025-12-09/
│   ├── PhoenixExpert_1830.md
│   ├── TestAgent_1830.md
│   ├── GitLabUpdateAgent_1830.md
│   └── Summary_1830.md
└── 2025-12-10/
    ├── PhoenixExpert_0915.md
    └── Summary_0915.md
```

## What's Included in Reports

- **Completed Tasks** - What tasks the agent completed
- **Communication with Other Agents** - Which agents it communicated with
- **Information Sources** - Where it retrieved information from
- **Recent Activities** - Last 10 activities

## AI Assistant Response Reporting

When AI assistant responds to user questions, a report is automatically written:

```python
from agents.ai_response_logger import log_ai_response

# When using one expert
log_ai_response(
    user_query="How does customer endpoint work?",
    expert_name="PhoenixExpert",
    response_summary="Explained customer endpoint functionality"
)

# When using multiple agents
log_ai_response(
    user_query="Test execution",
    agents_used=["TestAgent", "PhoenixExpert"],
    response_summary="Test completed"
)
```

## Generating Reports

To generate reports, use:

```python
from agents.reporting_service import get_reporting_service

# Get reporting service
reporting_service = get_reporting_service()

# Save reports for all agents
reporting_service.save_all_reports()

# Save report for a specific agent
reporting_service.save_agent_report("PhoenixExpert")

# Save only summary report
reporting_service.save_summary_report()
```

## Reporting for Agents

Agents can report their activities:

```python
from agents.reporting_service import get_reporting_service

reporting_service = get_reporting_service()

# Report task execution
reporting_service.log_task_execution(
    agent_name="MyAgent",
    task="Test execution",
    task_type="testing",
    success=True,
    duration_ms=1234.5
)

# Report information source
reporting_service.log_information_source(
    agent_name="MyAgent",
    source_type="file",
    source_description="config.json",
    information="Configuration loaded"
)

# Report communication (automatically done via AgentRegistry)
# But you can also do it manually:
reporting_service.log_consultation(
    from_agent="MyAgent",
    to_agent="PhoenixExpert",
    query="How does X work?",
    success=True,
    duration_ms=567.8
)
```

