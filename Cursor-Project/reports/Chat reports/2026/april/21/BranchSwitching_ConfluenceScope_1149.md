# Session Report — Environment-Aware Branch Switching & Confluence Scope Rules

**Date:** 2026-04-21  
**Session scope:** Bug Validator auto-branch switching, Cursor agent branch-context alignment, Confluence scope optimization

---

## 1. Bug Validator — Environment-Aware Branch Switching

### Problem
Bug Validator's Python CI pipeline always scanned Phoenix code from whichever branch happened to be checked out locally. Bugs reported against different environments (dev, test, preprod, prod) were validated against potentially wrong code.

### Solution: `branch_resolver.py`

New module added to `Cursor-Project/scripts/bug-validator/`:

| Component | Description |
|-----------|-------------|
| `detect_environment()` | Detects env from bug text using regex (Georgian/English/Russian): dev, dev2, test, preprod, prod |
| `resolve_environment()` | Returns `(env, was_explicit)` — defaults to `prod` when no keyword found |
| `pick_branch_for_env()` | Selects best branch per repo using ordered priority list + date-based fallback for release branches |
| `switch_phoenix_repos_to_env()` | Iterates all Phoenix repos: `git fetch origin` → stash → checkout → `git merge origin/<branch>` |

**Branch priority per environment:**

| Env | Priority order |
|-----|---------------|
| `dev` | `dev` → `dev-fix` |
| `dev2` | `dev2` → `Dev2Release_*` → `Dev2Update_*` |
| `test` | `test` → `TestRelease_*` |
| `preprod` | `preprod` / `pre-prod` → `PreProdRelease_*` |
| `prod` | `prod` → `ProdRelease_*` → `main` → `master` |

**Status values in report:**
- `switched` — changed to target branch + pulled updates
- `updated` — already on correct branch, merged updates from origin
- `already_on_branch` — correct branch, no remote to pull
- `skipped` — repo has no matching branch
- `failed` — stash/checkout/merge issue

### Integration in `main.py`
- Runs between Jira fetch (Step 1) and Phoenix code scan (Step 3)
- Adds `environment` and `branch_switch` sections to JSON/Markdown reports
- Shows per-repo branch status in Slack notifications

### Files changed/added
- `branch_resolver.py` (new) — 438 lines
- `main.py` — import + branch switch before scan + report fields + markdown section
- `slack_report_template.py` — env block in Slack messages
- `test_branch_resolver.py` (new) — 11 detection + 8 branch-picking unit tests

### Encoding fix
- Added UTF-8 stdout/stderr wrapper for Windows CI runner (`cp1252` cannot handle Georgian characters)

### Commits pushed to `origin/main`
- `9709df9` — add environment-aware branch switching to bug validator
- `6a39c23` — fix Unicode encoding error on Windows CI runner

---

## 2. Rule 38 — Phoenix Code-Check Branch Context (Cursor Agents)

### Problem
Cursor agents (PhoenixExpert, BugFinder, CrossDependencyFinder, etc.) read Phoenix code without checking which branch is active. Analysis conclusions could be based on wrong environment's code.

### Solution: Rule 38 [CRITICAL]
Added to `workflow_rules.mdc`. Before ANY Phoenix code read/search:

1. Detect environment from user prompt / Jira bug text
2. Default to `prod` if no env keyword found
3. Run `!update <branch>` per `git_sync_workflow.mdc` (fetch + checkout + merge)
4. Only then perform code analysis

### Files updated with Rule 38 references
- `.cursor/rules/workflows/workflow_rules.mdc` — Rule 38 definition
- `.cursor/agents/phoenix-qa.md` — Step 2 before answering
- `.cursor/agents/bug-validator.md` — Step 3 code validation
- `.cursor/agents/cross-dependency-finder.md` — Step 0 Jira-anchored analysis
- `.cursor/agents/hands-off.md` — Steps 2 and constraints
- `.cursor/agents/phoenix-qa.md` — Mandatory workflow Step 2 (Phoenix Q&A)
- `.cursor/agents/bug-validator.md` — Step 3 (bug validation)
- `.cursor/skills/phoenix-commands/SKILL.md` — Summary section

---

## 3. Rule 39 — Confluence Scope for Non-Bug Tickets

### Problem
CrossDependencyFinder and TestCaseGenerator ran broad Confluence searches even for tasks/changes/feedback where the requirements are already defined in the Jira ticket. This added noise and latency without improving analysis quality.

### Solution: Rule 39 [CRITICAL]
Added to `workflow_rules.mdc`. For non-bug tickets:

| Condition | Confluence behavior |
|-----------|-------------------|
| Jira ticket is **Bug** | Full Confluence search (evidence strength per Rule 32) |
| Jira ticket is **Task/Change/Feedback/Feature** + has Confluence links | Fetch ONLY those linked pages via `getConfluencePage` |
| Jira ticket is **Task/Change/Feedback/Feature** + no Confluence links | Skip Confluence entirely — Jira description + codebase only |
| User explicitly asks for broad search | Override: use normal Confluence search |
| PhoenixExpert Q&A | Always does its own Confluence search (unchanged) |

### Files updated with Rule 39 references
- `.cursor/rules/workflows/workflow_rules.mdc` — Rule 39 definition
- `.cursor/agents/cross-dependency-finder.md` — Step 2 Confluence section
- `.cursor/agents/test-case-generator.md` — Step 1 Confluence section
- `.cursor/agents/hands-off.md` — Constraints section

### Commit pushed to `origin/experiments`
- `ff5c5c0` — add Rule 38 (env branch-context) and Rule 39 (task-driven Confluence scope)

---

## Summary of All Commits

| Commit | Branch | Description |
|--------|--------|-------------|
| `9709df9` | `main` | Bug Validator: environment-aware branch switching |
| `6a39c23` | `main` | Bug Validator: UTF-8 encoding fix for Windows CI |
| `ff5c5c0` | `experiments` | Rule 38 (branch-context) + Rule 39 (Confluence scope) |

---

## Test Results

**Unit tests (`test_branch_resolver.py`):** 19/19 passed (0 failures)
- 11 environment detection cases (Georgian + English + default)
- 8 branch picking cases (canonical + release-style + fallback)

**Live dry-run on Phoenix repos:** All 10 repos correctly resolved for all 5 environments.

---

Agents involved: None (direct tool usage)
