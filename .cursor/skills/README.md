# Project Skills

Skills guide the Cursor agent for this repo's workflows. **Location:** workspace root `.cursor/skills/`.

## Skills

| Skill | Purpose |
|-------|---------|
| **phoenix-database** | PostgreSQL MCP; environments; connect-first; SQL templates in `references/` |

All other workflows in this project (cross-dependency-finder, test-case-generator, production-data-reader, energo-ts-run, jira-bug, etc.) are invoked directly through their `.cursor/agents/*.md` subagent specs (via the `Task` tool) and/or their `.cursor/commands/*.md` slash commands. Cursor's subagent / command discovery handles routing without an extra skill pointer layer.

Always-applied rules in `.cursor/rules/` cover safety / read-only, file organization, reporting (Rule 0.6), and agent workflow (Rule 0.2-0.5).

## Source

- **Subagent specs:** `.cursor/agents/*.md`
- **Rules:** `.cursor/rules/**/*.mdc` (index: `main/phoenix.mdc`)
- **Commands:** `.cursor/commands/*.md`

Rule 0.0: load rules before acting. Skills summarize; rules are authoritative.
