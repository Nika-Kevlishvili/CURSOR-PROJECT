# PhoenixExpert Report — remove merge/git from cross-dependency (2026-03-30 23:12)

## User request

Local **D:** (or any local clone) git merge archaeology does not help; **remove merge viewing entirely** from the cross-dependency workflow.

## Changes

- **Rule 35a** rewritten in `.cursor/rules/workflows/workflow_rules.mdc`: Jira + codebase + shallow Confluence; **prohibited** local merge/git, git snapshot script, sync triggered only for cross-dep; optional GitLab MR **only** if user explicitly asks.
- **Rule 35** / **phoenix.mdc** updated to match.
- **Removed** `Cursor-Project/examples/CrossDependency_GitSnapshot.ps1` and `cross_dependencies/cache/PDT-2553_git_snapshot.json`.
- **Updated** skills, agents, commands, `CROSS_DEPENDENCY_*` docs, `COMMANDS_REFERENCE.md`, `phoenix-commands` skill, `CURSOR_SUBAGENTS.md`, `AGENT_SUBAGENT_MAP.md`, `hands-off`, `test-case-generate`.

READ-ONLY for Phoenix code unchanged.

Agents involved: PhoenixExpert
