---
name: bug-validation
description: Routes bug validation requests to the bug-validator agent. Use when user mentions bug validation, verify a bug, or validate a reported issue.
---

# Bug Validation Skill

Routes bug validation work to the **bug-validator** agent.

## When to Apply

- User asks to validate a bug, verify a bug report, or check if a reported issue is genuine.
- User mentions "bug validation", "validate bug", "is this a bug", "verify this issue".
- User provides a Jira ticket key and asks about bug validity.

## Action

1. If environment is unknown, route to **environment-resolver** first.
2. Delegate to **bug-validator** subagent with the ticket/bug context.
3. For multi-step pipelines (bug → test cases), use **qa-workflow** orchestrator instead.

## Do NOT

- Duplicate the bug-validator workflow here — the agent has the full procedure.
- Perform ad-hoc validation without the structured agent.
- Skip environment resolution for environment-sensitive bugs.

## Reference

- Agent: `agents/bug-validator.md`
- Orchestrator: `agents/qa-workflow.md` (Pipeline 2: Bug Validation)
