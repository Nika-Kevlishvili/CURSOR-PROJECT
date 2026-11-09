# Claude Code workspace configuration

This folder is at the **Git workspace root** (next to `Cursor-Project/`). It replaces the former `.cursor/` folder (ported from Cursor; the mapping is below). Entry point for the model: [`../CLAUDE.md`](../CLAUDE.md).

| Area | Entry |
|------|-------|
| **Rules** (thematic `.md`) | [docs/rules-README.md](docs/rules-README.md) — index: [rules/main/phoenix.md](rules/main/phoenix.md) |
| **Subagents** | [docs/agents-README.md](docs/agents-README.md) |
| **Agent paths (canonical map)** | [../Cursor-Project/docs/AGENT_SUBAGENT_MAP.md](../Cursor-Project/docs/AGENT_SUBAGENT_MAP.md) |
| **Skills** | [docs/skills-README.md](docs/skills-README.md) |
| **Commands** | `commands/*.md` (slash commands) + `commands/*.ps1` (scripts) |
| **Procedures** | `procedures/` |
| **Hooks** | `settings.json` + `hooks/claude-hook.ps1` + `hooks/*.ps1` |

## Cursor → Claude Code mapping

| Cursor | Claude Code |
|--------|-------------|
| `.cursor/rules/**/*.mdc`, `alwaysApply: true` | `.claude/rules/**/*.md` with no `paths:` (always loaded) |
| `.cursor/rules/**/*.mdc`, `globs:` | `.claude/rules/**/*.md` with `paths:` (loads when a matching file is read) |
| `.cursor/agents/*.md` | `.claude/agents/*.md` |
| `.cursor/skills/*/SKILL.md` | `.claude/skills/*/SKILL.md` |
| `.cursor/commands/*.md` | `.claude/commands/*.md` |
| `.cursor/hooks.json` | `hooks` in `.claude/settings.json` |
| `AskQuestion` tool | `AskUserQuestion` tool |
| Task tool | Agent tool |

## Hooks

Every hook entry in `settings.json` runs `hooks/claude-hook.ps1 <Group>` with `pwsh`. The dispatcher converts the Claude Code payload to the JSON shape the guard scripts read, runs every guard registered for the group in one process, and converts the combined verdict (deny > ask > allow) back to Claude Code hook output. To add, remove or reorder a guard, edit the `$registry` table at the top of `claude-hook.ps1`.

| Claude Code event | Group | Guards |
|-------------------|-------|--------|
| `UserPromptSubmit` | `UserPromptSubmit` | `block-jira-phoenix-delivery`, `remind-test-case-env-first`, `block-energots-branch-requests` |
| `PreToolUse` on `Edit`, `Write`, `MultiEdit`, `NotebookEdit` | `PreEdit` | `protect-phoenix-code` (fail-closed), `protect-energots-writes` |
| `PostToolUse` on the same tools | `PostEdit` | `warn-phoenix-code-edit` |
| `PreToolUse` on `Bash` | `PreBash` | `block-energots-branch-switch`, `block-acli-rovo` (fail-closed), `block-elasticsearch-write`, `block-postgresql-write`. `control-git-push` is listed but commented out |
| `PreToolUse` on `mcp__.*` | `PreMcp` | `block-postgresql-mcp`, `block-elasticsearch-mcp`, `block-atlassian-mcp`, `block-confluence-write`, `control-database-write`, `block-bugreview-unapproved-jira` |
| `Stop` | `Stop` | `validate-confidence-format` — non-blocking: the warning is shown and handed to the agent with the next prompt |

`settings.json` also denies the Edit tools on `Cursor-Project/Phoenix/**` through `permissions.deny`, as a second layer under the hook.

The Bash guards match on the command text, so a command that only *mentions* a blocked pattern (for example inside a quoted string) is blocked too. Put such text in a file instead.

Requirements: PowerShell 7 (`pwsh`) on `PATH`. Script logs and hook state go to `.claude/logs/` (git-ignored).
