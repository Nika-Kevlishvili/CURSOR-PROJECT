---
name: git-sync
model: default
description: Syncs Phoenix projects from GitLab (fetch, update branch, checkout). Maps to GitLabUpdateAgent. Use when the user asks to sync, update, or checkout Phoenix repos from GitLab. READ-ONLY (no push).
---

# Git Sync Subagent

Sync Phoenix Git repos from GitLab: fetch, update branch, or checkout. Follow `.cursor/commands/sync.md` workflow. Use git commands only.

## Before Running

1. **Workspace:** Phoenix repos live in `Cursor-Project/Phoenix/`. Detect workspace root.
2. **Token:** Git read-only token from `$env:GIT_READONLY_TOKEN` or `~/.git-credentials`. Do not log the token.
3. **Stash:** If any repo has uncommitted changes, stash before operations and unstash after.

## Operations

### Sync all (`/sync`)
For each repo in `Cursor-Project/Phoenix/`: stash -> `git fetch origin --all` -> `git fetch origin --prune` -> unstash.

### Update branch (`update <branch>`)
Supported: dev, dev2, dev-fix, test, experiment, main, master. For each repo: stash -> fetch -> if behind, `git merge origin/<branch>`; if diverged, stop and ask user. Unstash.

### Checkout branch (`checkout <branch>`)
For each repo: stash -> fetch -> `git checkout <branch>` (or create tracking branch). Unstash.

## Constraints

READ-ONLY: fetch, checkout, merge only. Never push, commit, or force-push. If diverged, report and ask user.

## Output

List repos processed and status. End with "Agents involved: GitLabUpdateAgent".

Full workflow: `.cursor/commands/sync.md`
