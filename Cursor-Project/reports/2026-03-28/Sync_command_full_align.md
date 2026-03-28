# Sync command: full local alignment (`!sync`)

**Date:** 2026-03-28

## Change

`/sync` / **`!sync`** is defined as **fetch + merge** into the **current** branch (`origin/<branch>`), not fetch-only.

## Also documented

- **`git stash push -u`** — includes untracked files before merge.
- **Windows:** **`git config core.longpaths true`** per repo to avoid “Filename too long” / incomplete checkouts.
- **Diverged** repos (ahead and behind): stop and ask user (unchanged).

## Files updated

- `.cursor/rules/integrations/git_sync_workflow.mdc`
- `.cursor/commands/sync.md`
- `.cursor/agents/git-sync.md`
- `Cursor-Project/docs/COMMANDS_REFERENCE.md`
- `.cursor/skills/phoenix-commands/SKILL.md`
- `.cursor/rules/workflows/workflow_rules.mdc` (wording)

GitLab remains **read-only** (no `push`).

Agents involved: None (direct edit)
