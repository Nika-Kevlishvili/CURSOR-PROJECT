# Sync Cursor with Main or Staging Command

## Description

Updates the `cursor` branch in EnergoTS project from `main` or `staging` branch, **automatically resolves all merge conflicts** (keeping cursor branch changes), and preserves all local uncommitted changes.

## Usage

```powershell
# Sync from main (default)
.cursor/commands/sync-cursor-with-main.ps1

# Sync from staging
.cursor/commands/sync-cursor-with-main.ps1 -SourceBranch staging

# Or with full path
& "C:\Users\N.kevlishvili\Cursor\.cursor\commands\sync-cursor-with-main.ps1" -SourceBranch staging
```

## What It Does

1. **Checks current branch** - Verifies you're on `cursor` branch
2. **Stashes local changes** - Saves all uncommitted changes (including untracked files)
3. **Fetches from source branch** - Downloads latest changes from `origin/main` or `origin/staging`
4. **Merges source into cursor** - Merges source branch into `cursor` branch
5. **Auto-resolves conflicts** - Automatically resolves all conflicts by keeping cursor branch versions
6. **Completes merge** - Commits the merge automatically
7. **Restores local changes** - Applies your stashed changes back

## Parameters

- **`-SourceBranch`** (optional, default: `main`) - Source branch to sync from. Can be `main` or `staging`

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
=== Syncing cursor branch with staging ===

Current branch: cursor
Source branch: staging

Found uncommitted changes. Stashing to preserve them...
Modified files:
  M fixtures/baseFixture.ts
  M tests/setup/global-setup.ts
Changes stashed successfully.

Fetching latest changes from staging branch...
Fetch completed successfully.

Merging origin/staging into cursor branch...
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

Cursor branch has been synced with staging.
All conflicts resolved (kept cursor branch changes).
Your local changes have been preserved.
```

## Conflict Resolution Strategy

The command automatically resolves conflicts by **keeping cursor branch changes**:

- **Both modified** → Keeps cursor version (`git checkout --ours`)
- **Deleted by us** → Keeps deleted (`git rm`)
- **Deleted by them** → Keeps file (`git add`)

This ensures your cursor branch changes are preserved while incorporating new changes from the source branch (main or staging).

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

## Examples

### Sync from main (default)
```powershell
.cursor/commands/sync-cursor-with-main.ps1
# or
.cursor/commands/sync-cursor-with-main.ps1 -SourceBranch main
```

### Sync from staging
```powershell
.cursor/commands/sync-cursor-with-main.ps1 -SourceBranch staging
```

## Notes

- Default source branch is `main` if not specified
- Only `main` and `staging` branches are supported
- All conflicts are resolved automatically by keeping cursor branch versions
