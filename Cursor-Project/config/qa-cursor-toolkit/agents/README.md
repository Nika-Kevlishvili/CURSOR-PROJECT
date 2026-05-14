# QA Toolkit — Cursor Subagents

Subagents delegate work to specialized contexts. Each file under `agents/` describes how to run that role in Cursor chat.

**Location:** Copy to your project's `.cursor/agents/` directory.

---

## Agent Map

| Agent file | Role | When to use |
|------------|------|-------------|
| **qa-workflow.md** | **Orchestrator** | Sequences multi-agent pipelines (env → cross-dep → TC → quality → report). |
| **environment-resolver.md** | Environment resolver | Resolves dev/test/prod/... — asks user when ambiguous. |
| **bug-validator.md** | Bug validation | Confluence → Swagger → codebase → 5-verdict analysis. READ-ONLY. |
| **cross-dependency-finder.md** | Cross-dependencies | Find upstream/downstream deps and what could break. Feeds test-case-generator. |
| **test-case-generator.md** | Test cases | Generate Backend/Frontend TCs from bugs/tasks with cross-dep data. |
| **test-case-quality-validator.md** | TC quality | Score TCs on 6 axes (0–2 each). Pass threshold: 8/12. READ-ONLY. |
| **database-query.md** | DB queries | PostgreSQL via MCP; connect-first; environment-aware. |
| **shell.md** | Shell / CLI | Delegated terminal + safe git/CLI work. |
| **report-generator.md** | Reports | Save `.md` reports only when `/report`, `/feedback`, or explicit save. |

---

## How Cursor uses these

- Place agent `.md` files in your project's `.cursor/agents/` directory.
- Cursor loads them as subagent definitions.
- Delegate when a task fits a subagent's role.

## Related

- **Rules:** `.cursor/rules/` — see `rules/` folder in this toolkit.
- **Skills:** `.cursor/skills/` — see `skills/` folder in this toolkit.
- **Commands:** `.cursor/commands/` — see `commands/` folder in this toolkit.
