# Cursor Commands – Detailed Reference

This document lists every Cursor command and what it can do in detail.

---

## 1. Switch Phoenix repos to an environment branch (current command)

**Command:** `.cursor/commands/switch-phoenix-branches.ps1`
**Doc:** `.cursor/commands/switch-phoenix-branches.md`
**Rule:** `.cursor/rules/integrations/phoenix_branch_switching.mdc` (Rule PHOENIX-SWITCH.0)

**What it does (per repo under `Cursor-Project/Phoenix/`, in order):**
1. `git fetch origin --prune`
2. Verify `origin/<branch>` exists; otherwise mark `missing-remote` and skip.
3. **Discard uncommitted local changes** (`git reset --hard HEAD` + `git clean -fd`) — per workspace policy local Phoenix edits are NOT preserved during a switch.
4. `git checkout -B <branch> origin/<branch>` (creates / re-points local tracking branch).
5. `git reset --hard origin/<branch>` so the local branch matches the latest remote tip.

**Branch mapping (lowercase canonical):**

| `-Environment` | Remote branch        |
|----------------|----------------------|
| `dev`          | `origin/dev`         |
| `dev2`         | `origin/dev2`        |
| `test`         | `origin/test`        |
| `preprod`      | `origin/preprod`     |
| `prod`         | `origin/prod`        |
| `experiments`  | `origin/experiments` |

**Usage:**

```powershell
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment dev
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment test
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment experiments
# Plan only, no destructive actions:
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment dev -DryRun
```

**When Cursor agents must run this:** before environment-sensitive Phoenix code reading — Phoenix Q&A, bug validation (Rule 32), cross-dependency analysis (Rule 35a), test case generation (Rule 35), and the HandsOff flow (Rule 37). See Rule PHOENIX-SWITCH.0.

**Scope:** Only repos under `Cursor-Project/Phoenix/`. Read-only for the remote (no commits, no pushes, no MRs). Phoenix source files remain READ-ONLY for Cursor AI (Rule 0.8 Tier A). EnergoTS is unaffected — that path stays locked to `cursor` (Rule ENERGOTS.0).

> **Legacy note:** the older `/sync`, `!update <branch>`, `!checkout <branch>` triggers and the `git_sync_workflow.mdc` rule are no longer present in this workspace. Use the command above for any Phoenix branch alignment.

---

## 2. Sync workspace repo (Cursor-Project repository only)

**Command:** `.cursor/commands/sync-workspace-repo.ps1`

**What it can do:**
- Sync **only** the workspace root Git repository (the repo that contains `Cursor-Project`). Does **not** touch sub-repos (EnergoTS, Phoenix).
- Stash local changes → `git fetch origin` → merge `origin/&lt;current_branch&gt;` (or report up-to-date/diverged) → restore stash.

**When to use:** Update only the Cursor-Project repo (experiments, main, etc.) with the remote; do not sync EnergoTS or Phoenix.

---

## 3. Pull EnergoTS

**Command:** `.cursor/commands/pull-energots.ps1`  
**Optional:** `-RepoUrl "https://github.com/.../EnergoTS.git"` or `$env:ENERGOTS_REPO_URL`

**What it can do:**
- Resolve `Cursor-Project\EnergoTS`.
- If folder missing or no `.git`: clone when `-RepoUrl` or `ENERGOTS_REPO_URL` is set, then checkout `cursor`.
- If repo exists: stash local changes → `git fetch origin cursor` (and `--all`) → checkout `cursor` → update to `origin/cursor` (merge/reset) → restore stash.
- Never push; read-only for remote.

**When to use:** Get latest `cursor` from remote; or clone EnergoTS and land on `cursor`.

---

## 4. Push EnergoTS

**Command:** `.cursor/commands/push-energots.ps1`  
**Optional:** `-Message "Your commit message"` (default: "Update EnergoTS cursor branch")

**What it can do:**
- Use `Cursor-Project\EnergoTS`; ensure branch is `cursor`.
- If there are uncommitted changes: `git add -A` → `git commit -m <message>`.
- `git push origin cursor` to update GitHub’s EnergoTS cursor branch.

**When to use:** Publish local EnergoTS work to the remote cursor branch.

---

## 5. Sync Cursor with Main

**Command:** `.cursor/commands/sync-cursor-with-main.ps1`  
**Optional:** `-SourceBranch main` (default) or `-SourceBranch staging`

**What it can do:**
- Run in EnergoTS; ensure branch is `cursor`.
- Stash all local changes (including untracked).
- Fetch `origin/main` or `origin/staging`.
- Merge source into `cursor`.
- Auto-resolve conflicts by keeping cursor’s version (both modified → ours; deleted by us → keep deleted; deleted by them → keep file).
- Complete merge commit and restore stash.

**When to use:** Update EnergoTS `cursor` from `main` or `staging` and keep cursor’s changes in conflicts.

---

## 6. Sync Cursor with Staging (Backend-staging)

**Command:** `.cursor/commands/sync-cursor-with-staging.ps1`  
**Optional:** `-ForceSync` to force sync even with uncommitted changes

**What it can do:**
- **Default (smart mode)** when there are uncommitted changes: stay on `cursor` → fetch `origin/cursor` → update local cursor to remote cursor → keep uncommitted changes.
- **When no uncommitted changes (or with -ForceSync):** stash → fetch `origin/Backend-staging` → merge into `cursor` → auto-resolve conflicts (keep cursor version) → complete merge → restore stash.

**When to use:** Either just refresh local cursor from remote, or merge Backend-staging into cursor with conflict resolution.

---

## 7. Update experiments

**Command:** `.cursor/commands/update-experiments.ps1`  
**Optional:** `-Message "Custom commit message"` (default: "Update experiments branch")

**What it can do:**
- Ensure current branch is `experiments` (switch if needed).
- Stage all changes (`git add -A`).
- Commit with given or default message.
- Push to `origin/experiments` (with retries). Uses `GITHUB_TOKEN` if set for non-interactive push.

**When to use:** Commit and push all local changes to the experiments branch.

---

## 8. Update main from experiments

**Command:** `.cursor/commands/update-main-from-experiments.ps1`

**What it can do:**
- Stash uncommitted changes.
- Fetch `origin/experiments` and `origin/main`.
- Checkout `main`.
- Merge `origin/experiments` into main; auto-resolve conflicts by keeping experiments’ version.
- Push `main` to `origin/main`.
- Restore stash.

**When to use:** Merge experiments into main and push.

---

## 9. Phoenix (Phoenix Expert query)

**Trigger:** Phoenix-related questions (routed by Rule 0.2)

**What it can do:**
- Follow **Rule 0.3** (this workspace: no Python `IntegrationService` in chat; use MCP/Jira when external context is needed).
- Search Confluence via MCP (fresh, no cache).
- Search Phoenix codebase (primary source).
- Answer with source priority: codebase > Confluence > general knowledge.
- Optional save: **`Cursor-Project/reports/Chat reports/YYYY/<english-month>/<DD>/PhoenixExpert_{HHMM}.md`** per **`Cursor-Project/reports/README.md`**.

**When to use:** Any question about Phoenix backend, APIs, business logic, or documentation.

---

## 10. Consult (PhoenixExpert consultation)

**Trigger:** Before any task that affects Phoenix (Rule 8)

**What it can do:**
- Validate approach with PhoenixExpert before execution.
- Provide: endpoints, validation rules, permissions, business logic.
- Approve or reject; task cannot proceed without approval (Rule 27).

**When to use:** Before code changes, test runs, API collection generation, or any agent action on Phoenix.

---

## 11. Report (Task report generation)

**Trigger:** User request, **`/report`**, or other explicit need (Rule 0.6 — optional by default)

**What it can do:**
- Save agent-specific report: **`Cursor-Project/reports/Chat reports/<segment>/{AgentName}_{HHMM}.md`** (`<segment>` = `YYYY/<english-month>/<DD>/` per **`Cursor-Project/reports/README.md`**).
- Save summary: **`…/Chat reports/<segment>/Summary_{HHMM}.md`**.
- Use **`Cursor-Project/reports/README.md`** for folder reuse vs creation.

**When to use:** When you want a persisted run log or summary — **not** automatically after every chat task. **HandsOff** alone mandates `{JIRA_KEY}.md` under **HandsOff reports**. Bug validation does **not** auto-save `BugValidation_*.md`; use **`/report`** or ask explicitly.

---

## 12. Bug validate (Bug validation)

**Trigger:** Bug validation requests (Rule 32 – BugFinderAgent)

**What it can do:**
- Follow Rule 0.3; consult PhoenixExpert where Rule 8 applies.
- **Environment + Phoenix:** Resolve env; run **`switch-phoenix-branches.ps1`** (prod: user ack + `-ConfirmProd`) before reading Phoenix code.
- **Confluence:** MCP + REST fallback; classify evidence strength.
- **Swagger:** Run **`update-swagger-specs.ps1`**; cite refreshed or cached **`Cursor-Project/config/swagger/<id>/swagger-spec.json`** for API-level claims.
- **Code:** Search aligned Phoenix codebase; file/line evidence; compare to bug report.
- **Verdict:** Five-verdict matrix (see **`phoenix-bug-validation`** skill); full analysis in chat + Slack **`bug-validation`**. **Does not** auto-generate test cases or run Playwright (use test-case / HandsOff flows separately).
- Save **`BugValidation_{Name}.md`** under **Chat reports** only if the user runs **`/report`** or explicitly requests a file (per **`Cursor-Project/reports/README.md`**).

**When to use:** Validate a bug report against Confluence, OpenAPI, and code before any fix.

---

## 13. Production data reader

**Trigger:** Any production database data question (Rule PDR.0)

**What it can do:**
- Connect to production DB (PostgreSQLProd, readonly_user).
- Analyze any entity: liability, receivable, payment, deposit, invoice, contract, customer, etc.
- Use **PostgreSQLProd MCP**: `connect_db` then `query` with SELECT-only SQL; build analysis manually (no bundled `analyze_entity` helpers in this workspace).
- Explain step-by-step how data was created; offset sequence; reversals; relationships.
- Save report to **Chat reports** `…/<segment>/ProductionDataReaderAgent_{HHMM}.md` per **`Cursor-Project/reports/README.md`**.
- Read-only; no writes.

**When to use:** “How was X created?”, “What is liability X offset with?”, “What is the offset sequence?”, any Prod data or traceability question.

---

## 14. Cross-dependency finder

**Trigger:** Dependency analysis, “what could break”, or before test case generation (Rule 35). **Rule 35a** applies when user gives a Jira/bug/task key.

**What it can do:**
- **Rule 35a:** **Jira MCP + codebase + shallow Confluence** — **no** local merge/`git log`/git sync solely for cross-dep; **technical_details** from Jira + code. **GitLab MR** only if the user explicitly asks.
- Follow Rule 0.3; consult PhoenixExpert if needed (Rule 8).
- Define scope (bug/task/feature): entry points, modules, services.
- Find cross-dependencies: code (imports, APIs, DB, callers, consumers) and Confluence (shallow).
- Produce structured report: scope, entry_points, upstream, downstream, shared, data_entities, integration_points, **what_could_break**, **technical_details**.
- Optionally save to `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope>.json`.
- Output is passed to test-case-generator as `context['cross_dependency_data']`.
- Read-only.

**When to use:** “Find cross dependencies for…”, “What could break if we change…”, “Cross dependencies for BUG-1234”, or before generating test cases.

---

## 15. Test case generate

**Trigger:** “Generate test cases for this bug/task/feature” (Rule 35). Cross-dependency-finder follows Rule 35a (Jira + codebase + shallow Confluence; no local merge/git).

**What it can do:**
- **Step 1 (mandatory):** Run cross-dependency-finder for the same scope (Rule 35a); get report including what_could_break and technical_details; pass as `context['cross_dependency_data']`.
- **Step 2:** Follow Rule 0.3; consult PhoenixExpert; search Confluence and codebase; run **test-case-generator** workflow (subagent/skill) with `cross_dependency_data` from step 1.
- Save test cases under **`Cursor-Project/test_cases/`** per **`test_cases_structure.mdc`**:
  - **`Backend/<Topic_name>.md`** — TC-BE-N only; **always** when test cases are generated.
  - **`Frontend/<Topic_name>.md`** — TC-FE-N only; **only** if user chose Backend+Frontend (TC-FRONTEND-ASK.0 = Yes).
- Legacy **`test_cases/Objects/`**, **`Flows/`** and **`generated_test_cases/`** may appear in older material; prefer **`Backend/`** + **`Frontend/`** for new work.
- One `.md` per group with title, steps, expected result.
- Save reports (Rule 0.6). Read-only for Phoenix code.

**When to use:** Create test scenarios from a bug, task, or feature; always after cross-dependency finder for that scope.

---

## 16. EnergoTS test (EnergoTS test management)

**Trigger:** EnergoTS test creation, modification, analysis (Rule 0.8.1 – may edit only `EnergoTS/tests/`)

**What it can do:**
- Follow Rule 0.3; read Jira task (title, description) via Jira MCP before creating tests.
- Consult PhoenixExpert for API/business logic if needed.
- **Study test:** Analyze a test file (e.g. `tests/billing/...spec.ts`).
- **Create new test:** From Jira ID, domain, fixtures, endpoint, method, payload generator; test name must match Jira task title exactly.
- **Copy and convert:** Copy existing test to new path with conversion rules (e.g. change Jira ID).
- **Analyze patterns / list by domain:** e.g. list tests by domain (e.g. billing).
- Create/modify only `.spec.ts` files in `Cursor-Project/EnergoTS/tests/`; no other code changes.
- Save reports (Rule 0.6).

**When to use:** Create, copy, or analyze EnergoTS Playwright tests; always with Jira context and naming rule.

---

## 17. HandsOff (full automated flow)

**Trigger:** User provides a **Jira ticket** (link, key e.g. REG-123, or name) and types **/HandsOff** or **!HandsOff**.

**What it can do:**
1. **Get Jira ticket** – Parse issue key; call Jira MCP getJiraIssue → description, summary, tester/assignee.
2. **Cross-dependencies** – Run cross-dependency-finder for this Jira key (Rule 35a: Jira + codebase + shallow Confluence; no local merge/git); get cross_dependency_data.
3. **Test cases** – Run test-case-generator; save **`test_cases/Backend/<Topic>.md`** always; **`Frontend/<Topic>.md`** only if TC-FRONTEND-ASK.0 = Yes.
4. **TC quality (Step 3.5)** – **test-case-quality-validator**; 10-axis ≥80/100; max 3 rewrites; **BLOCK** if still failing.
5. **Playwright tests** – **`energo-ts-test`** + **`energo-ts-test/SKILL.md`** → **`EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`**; **`cursor`** branch (Rule ENERGOTS.0).
6. **Spec validation (Step 4.5)** – **playwright-test-validator**; **BLOCK** after 3 failed iterations unless user opts out.
7. **Run tests** – Playwright by Jira key or spec path; capture pass/fail and failure reasons.
8. **Report** – **`HandsOff reports/…/{JIRA_KEY}.md`** + **`playwright-report-detailed.md`** (DPR.0).
9. **Slack** – Three-block text + upload both `.md` files to Tester + **#ai-report**.

**Canonical:** **`.cursor/commands/hands-off.md`**

**When to use:** Run the full pipeline automatically for a Jira ticket: fetch → cross-deps → test cases → create Playwright tests → run → report (save + send to Slack). No user intervention after providing the ticket and /HandsOff.

---

## Summary table

| Command / trigger           | Main action |
|----------------------------|-------------|
| **Sync**                   | Phoenix: fetch / update branch / checkout (GitLab). |
| **Sync workspace repo**   | Workspace root repo only: fetch + merge current branch; no sub-repos (EnergoTS/Phoenix). |
| **Pull EnergoTS**          | Fetch and update local cursor from remote; never push. |
| **Push EnergoTS**          | Commit and push local cursor to GitHub. |
| **Sync cursor with main** | Merge main (or staging) into EnergoTS cursor; auto-resolve conflicts (keep cursor). |
| **Sync cursor with staging** | Smart: update from remote cursor or merge Backend-staging; keep cursor on conflict. |
| **Update experiments**     | Commit all and push to origin/experiments. |
| **Update main from experiments** | Merge experiments into main and push. |
| **Phoenix**                | Answer Phoenix questions (Confluence + codebase). |
| **Consult**                | PhoenixExpert approval before a task. |
| **Report**                 | Save agent and summary reports (Rule 0.6). |
| **Bug validate**           | Validate bug vs Confluence + Swagger/OpenAPI + aligned code (BugFinderAgent); read-only; no TC/Playwright in Rule 32. |
| **Production data reader** | Read and explain Prod DB data (read-only). |
| **Cross-dependency finder** | Find dependencies and what could break; feed test-case-generator. |
| **Test case generate**     | Generate test cases (after cross-dependency finder). |
| **EnergoTS test**          | Create/edit EnergoTS tests in `EnergoTS/tests/` only. |
| **HandsOff**               | Full flow: Jira → cross-deps → test cases → Playwright → run → report (save as Jira key + send to Slack to tester). |

---

*Document generated for Cursor workspace. All commands follow project rules (e.g. Rule 0.6 reports, Rule 0.8 no code edit except EnergoTS tests).*
