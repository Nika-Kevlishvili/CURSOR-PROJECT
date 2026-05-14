---
name: database-workflow
description: Routes database query requests to the database-query agent with correct environment. Use when user asks about database, queries, or a specific DB environment.
---

# Database Workflow Skill

Routes database work to the **database-query** agent.

## When to Apply

- User asks about database queries, data lookup, or DB-related investigation.
- User mentions a specific environment (Dev, Dev2, Test, PreProd, Prod).
- User wants to query tables, check records, or inspect DB schema.

## Action

1. If environment is not specified, route to **environment-resolver** first.
2. Delegate to **database-query** subagent with the resolved environment and query intent.
3. The agent connects via MCP and returns results.

## Do NOT

- Guess the environment — always resolve first.
- Log or expose database credentials.
- Execute write operations unless the user explicitly commands it.

## Reference

- Agent: `agents/database-query.md`
- Rule: `rules/integrations/database_workflow.mdc`
