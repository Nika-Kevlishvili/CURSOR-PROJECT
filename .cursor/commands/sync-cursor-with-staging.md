# Sync Cursor with Backend-Staging Command

## Description

Smart sync command that:
- **If uncommitted changes exist**: Downloads the latest valid version of the `cursor` branch from remote and preserves uncommitted changes
- **If no uncommitted changes**: Updates the `cursor` branch from `Backend-staging` branch with automatic conflict resolution

## Usage

```powershell
# From workspace root or any directory (smart mode - downloads cursor if uncommitted changes exist)
.cursor/commands/sync-cursor-with-staging.ps1

# Force Backend-staging sync even with uncommitted changes
.cursor/commands/sync-cursor-with-staging.ps1 -ForceSync

# Or with full path
& "C:\Users\N.kevlishvili\Cursor\.cursor\commands\sync-cursor-with-staging.ps1"
```

## What It Does

### Smart Mode (Default - when uncommitted changes exist):
1. **Checks current branch** - Verifies you're on `cursor` branch
2. **Detects uncommitted changes** - Checks for modified/untracked files
3. **Downloads latest cursor branch** - Fetches `origin/cursor` from remote
4. **Updates local cursor** - Resets local cursor to match remote cursor (if newer)
5. **Preserves uncommitted changes** - Keeps all your local modifications

### Backend-Staging Sync Mode (when no uncommitted changes OR -ForceSync):
1. **Checks current branch** - Verifies you're on `cursor` branch
2. **Stashes local changes** - Saves all uncommitted changes (if any)
3. **Fetches from Backend-staging** - Downloads latest changes from `origin/Backend-staging`
4. **Merges Backend-staging into cursor** - Merges `origin/Backend-staging` into `cursor` branch
5. **Auto-resolves conflicts** - Automatically resolves all conflicts by keeping cursor branch versions
6. **Completes merge** - Commits the merge automatically
7. **Restores local changes** - Applies your stashed changes back

## Key Features

- ✅ **Smart behavior** - Downloads cursor branch if uncommitted changes exist, otherwise syncs with Backend-staging
- ✅ **Preserves uncommitted changes** - Always keeps your local modifications
- ✅ **Latest cursor version** - Downloads the most recent valid cursor branch from remote
- ✅ **Automatic conflict resolution** - When syncing with Backend-staging, keeps cursor branch changes
- ✅ **Flexible modes** - Use `-ForceSync` to force Backend-staging sync even with uncommitted changes
- ✅ **Status reporting** - Shows what files were modified

## Example Output

### Smart Mode (with uncommitted changes):
```
=== Syncing cursor branch with Backend-staging ===

Current branch: cursor
Source branch: Backend-staging

Found uncommitted changes:
Modified files:
  M fixtures/baseFixture.ts
  M tests/setup/global-setup.ts

Downloading latest valid version of cursor branch...
Your uncommitted changes will be preserved.

Fetching latest cursor branch from remote...
Remote cursor branch has newer commits. Updating local cursor branch...
Restoring your uncommitted changes...
Uncommitted changes restored successfully.

=== Download completed ===

Current status:
M  fixtures/baseFixture.ts
M  tests/setup/global-setup.ts

Cursor branch updated to latest valid version from remote.
Your uncommitted changes have been preserved.
```

### Backend-Staging Sync Mode (no uncommitted changes):
```
=== Syncing cursor branch with Backend-staging ===

Current branch: cursor
Source branch: Backend-staging

No uncommitted changes found.

Fetching latest changes from Backend-staging branch...
Fetch completed successfully.

Merging origin/Backend-staging into cursor branch...
Merge completed successfully (no conflicts).

=== Sync completed ===

Cursor branch has been synced with Backend-staging.
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
- **Smart behavior**: If uncommitted changes exist, downloads latest cursor branch instead of syncing with Backend-staging
- **Use `-ForceSync`** to force Backend-staging sync even with uncommitted changes
- All local changes are always preserved
- Untracked files are also preserved
- **When syncing with Backend-staging**: All conflicts are resolved automatically by keeping cursor branch versions
- **Specifically designed for Backend-staging branch** - Use `sync-cursor-with-main` for main branch

## Comparison with Other Commands

- **`sync-cursor-with-main`** - Syncs from main branch
- **`sync-cursor-with-staging`** - Syncs from Backend-staging branch (this command)
