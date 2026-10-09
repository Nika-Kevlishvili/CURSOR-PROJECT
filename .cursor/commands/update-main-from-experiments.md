# Update Main Branch from Experiments Command

Updates main branch with experiments branch changes (merge and push).

## When to Use

Use this command when you want to:
- Merge experiments branch changes into main
- Update main branch with experiments version
- Synchronize main branch with experiments

## Usage

```powershell
.cursor/commands/update-main-from-experiments.ps1
```

## What It Does

1. **Stashes local changes** - Preserves any uncommitted changes
2. **Fetches latest** - Fetches both experiments and main branches
3. **Switches to main** - Checks out main branch
4. **Merges experiments** - Merges origin/experiments into main
5. **Resolves conflicts** - Auto-resolves conflicts by keeping experiments version
6. **Pushes main** - Pushes updated main branch to origin/main
7. **Restores stashed changes** - Restores your local changes

## Workflow

1. Stash uncommitted changes (if any)
2. Fetch origin/experiments and origin/main
3. Switch to main branch
4. Merge origin/experiments into main
5. Auto-resolve conflicts (keep experiments version)
6. Push main to origin/main
7. Restore stashed changes

## Conflict Resolution

If merge conflicts occur:
- **Both modified (UU/AA)**: Keeps experiments version (`git checkout --theirs`)
- **Deleted by us (DU)**: Keeps deleted
- **Deleted by them (UD)**: Keeps file (experiments version)

All conflicts are automatically resolved by keeping experiments branch changes.

## Example

```powershell
# Update main with experiments changes
.cursor/commands/update-main-from-experiments.ps1
```

## Notes

- Automatically stashes and restores local changes
- Keeps experiments branch changes in case of conflicts
- Only merges if experiments has changes that main doesn't have
- Shows status after completion

## Related Commands

- **`update-experiments`** - Updates experiments branch with local changes
- **`sync-cursor-with-main`** - Syncs cursor branch (EnergoTS) with main
