# Switch Branch

Switch to a specified branch across all Phoenix repositories (or a specific one). Optionally pull after switching.

## Overview

Runs `git checkout` across all Phoenix sub-projects under `Cursor-Project/Phoenix/`. Supports filtering by project name. After switching, optionally pulls the latest changes. Reports current branch state per project before and after.

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
   - No arguments -> show current branch status of every project (status check mode)
   - `<branch>` -> switch all projects to the specified branch
   - `<branch> project:<name>` -> switch only the matching project
   - `<branch> pull` -> switch all projects and pull after switching
   - `<branch> project:<name> pull` -> switch specific project and pull
   - Project name matching is case-insensitive and partial (e.g., `core` matches `phoenix-core`)

2. **Authentication** -- Before any git operations, ensure credentials are configured:
   - Use `$env:GIT_READONLY_TOKEN` from environment if set
   - Otherwise use existing `~/.git-credentials`
   - Git host: `git.domain.internal`

3. **Before switching -- check for uncommitted changes**
   - For each target project, run `git status --porcelain`
   - If there are uncommitted changes, warn the user: "Project X has uncommitted changes. Switching branch may fail or carry changes over."
   - Do NOT abort -- proceed but show the warning prominently

4. **For each target project:**
   - Record the current branch (`git branch --show-current`)
   - Check if the target branch exists locally: `git branch --list <branch>`
   - If it exists locally: `git checkout <branch>`
   - If not: check if it exists on remote: `git ls-remote --heads origin <branch>`
     - If remote exists: `git checkout -b <branch> origin/<branch>` (create tracking branch)
     - If remote doesn't exist either: report "Branch not found" and skip this project
   - If `pull` was specified: run `git pull` after checkout

5. **Report results**
   - Show before/after branch state per project
   - Highlight any warnings (uncommitted changes, branch not found)

## Output Format

### Branch switch:
```
## Branch Switch -- [timestamp]
Target branch: [branch name]

| Project | Was On | Now On | Status |
|---------|--------|--------|--------|
| phoenix-core-lib | dev2 | dev | OK Switched |
| phoenix-core | dev2 | dev | OK Switched |
| phoenix-scheduler | main | dev | OK Switched (new tracking branch) |
| phoenix-billing-run | dev2 | dev | OK Switched |
```

If a project has uncommitted changes:
```
| phoenix-core | dev2 | dev2 | WARNING Skipped -- uncommitted changes present |
```

If branch not found:
```
| phoenix-billing-run | dev2 | dev2 | ERROR Branch 'feature/xyz' not found locally or on remote |
```

### Status check (no arguments):
```
## Current Branch Status

| Project | Current Branch | Uncommitted Changes |
|---------|---------------|---------------------|
| phoenix-core-lib | dev2 | No |
| phoenix-core | dev2 | Yes (3 files) |
| phoenix-scheduler | dev2 | No |
```

## Usage Examples

```
/switch-branch
/switch-branch dev
/switch-branch dev2
/switch-branch test
/switch-branch prod
/switch-branch main
/switch-branch dev pull
/switch-branch test project:core
/switch-branch dev project:phoenix-core-lib
/switch-branch dev2 project:scheduler pull
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

- The command never force-switches or discards uncommitted changes -- safety first
- If you have uncommitted changes and still want to switch, stash them first: `git stash` in the project directory, then run `/switch-branch`
- READ-ONLY: this command only reads from remote and switches locally, never pushes
- After switching branches, run `/pull-repos` to ensure you have the latest remote changes
- Projects path: `Cursor-Project/Phoenix/` (same location used by `/sync` and `/pull-repos`)

## Agents Involved

GitLabUpdateAgent (git-sync subagent)
