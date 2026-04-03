# Git Sync from GitLab

Synchronize Phoenix projects from GitLab. READ-ONLY operations only (fetch, checkout, merge locally -- never push).

## Triggers

- `/sync` -- Fetch all Phoenix projects from GitLab
- `/sync` with `update <branch>` -- Update specified branch (dev, dev2, dev-fix, test, experiment)
- `/sync` with `checkout <branch>` -- Checkout specified branch

## Configuration

- **Git Host:** `git.domain.internal` (GitLab)
- **Token:** Use `$env:GIT_READONLY_TOKEN` from environment. If not set, use the token stored in `~/.git-credentials`.
- **Projects path:** `Cursor-Project/Phoenix/` (auto-discovered: all subdirectories containing `.git`)
- **Known repos:** phoenix-core-lib, phoenix-core, phoenix-billing-run, phoenix-api-gateway, phoenix-payment-api, phoenix-migration, phoenix-ui

## Key Principles

1. **Stash first:** ALWAYS stash uncommitted changes before operations, unstash after
2. **READ-ONLY:** Only fetch/checkout/merge locally -- NEVER push, commit, or modify remote
3. **Auto-discover:** Process all Git repos found in `Cursor-Project/Phoenix/`
4. **Divergence:** If local and remote branches have diverged, STOP and ask user
5. **Workspace detection:** Look for `Cursor-Project/Phoenix/` from workspace root; create if missing
6. **Clone if empty:** If `Phoenix/` has no git repos, clone all known projects using token auth

## Operations

### 1. Fetch All (`/sync`)

For each repo in `Cursor-Project/Phoenix/`:
1. Stash uncommitted changes
2. `git fetch origin --all && git fetch origin --prune`
3. Unstash changes

### 2. Update Branch (`update <branch>`)

For each repo in `Cursor-Project/Phoenix/`:
1. Stash uncommitted changes
2. `git fetch origin`
3. Checkout the branch (create tracking branch if needed: `git checkout -b <branch> origin/<branch>`)
4. Check divergence: if both ahead and behind, ABORT for that repo and ask user
5. If behind: `git merge origin/<branch>`
6. Unstash changes

### 3. Checkout Branch (`checkout <branch>`)

For each repo in `Cursor-Project/Phoenix/`:
1. Stash uncommitted changes
2. `git fetch origin`
3. `git checkout <branch>` (or create tracking branch from `origin/<branch>`)
4. If branch not on remote, skip and report
5. Unstash changes

## Authentication

Configure git credential helper with the readonly token before operations:

```powershell
$env:GIT_READONLY_TOKEN = "<token>"
git config --global credential.helper store
$GIT_HOST = "git.domain.internal"
$CREDENTIALS_FILE = "$env:USERPROFILE\.git-credentials"
"https://oauth2:$($env:GIT_READONLY_TOKEN)@$GIT_HOST" | Out-File -FilePath $CREDENTIALS_FILE -Encoding ASCII -NoNewline
```

## Error Handling

- **Uncommitted changes:** Auto-stash before, auto-unstash after
- **Branch diverged:** ABORT that repo, show divergence, ask user (merge/rebase/reset/abort)
- **Merge conflicts:** Keep stash, report conflicts, wait for user
- **Auth failure:** Check token value, test with `curl -H "Authorization: Bearer $TOKEN" https://git.domain.internal/api/v4/user`
- **Branch not found:** Skip repo, report, continue with others

## Response Must Include

- Projects processed
- Branch used (if applicable)
- Status for each repo (success / up-to-date / diverged / failed)
- Any stashed changes info

## Agents Involved

GitLabUpdateAgent (git-sync subagent)
