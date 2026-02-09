# Sync Cursor with Backend-Staging Command

## Description

Updates the `cursor` branch in EnergoTS project from `Backend-staging` branch, **automatically resolves all merge conflicts** (keeping cursor branch changes), and preserves all local uncommitted changes.

## Usage

```powershell
# From workspace root or any directory
.cursor/commands/sync-cursor-with-staging.ps1

# Or with full path
& "C:\Users\N.kevlishvili\Cursor\.cursor\commands\sync-cursor-with-staging.ps1"
```

## What It Does

1. **Checks current branch** - Verifies you're on `cursor` branch
2. **Stashes local changes** - Saves all uncommitted changes (including untracked files)
3. **Fetches from Backend-staging** - Downloads latest changes from `origin/Backend-staging`
4. **Merges Backend-staging into cursor** - Merges `origin/Backend-staging` into `cursor` branch
5. **Auto-resolves conflicts** - Automatically resolves all conflicts by keeping cursor branch versions
6. **Completes merge** - Commits the merge automatically
7. **Restores local changes** - Applies your stashed changes back

## Key Features

- ✅ **Automatic conflict resolution** - Keeps cursor branch changes for all conflicts
- ✅ **Preserves all local changes** - Uses `git stash` to save your work
- ✅ **Handles all conflict types**:
  - Both modified → Keeps cursor version
  - Deleted by us → Keeps deleted
  - Deleted by them → Keeps file (cursor version)
- ✅ **Automatic merge commit** - Completes merge automatically after resolving conflicts
- ✅ **Status reporting** - Shows what files were modified

## Example Output

```
=== Syncing cursor branch with Backend-staging ===

Current branch: cursor
Source branch: Backend-staging

Found uncommitted changes. Stashing to preserve them...
Modified files:
  M fixtures/baseFixture.ts
  M tests/setup/global-setup.ts
Changes stashed successfully.

Fetching latest changes from Backend-staging branch...
Fetch completed successfully.

Merging origin/Backend-staging into cursor branch...
Merge conflicts detected. Auto-resolving by keeping cursor branch changes...

  Keeping cursor version: .gitignore
  Keeping cursor version: package.json
  Keeping cursor version: playwright.config.ts
  Keeping deleted: fixtures/Config_POD.ts
  Keeping file: .github/workflows/main.yml

All conflicts resolved. Completing merge...
Merge completed successfully.

Restoring your local changes...
Local changes restored successfully.

=== Sync completed ===

Current status:
M  fixtures/baseFixture.ts
M  tests/setup/global-setup.ts

Cursor branch has been synced with Backend-staging.
All conflicts resolved (kept cursor branch changes).
Your local changes have been preserved.
```

## Conflict Resolution Strategy

The command automatically resolves conflicts by **keeping cursor branch changes**:

- **Both modified** → Keeps cursor version (`git checkout --ours`)
- **Deleted by us** → Keeps deleted (`git rm`)
- **Deleted by them** → Keeps file (`git add`)

This ensures your cursor branch changes are preserved while incorporating new changes from Backend-staging.

## Safety Features

- ✅ **Preserves all local changes** - Uses `git stash` to save your work
- ✅ **Automatic rollback** - Restores stashed changes if merge fails
- ✅ **Conflict detection** - Reports if any conflicts cannot be auto-resolved
- ✅ **Status reporting** - Shows what files were modified

## Notes

- This command only works in EnergoTS directory
- It requires you to be on `cursor` branch (switches automatically if needed)
- All local changes are preserved using git stash
- Untracked files are also stashed and restored
- **All conflicts are resolved automatically** by keeping cursor branch versions
- **Specifically designed for Backend-staging branch** - Use `sync-cursor-with-main` for main branch

## Comparison with Other Commands

- **`sync-cursor-with-main`** - Syncs from main branch
- **`sync-cursor-with-staging`** - Syncs from Backend-staging branch (this command)
