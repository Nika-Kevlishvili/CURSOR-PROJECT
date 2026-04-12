# Git hooks (repository)

## `pre-commit`

Blocks:

- Any staged path under `reports/Chat reports/` that is **not** a `.gitkeep` file.
- Staged edits to `Cursor-Project/.gitignore` that add lines referencing `Chat reports` together with `.md` tracking (prevents re-weakening the ignore policy).

## Enable (one-time per clone)

From the repository root:

```bash
git config core.hooksPath .githooks
```

Git runs hooks from this directory instead of `.git/hooks`.

## Windows

Use Git Bash for the `git config` command above, or run the equivalent in PowerShell from the repo root:

```powershell
git config core.hooksPath .githooks
```
