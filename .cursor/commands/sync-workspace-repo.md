# Sync workspace repo (Cursor-Project repository only)

Syncs **only** the workspace root Git repository (the repo that contains the `Cursor-Project` folder). Does **not** touch sub-repos (EnergoTS, Phoenix).

## Usage

```powershell
.cursor/commands/sync-workspace-repo.ps1
```

## What it does

1. **Finds workspace root** – Parent of `.cursor`, must contain `Cursor-Project` and `.git`.
2. **Stashes local changes** – Uncommitted and untracked files are stashed.
3. **Fetches** – `git fetch origin`.
4. **Updates current branch** – Merges `origin/<current_branch>` into the current branch (or reports up-to-date/diverged).
5. **Restores stash** – Reapplies stashed changes.

## When to use

- You want to update only the **Cursor-Project repo** (experiments, main, etc.) with the remote.
- You do **not** want to sync EnergoTS or Phoenix sub-repos.

## Safety

- Read-only for remote: fetch and merge only; no push.
- Local changes are stashed and then restored.
- If the branch has diverged, merge is skipped and you are told to resolve manually.

## Related

- **sync-main-project** – Syncs all repos under Cursor-Project (EnergoTS + Phoenix).
