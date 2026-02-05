---
name: phoenix-agent-workflow
description: Routes Phoenix project queries through AgentRouter, enforces PhoenixExpert consultation and IntegrationService before tasks, and triggers report generation. Use when handling Phoenix-related questions, multi-agent tasks, or when the user asks about agents, routing, consultation, or report generation.
---

# Phoenix Agent Workflow

Guides the agent to use the project's agent system correctly: routing, consultation, integration service, and reporting.

## When to Apply

- User asks about Phoenix (backend, endpoints, business logic, Confluence).
- Task involves tests, Postman, GitLab, environment access, or bug validation.
- User mentions consultation, routing, or "which agent".
- Any task that must follow Rule 0.0–0.6, Rule 8, Rule 11, Rule 13.

## Quick Reference

| Step | Action | Reference |
|------|--------|-----------|
| 0 | Read all rules from `.cursor/rules/` first | Rule 0.0 |
| 1 | Call `IntegrationService.update_before_task()` before executing task | Rule 11 |
| 2 | Route via `AgentRouter.route_query()` — do not pick agents manually | Rule 13 |
| 3 | Consult PhoenixExpert before task (all agents) | Rule 8 |
| 4 | After task: save agent report + summary report | Rule 0.6 |
| 5 | End response with "Agents involved: [names]" | Rule 0.1 |

## Routing

- **Route all queries** through AgentRouter for agent selection.
- Use: `from agents.Core.agent_router import get_agent_router; router = get_agent_router(); router.route_query(user_query, context)`
- Do not manually choose agents; router uses keywords (phoenix, test, postman, environment, etc.).

## Consultation (Rule 8)

- Before any task: get PhoenixExpert validation (approach, logic, correctness).
- Use: `AgentRegistry.consult_best_agent()` to get PhoenixExpert.
- Task must not proceed without approval. If rejected, follow PhoenixExpert guidance.

## IntegrationService (Rule 11)

- Before any task execution: call `IntegrationService.update_before_task()`.
- Use: `from agents.Core.integration_service import get_integration_service; get_integration_service().update_before_task(...)`
- Updates GitLab pipelines and Jira; skipping this is a critical violation.

## Agent Collaboration Patterns

- **Test:** TestAgent → consult PhoenixExpert → execute test.
- **Postman:** PostmanCollectionGenerator → consult PhoenixExpert → generate collection.
- **GitLab updates:** GitLabUpdateAgent → consult PhoenixExpert → validate → update.
- **Environment access:** EnvironmentAccessAgent → IntegrationService → access environment.
- **Bug validation:** BugFinderAgent → IntegrationService → consult PhoenixExpert → validate (Confluence + code) → report.

All participating agents must appear in the "Agents involved" footer.

## Agent Directory Structure (Rule 34)

New agents must live in subdirectories of `agents/`:

- `Main/` — primary (PhoenixExpert, TestAgent, BugFinderAgent).
- `Support/` — GitLabUpdateAgent, EnvironmentAccessAgent.
- `Core/` — AgentRegistry, AgentRouter, IntegrationService, GlobalRules.
- `Adapters/` — `*_adapter.py` implementing Agent interface.
- `Services/` — ReportingService, PostmanCollectionGenerator.
- `Utils/` — rules_loader, logger_utils, reporting_helper.

Never place agent modules in `agents/` root. Use absolute imports: `from agents.Main import ...`.

## Output Requirements

- State expert at beginning (e.g. "**Expert:** PhoenixExpert").
- End with: "Agents involved: [AgentNames]" or "Agents involved: None (direct tool usage)".
- All documentation and reports in English (Rule 0.7).

## Rules Source

Full rules: `.cursor/rules/` (agent_rules.mdc, core_rules.mdc, phoenix.mdc). Rule 0.0 requires loading rules before any action.
