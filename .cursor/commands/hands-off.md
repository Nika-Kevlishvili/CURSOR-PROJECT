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
3. **Content:** Each test case .md MUST follow the **Test Case Template**: **`Cursor-Project/config/Test_case_template.md`** (maximally detailed, human-readable: Summary, Scope, Test data, TC-1/TC-2 with Objective, Preconditions, Steps, Expected result, Actual result if bug, References).
4. **Verify on disk:** After generation, check that the folder and .md files exist. If missing, **write them directly** (create folder, create each .md from test case content using the template). Update **`test_cases/Flows/README.md`** (or Objects/README.md) to include the new flow/entity.
4. Note the paths of test case files for the bridge to Playwright.

Reference: `.cursor/commands/test-case-generate.md`, `.cursor/rules/handsoff_playwright_report.mdc` §1.

### Step 4: Create Playwright tests from test cases (bridge) [MANDATORY: energo-ts-test agent]

1. **MUST use EnergoTSTestAgent (energo-ts-test):** The Playwright spec MUST be created by the **energo-ts-test** agent (EnergoTSTestAgent). Do NOT write the spec manually or with ad-hoc code (e.g. custom `getToken()`, custom `apiRequest()`). The agent reads the test case .md content and produces a spec using the **EnergoTS framework** (fixtures: Request, Endpoints, baseFixture, etc.).
2. **Input to agent:** Pass to the energo-ts-test agent: (a) **paths to test case .md files** from Step 3 (e.g. `Cursor-Project/test_cases/Flows/Invoice_cancellation/*.md`), (b) **Jira key and ticket title**, (c) cross_dependency_data or entry points if useful. The agent MUST use this content to derive scenarios, endpoints, steps, and assertions.
3. **Output:** Spec file **`Cursor-Project/EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`** (e.g. `NT-1-invoice-cancellation.spec.ts`). EnergoTS must be on **cursor** branch (Rule ENERGOTS.0). Spec MUST follow project patterns (fixtures, test naming with Jira key, one test per main scenario from the .md).
4. **Verify on disk:** After the agent runs, verify the spec file exists. If the agent did not create it, invoke the agent again with explicit test case paths and Jira context; do not fall back to writing an ad-hoc spec.

Reference: `.cursor/rules/handsoff_playwright_report.mdc` §2, `.cursor/commands/energo-ts-test.md`.

### Step 5: Run Playwright tests

1. **Ensure cursor branch** – In `Cursor-Project/EnergoTS/`, if current branch is not `cursor`, run `git checkout cursor` (Rule ENERGOTS.0).
2. **Ensure fixtures exist:** If `fixtures/token.json` or `fixtures/envVariables.json` are missing, run **global setup first** so tests can load: from `EnergoTS/` run `npx playwright test --project=setup` (requires .env with PORTAL_USER, PASSWORD, DEVAUTHAPI or TESTAUTHAPI). This creates token.json and envVariables.json.
3. **Run tests** from `Cursor-Project/EnergoTS/`: `npx playwright test --grep "<JIRA_KEY>"` or `npx playwright test tests/cursor/<JIRA_KEY>-*.spec.ts`. Use list (or similar) reporter to capture output.
4. **Capture** the run output: which tests ran, which **passed** or **failed**, and for each failure the **reason** (assertion message, status code, error snippet). If tests could not run (e.g. setup failed or token still missing), record "Not run" and the exact reason (e.g. ENOENT token.json, or setup error message).

Reference: `.cursor/commands/energo-ts-run.md`, Rule 36, `.cursor/rules/handsoff_playwright_report.mdc` §5–6.

### Step 6: Build and save report (Step 9 – part 1)

1. **Report filename** = `{JIRA_KEY}.md` (e.g. `REG-123.md`).
2. **Report content** (English, Rule 0.7) – **ONLY Playwright test results** (see `.cursor/rules/handsoff_playwright_report.mdc`). For the **Slack message** (Step 7), build content using the **Slack report template**: **`Cursor-Project/config/Slack_report_template.md`**. The template defines: header (`{JIRA_KEY} – Playwright test results`), Jira/Title/Date/Assignee/Tester, Total passed/failed/skipped, separator, then per test: Test N, Test description, Expected result, Actual result, Test result. Fill all placeholders from Jira and Playwright run. The saved file `reports/YYYY-MM-DD/{JIRA_KEY}.md` may use the same structure (recommended so Slack and file stay in sync).
   - Optional in file: spec path and how to run (e.g. `npx playwright test --grep "<JIRA_KEY>"`).
   - Do NOT fill the report with cross-deps, artifact lists, or long non–test-result sections.
3. **Save** the report to `Cursor-Project/reports/YYYY-MM-DD/{JIRA_KEY}.md` (use current date).

Reference: `Cursor-Project/config/Slack_report_template.md`; `Cursor-Project/agents/Services/reporting_service.py` `save_agent_report(agent_name, filename=...)`.

### Step 7: Send report to Slack (Step 9 – part 2)

**Rule: The report must ALWAYS be sent to BOTH recipients – to the tester (DM) and to #ai-report. Never send to only one.**

1. **Tester:** Get the **tester** for the ticket from Jira (e.g. **Assignee** – use the **display name**). If no tester/assignee is set, use a fallback (e.g. skip tester DM but still send to #ai-report – document in config).
2. **Slack message:** Build the message using the **Slack report template**: **`Cursor-Project/config/Slack_report_template.md`**. The content MUST follow that template (header, Jira/Title/Date/Assignee/Tester, Total, separator, then each test with Test description, Expected result, Actual result, Test result). Use the same content for both recipients.
3. **Find tester on Slack by name (not by ID):** Call **user-slack** MCP `slack_search_users(query: <assignee display name>)` (e.g. `slack_search_users({"query": "nika kevlishvili"})`). Use the **name** from Jira assignee; do NOT use hardcoded Slack user IDs. From the result, take the **User ID** (e.g. `U07A2K9D4J3`). Do **not** add @mention in the message.
4. **Slack – send to BOTH (mandatory):**
   - **To tester (DM):** Call `slack_send_message(channel_id: <user_id from step 3>, message: report_content)`. The user-slack MCP treats `channel_id` = user ID as a DM to that user. Send the **full report** (template-filled content, same as in `reports/YYYY-MM-DD/{JIRA_KEY}.md`). Do NOT use @mention.
   - **To #ai-report:** Call `slack_send_message(channel_id: "C0AK96S1D7X", message: report_content)`. Send the **same full report** (duplicate). **Always use** channel_id **`C0AK96S1D7X`** for #ai-report. Do NOT send only a short summary.
   - **Both sends are required** every time; the report must always be in both places.
5. If message length limit applies (e.g. 5000 chars), send at least the full Playwright-results section (each test: description, expected, actual, result). Message should indicate it is the HandsOff run result for the Jira ticket.

Reference: `Cursor-Project/config/Slack_report_template.md`; user-slack MCP tools (`slack_send_message`, `slack_search_users`, `slack_search_channels`); Jira issue fields for assignee display name; find tester **by name** via `slack_search_users`; then send to **both** tester (channel_id = user_id) and #ai-report (C0AK96S1D7X). Do not add @mention.

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
