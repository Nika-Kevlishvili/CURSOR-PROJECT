# Update Experiments Branch Command

Updates experiments branch with local changes (commit and push).

## When to Use

Use this command when you want to:
- Commit local changes to experiments branch
- Push experiments branch to remote
- Synchronize local experiments branch with remote

## Usage

```powershell
.cursor/commands/update-experiments.ps1
.cursor/commands/update-experiments.ps1 -Message "Custom commit message"
```

## What It Does

1. **Checks current branch** - Ensures you're on experiments branch
2. **Stages all changes** - Adds all modified and new files
3. **Commits changes** - Creates a commit with the provided message (or default)
4. **Pushes to remote** - Pushes experiments branch to origin/experiments

## Parameters

- `-Message` (optional): Custom commit message. Default: "Update experiments branch"

## Workflow

1. Switch to experiments branch (if not already on it)
2. Stage all changes (`git add -A`)
3. Commit changes (`git commit -m <message>`)
4. Push to origin/experiments (`git push origin experiments`)

## Example

```powershell
# Update with default message
.cursor/commands/update-experiments.ps1

# Update with custom message
.cursor/commands/update-experiments.ps1 -Message "Add new feature X"
```

## Notes

- Automatically switches to experiments branch if needed
- Preserves all local changes
- Only pushes if branch is ahead of remote
- Shows status after completion

## Related Commands

- **`update-main-from-experiments`** - Updates main branch with experiments changes
- **`sync-cursor-with-main`** - Syncs cursor branch (EnergoTS) with main
