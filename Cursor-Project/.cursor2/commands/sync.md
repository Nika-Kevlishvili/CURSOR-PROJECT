# Git Sync from GitLab

Synchronize Phoenix projects from GitLab using `git_sync_workflow.mdc` rules.

## MANDATORY: Follow git_sync_workflow.mdc Rules

**CRITICAL:** This command MUST follow the workflow defined in `.cursor/rules/git_sync_workflow.mdc`.

**DO NOT use GitLabUpdateAgent class or any Python agent code. Use direct git commands as specified in the rules.**

## Triggers

- `!sync` - Fetch all Phoenix projects from GitLab
- `!update <branch>` - Update specified branch (dev, dev2, dev-fix, test)
- `!checkout <branch>` - Checkout specified branch

## Workflow Reference

**ALWAYS read and follow:** `.cursor/rules/git_sync_workflow.mdc`

### Key Principles (from git_sync_workflow.mdc):

1. **Preserve Local Changes:** ALWAYS stash uncommitted changes before operations
2. **GitLab is Source:** Fetch from GitLab remote (git.domain.internal)
3. **READ-ONLY:** Only fetch/checkout/merge - NEVER push
4. **Auto-Discovery:** Process all Git repos in `Cursor-Project/Phoenix/` directory
5. **Cross-Platform:** Use PowerShell syntax on Windows
6. **Correct Path:** ALWAYS use `Cursor-Project/Phoenix/` - if doesn't exist, create it and clone projects there

### Operations:

#### 1. Sync All Projects (`!sync`)
```
1. Detect workspace root (find Phoenix/ directory)
2. For each repo in Phoenix/:
   - Stash uncommitted changes
   - git fetch origin --all
   - git fetch origin --prune
   - Unstash changes
```

#### 2. Update Branch (`!update <branch>`)
```
1. Detect workspace root
2. For each repo in Phoenix/:
   - Stash uncommitted changes
   - git fetch origin
   - Check if local is behind remote
   - If behind: git merge origin/<branch>
   - If diverged: STOP and ask user
   - Unstash changes
```

#### 3. Checkout Branch (`!checkout <branch>`)
```
1. Detect workspace root
2. For each repo in Phoenix/:
   - Stash uncommitted changes
   - git fetch origin
   - git checkout <branch> (or create tracking branch)
   - Unstash changes
```

## Token Configuration

Token is already configured in `git_sync_workflow.mdc`:
- **Token:** `glpat-s3G3rmuJUPbsJBns039NRG86MQp1OjNzCA.01.0y0s67eqg`
- **Host:** `git.domain.internal`
- **Credentials:** Stored in `~/.git-credentials`

## Usage Examples

**Fetch all projects:**
```
/sync

განაახლე Phoenix პროექტები GitLab-დან
```

**Update dev branch:**
```
/sync

!update dev
```

**Checkout dev2 branch:**
```
/sync

!checkout dev2
```

## Response Must Include:
- Projects processed
- Branch used (if applicable)
- Status for each repo (success/up-to-date/diverged/failed)
- Any stashed changes info

## CRITICAL RULES:

1. **ALWAYS stash local changes first** - never lose local work
2. **NEVER use force=True or hard reset** - preserve local changes
3. **If branches diverged** - STOP and ask user how to proceed
4. **READ-ONLY operations only** - no push, no commit
5. **Follow git_sync_workflow.mdc exactly** - it has complete implementation

## Agents Involved:
GitLabUpdateAgent (follows git_sync_workflow.mdc rules)
