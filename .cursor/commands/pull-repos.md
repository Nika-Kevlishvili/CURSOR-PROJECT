# Pull Repositories

Pull the latest changes from remote for all Phoenix repositories (or a specific one). Optionally switch to and pull a specific branch.

## Overview

Runs `git pull` across all Phoenix sub-projects under `Cursor-Project/Phoenix/`. Supports filtering by project name and/or branch name. Reports success/failure per project clearly.

## Phoenix Projects

All repositories live under `Cursor-Project/Phoenix/`:

| Project | Directory |
|---------|-----------|
| phoenix-api-gateway | `Cursor-Project/Phoenix/phoenix-api-gateway/` |
| phoenix-billing-run | `Cursor-Project/Phoenix/phoenix-billing-run/` |
| phoenix-core | `Cursor-Project/Phoenix/phoenix-core/` |
| phoenix-core-lib | `Cursor-Project/Phoenix/phoenix-core-lib/` |
| phoenix-db | `Cursor-Project/Phoenix/phoenix-db/` |
| phoenix-mass-import | `Cursor-Project/Phoenix/phoenix-mass-import/` |
| phoenix-migration | `Cursor-Project/Phoenix/phoenix-migration/` |
| phoenix-payment-api | `Cursor-Project/Phoenix/phoenix-payment-api/` |
| phoenix-scheduler | `Cursor-Project/Phoenix/phoenix-scheduler/` |
| phoenix-ui | `Cursor-Project/Phoenix/phoenix-ui/` |

Auto-discover: process all subdirectories containing `.git` in `Cursor-Project/Phoenix/`.

## Steps

1. **Parse arguments**
   - No arguments -> pull all projects, each on their current branch
   - `branch:<name>` -> pull all projects on the specified branch (switch to it first if needed)
   - `project:<name>` -> pull only the matching project on its current branch
   - `project:<name> branch:<name>` -> pull only the matching project on the specified branch
   - Project name matching is case-insensitive and partial (e.g., `core` matches `phoenix-core`)

2. **Authentication** -- Before any git operations, ensure credentials are configured:
   - Use `$env:GIT_READONLY_TOKEN` from environment if set
   - Otherwise use existing `~/.git-credentials`
   - Git host: `git.domain.internal`

3. **For each target project:**
   - `cd` into the project directory under `Cursor-Project/Phoenix/`
   - If a branch was specified:
     - Check if the branch exists locally: `git branch --list <branch>`
     - If it exists locally: `git checkout <branch>`
     - If not: `git checkout -b <branch> origin/<branch>` (track remote branch)
     - If the remote branch doesn't exist either: report error and skip this project
   - Run `git pull`
   - Capture the output (up to date / fast-forward / merge / error)

4. **Report results**
   - Show a summary table with status per project
   - For errors (merge conflicts, authentication failures, branch not found), show the exact git error message
   - If all succeed: "All repositories updated successfully"

## Output Format

```
## Pull Results -- [timestamp]
Branch: [branch name or "current branch"]

| Project | Branch | Status | Details |
|---------|--------|--------|---------|
| phoenix-core-lib | dev2 | OK Updated | 3 files changed, 47 insertions(+) |
| phoenix-core | dev2 | OK Up to date | Already up to date. |
| phoenix-scheduler | dev2 | OK Up to date | Already up to date. |
```

If any project has an error:
```
| phoenix-core | dev2 | ERROR | error: Your local changes would be overwritten... |
```

## Usage Examples

```
/pull-repos
/pull-repos branch:dev
/pull-repos branch:dev2
/pull-repos branch:test
/pull-repos project:core
/pull-repos project:phoenix-core
/pull-repos project:core-lib branch:dev
/pull-repos project:scheduler branch:main
```

## Common Branches

| Branch | Purpose |
|--------|---------|
| `dev` | Development |
| `dev2` | Development 2 |
| `main` | Main/stable |
| `test` | Test environment |
| `prod` | Production |

## Notes

- If a project has uncommitted local changes, `git pull` may fail with a conflict error -- the command reports this clearly without overwriting anything
- The command never force-pulls or discards local changes
- READ-ONLY: this command only pulls from remote, never pushes
- Projects path: `Cursor-Project/Phoenix/` (same location used by `/sync`)

## Agents Involved

GitLabUpdateAgent (git-sync subagent)
