---
name: phoenix-core
description: Core Phoenix agent workflow -- routing, PhoenixExpert consultation, IntegrationService, read-only safety, report generation, agent directory structure. Use when handling Phoenix questions, multi-agent tasks, agent routing, consultation, safety/permissions questions, or report generation.
---

# Phoenix Core Workflow

Guides the agent through the mandatory Phoenix workflow: routing, consultation, safety, and reporting.

## When to Apply

- User asks about Phoenix (backend, endpoints, business logic, Confluence)
- Task involves agents, routing, consultation, or "which agent"
- User asks about permissions, safety, what is allowed/forbidden
- After any task (report generation)
- Any task that requires Rules 0.1-0.8, Rule 8, Rule 11, Rule 13

## Workflow Steps

| Step | Action | Rule |
|------|--------|------|
| 1 | Call IntegrationService.update_before_task() | Rule 0.3 / 11 |
| 2 | Route via AgentRouter -- do not pick agents manually | Rule 13 |
| 3 | Consult PhoenixExpert before task (all agents) | Rule 0.4 / 8 |
| 4 | Execute task | -- |
| 5 | Save agent report + summary to `Cursor-Project/reports/YYYY-MM-DD/` | Rule 0.6 |
| 6 | End with "Agents involved: [names]" | Rule 0.1 |

## Safety (Read-Only)

- GitLab and Confluence are STRICTLY READ-ONLY. No commits, pushes, merges, page edits.
- Confluence edit tools FORBIDDEN: updateConfluencePage, createConfluencePage, createConfluenceFooterComment, createConfluenceInlineComment.
- Code modification per Rule 0.8 tiers: Phoenix (never), EnergoTS (only EnergoTSTestAgent in tests/), elsewhere (user-requested OK).
- NEVER log credentials, passwords, API keys, or tokens.

## Reporting (Rule 0.6)

After every task, save:
- Agent report: `Cursor-Project/reports/YYYY-MM-DD/{AgentName}_{HHMM}.md`
- Summary: `Cursor-Project/reports/YYYY-MM-DD/Summary_{HHMM}.md`
- Use current date dynamically. Reports required even for simple answers.

## Agent Directory Structure (Rule 34)

New agents go in `agents/` subdirectories: Main/, Support/, Core/, Adapters/, Services/, Utils/. Never in `agents/` root. See `Cursor-Project/agents/README.md`.

## Collaboration Patterns

- Test: TestAgent -> consult PhoenixExpert -> execute
- Postman: PostmanCollectionGenerator -> consult PhoenixExpert -> generate
- Bug validation: BugFinderAgent -> IntegrationService -> PhoenixExpert -> validate -> report
- Environment: EnvironmentAccessAgent -> IntegrationService -> access

All participating agents appear in "Agents involved" footer.

## Output

- State expert at beginning (e.g. "**Expert:** PhoenixExpert")
- End with "Agents involved: [names]"
- All documentation in English (Rule 0.7)

Full rules: `.cursor/rules/core_rules.mdc`, `safety_rules.mdc`, `agent_rules.mdc`.
