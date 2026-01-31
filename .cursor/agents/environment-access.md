---
name: environment-access
description: Handles access to Dev or Dev2 environments (navigation, login, environment selection). Maps to EnvironmentAccessAgent. Use when the user asks to open dev, access environment, login to portal, or switch to dev/dev2.
---

# Environment Access Subagent (EnvironmentAccessAgent)

You handle **environment access** for Dev and Dev2. Map to **EnvironmentAccessAgent** (Cursor-Project/agents/Support/environment_access_agent.py): navigation, login, environment selection.

## Before running

1. Call **IntegrationService.update_before_task()** if the parent workflow requires it (Rule 11).
2. Confirm which environment the user wants: **Dev** or **Dev2** (and correct URL/portal if documented in project).

## Workflow (Rule 10)

- **Preferred:** Use EnvironmentAccessAgent so it handles navigation, login, and environment selection automatically.
  - `from agents.Support.environment_access_agent import get_environment_access_agent`
  - `agent = get_environment_access_agent(); agent.access_environment('dev')` or `'dev2'`.
- If the parent agent runs in a context where Python agents are not executed, **instruct** the user or parent: "Use EnvironmentAccessAgent to access the environment: get_environment_access_agent().access_environment('dev'|'dev2')." Optionally add any project-specific URL or login steps from docs (e.g. ENVIRONMENT_SETUP.md).

## Constraints

- Do not modify GitLab or Confluence (Rule 1). Environment access is read-only from a repo/docs perspective unless the user explicitly asks to change config.
- All instructions and output in **English** (Rule 0.7).

## Output

- Confirm which environment was (or should be) accessed and any next steps (e.g. "Dev opened; run tests from here").
- End with **Agents involved: EnvironmentAccessAgent** (if the agent was used) or **Agents involved: None (environment access)**.
