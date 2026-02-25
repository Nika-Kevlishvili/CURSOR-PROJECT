# Pull EnergoTS Command

## Description

**Pull EnergoTS** fetches and updates the local EnergoTS repository to the **latest `cursor` branch from remote** (origin/cursor). Works **whether the EnergoTS folder is empty, missing, or already has content** — if the folder is missing or not a git repo, the script can clone the repository first (when a repo URL is provided).

## Usage

```powershell
# Pull cursor branch (EnergoTS folder must already exist and be a git repo)
.cursor/commands/pull-energots.ps1

# Clone then checkout cursor (when folder is missing or empty) — provide repo URL
.cursor/commands/pull-energots.ps1 -RepoUrl "https://github.com/your-org/EnergoTS.git"

# Or set env and run (for clone when folder empty)
$env:ENERGOTS_REPO_URL = "https://github.com/your-org/EnergoTS.git"
.cursor/commands/pull-energots.ps1
```

## What It Does

1. **Resolves Cursor-Project path** — Finds `Cursor-Project\EnergoTS` from the workspace.
2. **Handles missing or empty folder:**
   - If the EnergoTS folder does **not** exist or has **no `.git`**: clones the repo when `-RepoUrl` or `$env:ENERGOTS_REPO_URL` is set, then checks out `cursor`.
   - If the folder **exists** and is a git repo: continues with fetch/update.
3. **Stashes local changes** — Saves uncommitted changes (including untracked) before updating.
4. **Fetches remote** — `git fetch origin cursor` (and `--all` for robustness).
5. **Updates to latest cursor** — Checks out `cursor`, then merges or resets to `origin/cursor` so local matches remote.
6. **Restores stashed changes** — Reapplies stashed work after the update.

## Parameters

- **`-RepoUrl`** (optional) — Git clone URL for EnergoTS. Used when the EnergoTS folder is missing or not a git repo. Can also be set via `$env:ENERGOTS_REPO_URL`.

## When to Use

- You want the **latest `cursor` branch** from remote, regardless of local state.
- The EnergoTS folder is **empty or missing** and you want to clone and land on `cursor` (with `-RepoUrl` or `ENERGOTS_REPO_URL`).
- You want to **refresh** your local `cursor` branch without syncing from `main` or `staging`.

## Safety

- **EnergoTS branch lock:** Only the `cursor` branch is used; no switch to other branches.
- Local uncommitted changes are stashed and then restored.
- Read-only regarding remote: only fetch and local merge/reset; no push.

## Example Output

```
=== Pull EnergoTS ===

EnergoTS path: C:\...\Cursor-Project\EnergoTS
Current branch: cursor

Fetching origin (cursor)...
Fetch completed.

Updating local cursor to origin/cursor...
Local cursor is now up to date with origin/cursor.

Restoring local changes...
=== Done ===
```

## Notes

- If the folder is missing or not a git repo and no `-RepoUrl` or `ENERGOTS_REPO_URL` is set, the script exits with an error and asks you to provide the clone URL.
- Compliant with EnergoTS branch lock: only `cursor` branch is used; no automatic sync from `main`.
