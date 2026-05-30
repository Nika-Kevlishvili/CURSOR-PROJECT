# HandsOff – Full Automated Flow

**Canonical checklist (Rule 37):** This file remains the **full** operational procedure for HandsOff orchestrators. Orchestrator role summary and agent invocation order: **`.cursor/agents/hands-off.md`**.

Run the **entire flow** without user intervention when the user provides a **Jira ticket** (link, name, or key) and invokes **`/HandsOff`** or **`!HandsOff`**. Orchestrate: Jira → cross-dependencies → test cases → Playwright tests → run tests → report (save + send to Slack to tester).

**Slack:** This command implements **path 2** (HandsOff) in **`Cursor-Project/config/template/Slack_reporting_paths.md`**. Path 1 = bug validation (Rule 32, unchanged). Path 3 = user-triggered scoped Playwright Slack → **`.cursor/commands/send-playwright-results-slack.md`**.

## When to Use

- User provides a **Jira ticket** (link, ticket key e.g. REG-123, or ticket name) and types **`/HandsOff`** or **`!HandsOff`**.
- User wants the system to run the full pipeline automatically: fetch ticket → cross-deps → test cases → create Playwright tests → run them → save report named after the ticket → send report to Slack to the tester on the ticket.

## Mandatory Workflow (run in order)

### Step 1: Get Jira ticket and description + align Phoenix branches (Rule PHOENIX-SWITCH.0)

1. **Rule 0.3** — No Python `IntegrationService` in this workspace; follow MCP/Jira when needed (see `.cursor/rules/main/core_rules.mdc`).
2. **Parse input** – From the user message extract the Jira **issue key** (e.g. REG-123, BUG-456). If user gave a link, parse the key from the URL. If user gave a screenshot only, use vision/OCR if available to extract the key.
3. **Get cloudId** – Use Jira MCP (e.g. `getAccessibleAtlassianResources` or equivalent) to obtain cloudId if needed.
4. **Fetch ticket** – Prefer **Jira MCP** `getJiraIssue(cloudId, issueIdOrKey)` (use `expand: "names"` to resolve field names). If Jira MCP is unavailable or fails after retries, use the **REST read fallback** per **`.cursor/rules/integrations/jira_rest_fallback.mdc`**. Load **`.cursor/skills/jira-evidence/SKILL.md`** for custom fields, attachments, and linked Confluence (Rule 44). Disclose **`Jira source: REST fallback …`** in the run log / report notes when REST was used. For **Tester**, read **only** from the Jira custom field **"Tester"**: in this project that is **`customfield_10095`** (single user picker). Use that user’s **displayName** for Slack lookup in Step 7. **Do NOT use** Assignee, **customfield_11151** (BA), or any other field as Tester. If `customfield_10095` is null, there is no Tester – send report only to #ai-report. Store for later: description, Tester display name (from customfield_10095 only), and resolved environment.
5. **Resolve environment + align Phoenix branches** – **MANDATORY resolver call:** invoke **`environment-resolver`** (EnvironmentResolverAgent) with Jira ticket context first. Use only the resolved environment (`dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`) for branch alignment. If ambiguity remains, EnvironmentResolverAgent must ask the user via questionnaire (Rule CONF.0) before continuing. **Prod safety gate (Rule PHOENIX-SWITCH.0 §1a):** if the resolved env is `prod`, FIRST tell the user that aligning to `origin/prod` will discard any uncommitted Phoenix edits and force-reset every Phoenix repo, then wait for explicit user acknowledgement. Only then call the script with `-ConfirmProd`. For non-prod envs, run without `-ConfirmProd`:
   - `powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment <env>` (add ` -ConfirmProd` for `prod` only, after user ack)
   - Aligns every `Cursor-Project/Phoenix/*` repo to `origin/<branch>` (latest tip). Local Phoenix edits are DISCARDED; Phoenix files remain READ-ONLY (Rule 0.8 Tier A).
   - Inspect exit code: `0` proceed; `2` proceed but include a "mixed alignment state" note in the HandsOff report; `3` STOP the flow and ask the user to fix connectivity / VPN / credentials before retrying.
   - Capture the per-repo alignment outcome AND the exit code — include it in the final HandsOff report so the tester can see the exact code state used by Steps 2–5.
6. **Pass env forward**: include the resolved environment + alignment exit code in the prompts to cross-dependency-finder and test-case-generator so they can apply subagent reuse (Rule PHOENIX-SWITCH.0 §7a) and skip a redundant alignment.
7. If ticket cannot be fetched, stop and report error.

### Step 2: Cross-dependencies (Rule 35a)

1. Run **cross-dependency-finder** for this Jira key (same scope = ticket description).
2. Finder MUST follow **Rule 35a**: **Jira MCP + codebase + shallow Confluence** — no local merge-history archaeology/git-snapshot; **technical_details** from ticket + code (GitLab MR only if user explicitly asked).
3. Cross-dependency-finder may consult **PhoenixExpert**. Obtain the full structured output: scope, entry_points, upstream, downstream, what_could_break, **technical_details**.
4. Pass this output as **cross_dependency_data** to the next step.

Reference: `.cursor/skills/cross-dependency-finder/SKILL.md`, `.cursor/agents/cross-dependency-finder.md`, Rule 35a in `.cursor/rules/workflows/workflow_rules.mdc`.

### Step 3: Test case generator (comprehensive coverage – mandatory)

**Canonical procedure:** **`.cursor/skills/test-case-generator/SKILL.md`**. **Gates (do not duplicate here):** `.cursor/rules/workspace/test_cases_structure.mdc` — TC-ENV-ASK.0 → TC-FRONTEND-ASK.0 → Phoenix align (Step 1 should have completed env + alignment).

1. Run **test-case-generator** with Jira description/summary, `prompt_type` (`bug`|`task`), `context['cross_dependency_data']` from Step 2.
2. **Coverage (CRITICAL):** exhaustive positive/negative/edge/regression per SKILL §5 — not a minimal random set.
3. **Outputs:** `Cursor-Project/test_cases/Backend/<Topic>.md` always; `Frontend/<Topic>.md` only if TC-FRONTEND scope = Backend+Frontend. Template: **`Test_case_template.md`**. Playwright instructions pack per SKILL.
4. Verify Backend file on disk; verify Frontend only when scope includes it. Note paths for Step 4.

Reference: `.cursor/agents/test-case-generator.md`, `handsoff_playwright_report.mdc` §1.

### Step 3.5: Test case quality validation [MANDATORY]

**Canonical procedure:** **`.cursor/skills/test-case-quality-validator/SKILL.md`**. Invoke **test-case-quality-validator** with `topic_name`, `backend_path`, optional `frontend_path`. **≥80/100** on 10-axis rubric; max **3** iterations; **BLOCK** before Step 4 if still failing.

Reference: Rule 35 Step 2.5; `handsoff_playwright_report.mdc` §1.5.

### Step 4: Create Playwright tests from test cases (bridge) [MANDATORY: energo-ts-test agent]

**Canonical procedure:** **`.cursor/skills/energo-ts-test/SKILL.md`** (Swagger refresh Rule 41, instructions pack, reference specs, no `beforeAll` Rule 40).

1. Invoke **energo-ts-test** with Backend TC path (+ Frontend when exists), Jira key/title, cross_dependency_data if useful.
2. **Output:** `Cursor-Project/EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts` on **cursor** branch. **1:1** TC coverage (`test()` or `test.skip()` per TC).
3. Verify spec exists and TC count matches.

Reference: `.cursor/agents/energo-ts-test.md`, `handsoff_playwright_report.mdc` §2.

### Step 4.5: Validate Playwright tests (quality gate) [MANDATORY]

**Canonical procedure:** **`.cursor/skills/playwright-test-validator/SKILL.md`**. Invoke **playwright-test-validator** with TC paths + spec path + Jira key. **≥80/100**; max **3** iterations; **BLOCK** before Step 5 unless user explicitly opts out.

Reference: `handsoff_playwright_report.mdc` §2.

### Step 5: Run Playwright tests

1. **Ensure cursor branch** – In `Cursor-Project/EnergoTS/`, if current branch is not `cursor`, run `git checkout cursor` (Rule ENERGOTS.0).
2. **Ensure fixtures exist:** If `fixtures/token.json` or `fixtures/envVariables.json` are missing, run **global setup first** so tests can load: from `EnergoTS/` run `npx playwright test --project=setup` (requires .env with PORTAL_USER, PASSWORD, DEVAUTHAPI or TESTAUTHAPI). This creates token.json and envVariables.json.
3. **Run tests** from `Cursor-Project/EnergoTS/`: `npx playwright test --grep "<JIRA_KEY>"` or `npx playwright test tests/cursor/<JIRA_KEY>-*.spec.ts`. Use list (or similar) reporter to capture output.
4. **Capture** the run output: which tests ran, which **passed** or **failed**, and for each failure the **reason** (assertion message, status code, error snippet). If tests could not run (e.g. setup failed or token still missing), record "Not run" and the exact reason (e.g. ENOENT token.json, or setup error message).

Reference: `.cursor/skills/energo-ts-run/SKILL.md`, `.cursor/agents/energo-ts-run.md`, Rule 36, `.cursor/rules/workflows/handsoff_playwright_report.mdc` §5–6.

### Step 6: Build and save report (Step 9 – part 1)

**Reporting (three parts):** (1) **Smart `{JIRA_KEY}.md`** — TC mapping, links, expected vs actual, meets expectation (`Playwright_run_detailed_report_template.md`). (2) **Machine `playwright-report-detailed.md`** — step/annotation detail from **`playwright-report.json`** via **`generate-detailed-report.mjs`** (Rule **DPR.0** — mandatory when JSON exists). (3) **Slack (Step 7)** — short three-block text + **file uploads** of **both** `.md` files to Tester + **#ai-report** (`C0AK96S1D7X`). Follow **`Slack_report_summary_short_template.md`** for chat body only.

1. **Detailed report filename** = `{JIRA_KEY}.md` (e.g. `REG-123.md`).
2. **Detailed report content** (English, Rule 0.7) — **`Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md`**: run metadata; for **each** executed test — Playwright title, **which TC-BE-N / TC-FE-N** it covers (from titles + test case `.md` from Step 3), **created entity links** (URLs or `{METHOD} path + id` from responses / `test.info().annotate`), **expected** vs **actual**, **meets expectation** (Yes/No/Not run). Map failures to TC **Expected result** lines from the test case files when possible.
   - Sources: Playwright stdout/HTML/`playwright-report.json`, test case `.md` files from Step 3, Jira fields from Step 1.
   - Do NOT fill this file with cross-deps-only narrative; focus on run outcomes and traceability to TCs.
3. **Save** the detailed report to **`HandsOff reports`** under **`YYYY/<english-month>/<DD>/{JIRA_KEY}.md`** per **`Cursor-Project/reports/README.md`** (Rule 37).
4. **Machine detailed Markdown (mandatory when JSON exists):** From **`Cursor-Project/EnergoTS`**, if **`playwright-report.json`** exists, run **`node ../config/playwright/generate-detailed-report.mjs`** (writes **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** next to the JSON). If JSON is missing, skip and record in optional **Notes** (Slack / report) why machine detail was not generated.

Reference: `Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md`; `Slack_report_summary_short_template.md`; Rule 37 — Rule 0.6 exception; no Python `ReportingService` in this workspace.

### Step 7: Send report to Slack (Step 9 – part 2)

**Rule [CRITICAL]: Delivery is ONLY to (1) the Tester (DM) and (2) the #ai-report channel. Do NOT send to Assignee or anyone else.**

**A. Slack text (three blocks only):** Build with **`Slack_report_summary_short_template.md`** — (1) `{JIRA_KEY} – Playwright test results`, (2) `Jira:` / `Title:` / `Date:` / `Assignee:` / `Tester:`, (3) `Total: … passed, … failed, … skipped.` Optional **`Notes:`** (environment, MCP gaps, or **`Full narrative:`** + path if upload cannot run). **Do not** add per-test bullets to the chat body.

**B. Slack file attachments (MANDATORY when token exists):** Upload **both** (when the second exists): (i) **`{JIRA_KEY}.md`** from Step 6, and (ii) **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** if Step 6 produced it. For **each** destination — Tester (DM, if any) and **#ai-report** (`C0AK96S1D7X`) — run **`upload-file-to-slack.ps1`** **once per file** (up to **four** script runs: two files × two destinations). Use **`SLACK_API_TOKEN`** or **`SLACK_BOT_TOKEN`** (`files:write`); optional **`_run-upload-with-dotenv.ps1`** loads token from **`EnergoTS/.env`**. Do **not** paste full report bodies in chat. **Incomplete:** short Slack only, or only one file uploaded, while token exists and both files exist.

**C. Optional long narrative in chat:** Only if the user explicitly requests — paste **`Slack_report_template.md`** (rare).

1. **Tester:** From **`customfield_10095`** only; **`slack_search_users`** → User ID. No Tester → skip DM; still upload + post to #ai-report.
2. **Send text:** **`slack_send_message`** — same three-block body (+ optional Notes) to Tester (if any) and **`C0AK96S1D7X`**.
3. **Upload files:** For **each** destination (Tester ID if any, then **`C0AK96S1D7X`**), run **`upload-file-to-slack.ps1`** with absolute **`-FilePath`** to (a) the HandsOff **`{JIRA_KEY}.md`**, then (b) **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** if that file exists.

Reference: `Slack_report_summary_short_template.md`; `Cursor-Project/config/slack/README.md`; optional long Slack: `Slack_report_template.md`; user-slack MCP.

### Step 8: Agent questions after report (with attribution)

1. **Collect questions:** After the report is saved and sent (Steps 6–7), **each participating agent** (CrossDependencyFinder, TestCaseGenerator, EnergoTSTestAgent, PlaywrightTestValidatorAgent, PhoenixExpert if consulted, etc.) MUST contribute **questions related to the task** when needed (clarifications, edge cases, follow-ups, open points).
2. **Attribution:** Each question MUST be tagged with the agent that asked it. Format: `[AgentName]: <question text>` (e.g. `[TestCaseGenerator]: Should cancellation be tested for already-reversed invoices?`).
3. **Send after report:** Send the list of agent questions **after** the report (e.g. a second Slack message to the same recipients: tester and AI report channel). Do not replace the report; questions are a follow-up.
4. If an agent has no questions, it may contribute none; only agents that participated and have relevant questions need to contribute.

Reference: `.cursor/rules/workflows/handsoff_playwright_report.mdc` §7.

## Constraints

- **READ-ONLY** for Phoenix application code; do not modify production code (Rule 0.8). Only generated test files in `EnergoTS/tests/` may be created/modified (Rule 0.8.1).
- All report and test case content in **English** (Rule 0.7).
- **EnergoTS:** Use only the **cursor** branch for running and creating tests (Rule ENERGOTS.0).
- **Rule 35/35a:** Cross-dependency-finder must run before test case generator (Jira + codebase + shallow Confluence; no local merge-history archaeology/git-snapshot); **technical_details** when a Jira key is provided.

## Response Requirements

- While the flow runs, you may briefly confirm each step (e.g. "Step 1: Fetched REG-123…", "Step 2: Cross-deps done…").
- At the end, summarize: Jira key, tests run, pass/fail counts, report path, and that the report was sent to Slack only to Tester (DM) and #ai-report (never to Assignee or anyone else; or only to #ai-report if no Tester).
- End with: **"Agents involved: HandsOff (orchestrator), CrossDependencyFinderAgent, TestCaseGeneratorAgent, TestCaseQualityValidatorAgent, EnergoTSTestAgent, PlaywrightTestValidatorAgent, EnergoTS Playwright Test Runner"** (and PhoenixExpert if consulted).

## Report file (Rule 37 / Rule 0.6 exception)

- **Required:** `HandsOff reports/…/YYYY/<english-month>/<DD>/{JIRA_KEY}.md` per **`Cursor-Project/reports/README.md`** with Playwright test results (for Slack and audit).
- **Optional:** extra orchestrator notes in the same folder only if useful — no Python ReportingService.

## Example Triggers

- User: "REG-123 /HandsOff"
- User: "https://jira.example.com/browse/REG-123 !HandsOff"
- User: "Run HandsOff for this ticket" (with ticket key or link in the same message)
