# HandsOff – Full Automated Flow

Run the **entire flow** without user intervention when the user provides a **Jira ticket** (link, name, or key) and invokes **`/HandsOff`** or **`!HandsOff`**. Orchestrate: Jira → cross-dependencies → test cases → Playwright tests → run tests → report (save + send to Slack to tester).

## When to Use

- User provides a **Jira ticket** (link, ticket key e.g. REG-123, or ticket name) and types **`/HandsOff`** or **`!HandsOff`**.
- User wants the system to run the full pipeline automatically: fetch ticket → cross-deps → test cases → create Playwright tests → run them → save report named after the ticket → send report to Slack to the tester on the ticket.

## Mandatory Workflow (run in order)

### Step 1: Get Jira ticket and description

1. **IntegrationService** – Call `IntegrationService.update_before_task()` first (Rule 11).
2. **Parse input** – From the user message extract the Jira **issue key** (e.g. REG-123, BUG-456). If user gave a link, parse the key from the URL. If user gave a screenshot only, use vision/OCR if available to extract the key.
3. **Get cloudId** – Use Jira MCP (e.g. `getAccessibleAtlassianResources` or equivalent) to obtain cloudId if needed.
4. **Fetch ticket** – Call **Jira MCP** `getJiraIssue(cloudId, issueIdOrKey)` to get the ticket **summary** (title) and **description**. Store for later: description (for cross-deps and test case generator), and **tester/assignee** (for Step 9 Slack).
5. If ticket cannot be fetched, stop and report error.

### Step 2: Cross-dependencies (Rule 35a)

1. Run **cross-dependency-finder** for this Jira key (same scope = ticket description).
2. Finder MUST follow **Rule 35a**: (a) **merge history lookup** for this Jira key (local git + GitLab); (b) **conditional sync** of the branch if a merge exists for this ticket; (c) include **technical_details** from merge (MR/commits, changed files) in the output.
3. Cross-dependency-finder may consult **PhoenixExpert**. Obtain the full structured output: scope, entry_points, upstream, downstream, what_could_break, **technical_details**.
4. Pass this output as **cross_dependency_data** to the next step.

Reference: `.cursor/commands/cross-dependency-finder.md`, Rule 35a in `.cursor/rules/workflow_rules.mdc`.

### Step 3: Test case generator

1. Run **test-case-generator** with:
   - **prompt** = Jira ticket description (and summary if useful).
   - **prompt_type** = `'bug'` or `'task'` as appropriate.
   - **context** = `{ 'cross_dependency_data': <output from Step 2> }` (and codebase_findings / confluence_data if collected).
2. Test cases MUST be saved in the **required folder** (see `.cursor/rules/test_cases_structure.mdc`): **`Cursor-Project/test_cases/Flows/<Flow_name>/`** or **`Cursor-Project/test_cases/Objects/<Entity>/`** (e.g. `test_cases/Flows/Invoice_cancellation/`). Use thematic names with underscores.
3. **Verify on disk:** After generation, check that the folder and .md files exist. If missing, **write them directly** (create folder, create each .md from test case content). Update **`test_cases/Flows/README.md`** (or Objects/README.md) to include the new flow/entity.
4. Note the paths of test case files for the bridge to Playwright.

Reference: `.cursor/commands/test-case-generate.md`, `.cursor/rules/handsoff_playwright_report.mdc` §1.

### Step 4: Create Playwright tests from test cases (bridge)

1. **Path:** Create (or ensure) Playwright spec in **`Cursor-Project/EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`** (e.g. `NT-1-invoice-cancellation.spec.ts`). EnergoTS must be on **cursor** branch (Rule ENERGOTS.0).
2. **Bridge:** Read test case .md file(s) from `Cursor-Project/test_cases/Flows/...` or `test_cases/Objects/...`. From content derive endpoints and steps; write a spec that references the Jira key in describe/test titles and covers the ticket (e.g. API calls, assertions).
3. **Verify on disk:** After creation, verify the spec file exists. If the orchestrator/subagent did not create it, **write the spec file directly** with at least one test per main scenario (e.g. create request, happy path). Do not assume "created" without checking.

Reference: `.cursor/rules/handsoff_playwright_report.mdc` §2, `.cursor/commands/energo-ts-test.md`.

### Step 5: Run Playwright tests

1. **Ensure cursor branch** – In `Cursor-Project/EnergoTS/`, if current branch is not `cursor`, run `git checkout cursor` (Rule ENERGOTS.0).
2. **Ensure fixtures exist:** If `fixtures/token.json` or `fixtures/envVariables.json` are missing, run **global setup first** so tests can load: from `EnergoTS/` run `npx playwright test --project=setup` (requires .env with PORTAL_USER, PASSWORD, DEVAUTHAPI or TESTAUTHAPI). This creates token.json and envVariables.json.
3. **Run tests** from `Cursor-Project/EnergoTS/`: `npx playwright test --grep "<JIRA_KEY>"` or `npx playwright test tests/cursor/<JIRA_KEY>-*.spec.ts`. Use list (or similar) reporter to capture output.
4. **Capture** the run output: which tests ran, which **passed** or **failed**, and for each failure the **reason** (assertion message, status code, error snippet). If tests could not run (e.g. setup failed or token still missing), record "Not run" and the exact reason (e.g. ENOENT token.json, or setup error message).

Reference: `.cursor/commands/energo-ts-run.md`, Rule 36, `.cursor/rules/handsoff_playwright_report.mdc` §5–6.

### Step 6: Build and save report (Step 9 – part 1)

1. **Report filename** = `{JIRA_KEY}.md` (e.g. `REG-123.md`).
2. **Report content** (English, Rule 0.7) – **ONLY Playwright test results** (see `.cursor/rules/handsoff_playwright_report.mdc`):
   - Jira ticket key and title; spec file path and how to run (e.g. `npx playwright test --grep "<JIRA_KEY>"`).
   - **Per test:** (1) Test name, (2) **What is verified** (detailed), (3) **Steps**, (4) **Result:** Passed / Failed / Not run, (5) **If failed:** reason (assertion message, status, response/error).
   - Do NOT fill the report with cross-deps, artifact lists, or long non–test-result sections.
3. **Save** the report to `Cursor-Project/reports/YYYY-MM-DD/{JIRA_KEY}.md` (use current date).

Reference: `Cursor-Project/agents/Services/reporting_service.py` `save_agent_report(agent_name, filename=...)`.

### Step 7: Send report to Slack (Step 9 – part 2)

1. **Tester:** Get the **tester** for the ticket from Jira (e.g. **Assignee** or custom field). If no tester/assignee is set, use a fallback (e.g. skip Slack or send to a default channel – document in config).
2. **Slack – two recipients (same full report to both):**
   - **To tester:** Use **user-slack** MCP `slack_send_message` to send the **full report** content to the tester (see `.cursor/rules/handsoff_playwright_report.mdc`). Send the same content as in `reports/YYYY-MM-DD/{JIRA_KEY}.md` (or at least the full Playwright-results section). Do NOT send only a short summary.
   - **To AI report channel:** Send the **same full report** (duplicate) to the **AI report** channel. Use `slack_search_channels` to resolve the channel by name (e.g. "AI report") to get `channel_id`, then call `slack_send_message(channel_id, report_content)`. Same content as saved in `reports/YYYY-MM-DD/{JIRA_KEY}.md`.
3. If message length limit applies (e.g. 5000 chars), send the full Playwright-results section (each test: what is verified, result, failure reason). Message should indicate it is the HandsOff run result for the Jira ticket.

Reference: user-slack MCP tools (`slack_send_message`, `slack_search_channels`); Jira issue fields for assignee/custom tester.

### Step 8: Agent questions after report (with attribution)

1. **Collect questions:** After the report is saved and sent (Steps 6–7), **each participating agent** (CrossDependencyFinder, TestCaseGenerator, EnergoTSTestAgent, PhoenixExpert if consulted, etc.) MUST contribute **questions related to the task** when needed (clarifications, edge cases, follow-ups, open points).
2. **Attribution:** Each question MUST be tagged with the agent that asked it. Format: `[AgentName]: <question text>` (e.g. `[TestCaseGenerator]: Should cancellation be tested for already-reversed invoices?`).
3. **Send after report:** Send the list of agent questions **after** the report (e.g. a second Slack message to the same recipients: tester and AI report channel). Do not replace the report; questions are a follow-up.
4. If an agent has no questions, it may contribute none; only agents that participated and have relevant questions need to contribute.

Reference: `.cursor/rules/handsoff_playwright_report.mdc` §7.

## Constraints

- **READ-ONLY** for Phoenix application code; do not modify production code (Rule 0.8). Only generated test files in `EnergoTS/tests/` may be created/modified (Rule 0.8.1).
- All report and test case content in **English** (Rule 0.7).
- **EnergoTS:** Use only the **cursor** branch for running and creating tests (Rule ENERGOTS.0).
- **Rule 35/35a:** Cross-dependency-finder must run before test case generator; merge lookup and technical_details are mandatory when a Jira key is provided.

## Response Requirements

- While the flow runs, you may briefly confirm each step (e.g. "Step 1: Fetched REG-123…", "Step 2: Cross-deps done…").
- At the end, summarize: Jira key, tests run, pass/fail counts, report path, and that the report was sent to Slack (to tester and to AI report channel; or fallback if no tester).
- End with: **"Agents involved: HandsOff (orchestrator), CrossDependencyFinderAgent, TestCaseGeneratorAgent, EnergoTSTestAgent, EnergoTS Playwright Test Runner"** (and PhoenixExpert if consulted).

## Generate Reports (Rule 0.6)

- The HandsOff run report is saved as `Cursor-Project/reports/YYYY-MM-DD/{JIRA_KEY}.md` with test results.
- Optionally save an agent/summary report for the orchestrator run to the same date folder (e.g. `HandsOff_Summary_{HHMM}.md` or use ReportingService).

## Example Triggers

- User: "REG-123 /HandsOff"
- User: "https://jira.example.com/browse/REG-123 !HandsOff"
- User: "Run HandsOff for this ticket" (with ticket key or link in the same message)
