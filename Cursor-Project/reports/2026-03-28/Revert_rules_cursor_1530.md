# Revert: rules and related `.cursor` docs to pre-sync state

**Request:** Restore rules and chat-applied rule-related edits to the previous version (user preference).

**Actions:**
- `git restore .cursor/` — restored tracked agents, commands, skills, hooks, and **flat** `.cursor/rules/*.mdc` (13 files at `.cursor/rules/` root).
- `git clean -fd .cursor/rules/` — removed untracked thematic layout (`main/`, `agents/`, `integrations/`, etc.) and `rules/README.md`.
- Removed untracked `.cursor/README.md` (hub doc from sync).
- `git restore` for `Cursor-Project/README.md` and docs: `RULES_LOADING_SYSTEM.md`, `CURSOR_SUBAGENTS.md`, `PROTECTION_STATUS.md`, `COMMANDS_REFERENCE.md`, `CROSS_DEPENDENCY_FINDER_AGENT.md`, `SETUP_CURSOR_NEW_PROJECT_GITLAB.md`.

**Left unchanged:** Untracked `update-swagger-specs` command files under `.cursor/commands/`; report folders under `Cursor-Project/reports/2026-03-28/` (optional manual cleanup).

**Agents involved:** None (git restore / clean).
