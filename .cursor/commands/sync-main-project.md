# Sync main project Command

## Description

**Sync main project** performs a **full pull and synchronization** of the entire Cursor-Project with the remote: all Git repositories under `Cursor-Project` (EnergoTS and Phoenix subprojects) are fetched and updated to their current branch’s remote state. Local uncommitted changes are stashed before sync and restored after.

## Usage

```powershell
# Sync main project (all repos in Cursor-Project)
.cursor/commands/sync-main-project.ps1

# With full path
& "C:\Users\N.kevlishvili\Cursor\.cursor\commands\sync-main-project.ps1"
```

## What It Does

1. **Finds workspace and Cursor-Project** — Resolves `Cursor-Project` from the workspace root.
2. **Discovers Git repos** — Collects all repositories:
   - `Cursor-Project/EnergoTS` (if it has `.git`)
   - Each subdirectory of `Cursor-Project/Phoenix/` that contains `.git`
   - Any other direct subdirectory of `Cursor-Project` that contains `.git`
3. **For each repository:**
   - Stashes uncommitted changes (including untracked).
   - **EnergoTS:** Ensures branch is `cursor`, then `git fetch origin` and updates to `origin/cursor` (merge or reset).
   - **Phoenix (and others):** `git fetch origin`, then merges `origin/<current_branch>` into the current branch (or reports if diverged).
   - Restores stashed changes.
4. **Reports** — Prints status for each repo (success / up-to-date / diverged / error).

## When to Use

- You want **one command** to pull and sync the whole Cursor-Project (EnergoTS + Phoenix) with remotes.
- You want all local branches in Cursor-Project to match their remotes without switching branches (except EnergoTS, which stays on `cursor`).

## Safety

- **Read-only for remote:** Only fetch and merge; no push.
- **EnergoTS:** Only the `cursor` branch is used (EnergoTS branch lock respected).
- **Local changes:** Stashed before sync and restored after; if merge fails or branches diverged, you are informed and stash remains for manual handling.

## Example Output

```
=== Sync main project ===

Workspace: C:\...\Cursor
Cursor-Project: C:\...\Cursor-Project

Repos: EnergoTS, Phoenix\phoenix-core-lib, Phoenix\phoenix-core, ...

--- EnergoTS ---
  Branch: cursor | Fetch + merge origin/cursor | OK

--- Phoenix\phoenix-core-lib ---
  Branch: dev | Fetch + merge origin/dev | OK

--- Phoenix\phoenix-core ---
  Branch: dev | Fetch + merge origin/dev | Up to date

=== Done ===
```

## Notes

- If a repo’s local branch has **diverged** from the remote (both have new commits), the script does not force a merge; it reports and leaves the repo as-is so you can resolve (merge/rebase) manually.
- Phoenix repos use the same Git remotes and credentials as in `git_sync_workflow.mdc` (e.g. GitLab token in credentials). Ensure `git fetch` works for each remote (token/SSH configured).
- Compliant with EnergoTS branch lock: EnergoTS is only updated on the `cursor` branch.
