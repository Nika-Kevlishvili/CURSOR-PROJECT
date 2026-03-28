# Sync workspace repo — 2026-03-25

## Result

- **Repository:** `D:\Cursor\cursor-project` (branch `experiments`).
- **Script:** `.cursor/commands/sync-workspace-repo.ps1` stashed local work then exited early (PowerShell likely treated a Git LF/CRLF **warning** on stderr as a terminating error under `$ErrorActionPreference = Stop`).
- **Completed manually:** `git fetch origin`, fast-forward merge `origin/experiments` (15 commits), then `git stash pop`.

## Merge summary (fast-forward)

- `.cursor/commands/sync.md` — updated
- `.cursor/rules/git_sync_workflow.mdc` — updated
- `Cursor-Project/EnergoTS` — submodule pointer updated

## After sync

- Branch **up to date** with `origin/experiments`.
- Prior local changes restored from stash (tracked + untracked); working tree still shows submodule and test-case paths as before sync.
