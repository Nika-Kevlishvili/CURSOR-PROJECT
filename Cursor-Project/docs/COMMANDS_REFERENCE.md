# Cursor Commands – Detailed Reference

This document lists every Cursor command and what it can do in detail.

---

## 1. Sync (Git – Phoenix from GitLab)

**Trigger:** `/sync` or `!sync`, `!update <branch>`, `!checkout <branch>`

**What it can do:**
- **!sync** – Fetch all Phoenix projects from GitLab (`Cursor-Project/Phoenix/`): stash local changes → `git fetch origin --all` and `--prune` → unstash.
- **!update &lt;branch&gt;** – Update the given branch (dev, dev2, dev-fix, test) in every Phoenix repo: stash → fetch → if behind, merge `origin/&lt;branch&gt;`; if diverged, stop and ask you.
- **!checkout &lt;branch&gt;** – In every Phoenix repo: stash → fetch → checkout &lt;branch&gt; (or create tracking branch) → unstash.

**Scope:** Only repos under `Cursor-Project/Phoenix/`. Read-only for remote (no push). Uses token from `git_sync_workflow.mdc`.

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
- **Step 1 – Confluence:** Search Confluence via MCP; check if bug description matches docs; report correct/incorrect/partially correct.
- **Step 2 – Code:** Search codebase; check if implementation matches expected behavior; report satisfies/does not satisfy.
- **Step 3 – Conclusion:** Combine findings; conclude if bug is valid; suggest fix but do not implement (read-only).
- Full analysis in chat; save **`BugValidation_{Name}.md`** under **Chat reports** only if the user runs **`/report`** or explicitly requests a file (per **`Cursor-Project/reports/README.md`**).

**When to use:** Validate a bug report against Confluence and code before any fix.

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
  - **`Objects/<Entity>/`** – e.g. `Product_contract/Create.md`.
  - **`Flows/<Flow_name>/`** – e.g. `Contract_termination/Multi_version_termination_date.md`.
- Legacy **`generated_test_cases/`** may still exist in older material; prefer **`test_cases/`** for new work.
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
3. **Test cases** – Run test-case-generator with ticket description and cross_dependency_data; save under `Cursor-Project/test_cases/Flows/` or `Objects/` (per template).
4. **Playwright tests** – Follow **`.cursor/agents/energo-ts-test.md`**: map test case `.md` → spec with EnergoTS framework; output **`EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`**; stay on **`cursor`** branch (Rule ENERGOTS.0). No Python `get_energo_ts_test_agent()` in this workspace.
5. **Run tests** – Run Playwright tests (e.g. by Jira key or newly created file); capture pass/fail and failure reasons.
6. **Report (Step 9)** – Save report as **`HandsOff reports/…/YYYY/<english-month>/<DD>/{JIRA_KEY}.md`** per **`Cursor-Project/reports/README.md`** with: Jira key, title, tests run, per-test pass/fail and reason.
7. **Slack** – Send **full** report per **`Slack_report_template.md`** to **Tester** (Jira custom field `customfield_10095`, DM via `slack_search_users`) and **#ai-report** (`C0AK96S1D7X`) only — see **`handsoff_playwright_report.mdc`**.

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
| **Bug validate**           | Validate bug vs Confluence + code (BugFinderAgent); read-only. |
| **Production data reader** | Read and explain Prod DB data (read-only). |
| **Cross-dependency finder** | Find dependencies and what could break; feed test-case-generator. |
| **Test case generate**     | Generate test cases (after cross-dependency finder). |
| **EnergoTS test**          | Create/edit EnergoTS tests in `EnergoTS/tests/` only. |
| **HandsOff**               | Full flow: Jira → cross-deps → test cases → Playwright → run → report (save as Jira key + send to Slack to tester). |

---

*Document generated for Cursor workspace. All commands follow project rules (e.g. Rule 0.6 reports, Rule 0.8 no code edit except EnergoTS tests).*
