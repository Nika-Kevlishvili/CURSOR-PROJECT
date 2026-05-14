---
name: cross-dependency-finder
description: Routes cross-dependency analysis to the cross-dependency-finder agent. Use when user asks for dependency analysis, what could break, or before generating test cases.
---

# Cross-Dependency Finder Skill

Routes cross-dependency work to the **cross-dependency-finder** agent.

## When to Apply

- User asks for cross-dependencies, dependency analysis, or "what could break."
- User wants to understand impact of changes.
- Before generating test cases (mandatory first step).

## Action

1. Delegate to **cross-dependency-finder** subagent with scope (bug/task/feature description, Jira key).
2. The agent returns a structured JSON report.
3. Pass the report to **test-case-generator** as `context['cross_dependency_data']` if test cases are being generated.

## Do NOT

- Duplicate the cross-dep workflow here — the agent has the full procedure.
- Perform ad-hoc dependency analysis without the structured agent.
- Skip this step when test case generation is requested.

## Reference

- Agent: `agents/cross-dependency-finder.md`
- Orchestrator: `agents/qa-workflow.md` (Pipeline 1, step 2)
