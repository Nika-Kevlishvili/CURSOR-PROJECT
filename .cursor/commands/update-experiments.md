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

## One-time setup: push without manual run

For push to succeed when you run the command (e.g. from Cursor) without typing credentials, set **GITHUB_TOKEN** once:

1. Create a GitHub Personal Access Token (PAT): GitHub → Settings → Developer settings → Personal access tokens → Generate (scope: `repo`).
2. Set it in your environment (PowerShell, persistent for your user):
   ```powershell
   [System.Environment]::SetEnvironmentVariable("GITHUB_TOKEN", "ghp_YourTokenHere", "User")
   ```
3. Restart Cursor (or any app that needs to see the new env var). After that, `update-experiments` will use this token for push and you won’t need to run `git push` manually.

## Notes

- Automatically switches to experiments branch if needed
- Preserves all local changes
- Only pushes if branch is ahead of remote
- Push is retried up to 3 times on failure
- Shows status after completion

## Related Commands

- **`update-main-from-experiments`** - Updates main branch with experiments changes
- **`sync-cursor-with-main`** - Syncs cursor branch (EnergoTS) with main
