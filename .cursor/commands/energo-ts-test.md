# EnergoTS Test Management Command

Route EnergoTS test-related queries to EnergoTSTestAgent (Rule 0.8.1 exception for test file modifications).

## When to Use

Use this command when the user asks about:
- EnergoTS tests
- Creating new tests in EnergoTS
- Modifying tests in EnergoTS
- Copying/converting tests
- Analyzing test patterns
- Studying test files
- Test management in EnergoTS project

## Mandatory Workflow:

0. **Playwright instructions:** Before creating or editing EnergoTS `.spec.ts`, the agent MUST read **`Cursor-Project/config/playwright_generation/playwright instructions/`** in order: `project-description.md`, `general-rules.md`, `test-writing-rules.instructions.md`, `SKILL.md`, then other `*.md` in that folder alphabetically (**ignore** `__MACOSX` / `._*`). Specs MUST comply with that pack plus **EnergoTSTestAgent** Jira title naming when they differ (see agent doc).
1. **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed.
2. **Read Jira Task** - ALWAYS read Jira task title and description BEFORE creating test
3. **Clarify Requirements** - Ask clarifying questions if test requirements are unclear
4. **PhoenixExpert Consultation** - Consult PhoenixExpert if needed for API/business logic understanding (Rule 0.4)
5. **EnergoTSTestAgent** - Use EnergoTSTestAgent for all test operations ONLY after full understanding
6. **Report (optional)** - Summarize in chat; save markdown under `reports/` only if the user asks (Rule 0.6 default)

## Available Operations

Delegate to **`.cursor/agents/energo-ts-test.md`**. There is **no** Python `get_energo_ts_test_agent` in this workspace.

- **Study test** — read/analyze existing `.spec.ts` under `EnergoTS/tests/`.
- **Create test** — write new `.spec.ts` under `EnergoTS/tests/` using project fixtures after Jira/requirements are clear.
- **Copy / convert** — adapt specs path-to-path under `EnergoTS/tests/`.
- **Patterns / list by domain** — use `grep`/filesystem search from `EnergoTS/tests/`.

## HandsOff bridge: create Playwright tests FROM test cases [CRITICAL when invoked from HandsOff]

When the **HandsOff** flow invokes the energo-ts-test agent (Step 4), the agent receives **test case .md paths** (e.g. `Cursor-Project/test_cases/Backend/Invoice_cancellation.md` and `Cursor-Project/test_cases/Frontend/Invoice_cancellation.md`) and **Jira key + ticket title**. The agent MUST:

0. **Read** **`Cursor-Project/config/playwright_generation/playwright instructions/`** (full set per Mandatory Workflow step 0) before writing the spec.
1. **Read** both test case .md files (Backend and Frontend) and parse scenarios (TC-BE-N from Backend, TC-FE-N from Frontend), steps, expected results, and entry points (endpoints).
2. **Map** each scenario to a Playwright test: use project **fixtures** (Request, Endpoints, baseFixture, etc.) and **project patterns**; do NOT write custom `getToken()`, `apiRequest()`, or inline auth/request helpers unless they already exist in the framework.
3. **Produce** a single spec file **`EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`** with:
   - `test.describe('…')` containing the Jira key and ticket title;
   - one `test('…')` per main scenario from the .md, with test names including the Jira key (e.g. `[NT-1]: …`);
   - API calls and assertions derived from the test case steps and expected results.
4. **Write** the spec file directly from the .md (endpoints, methods, scenarios). If multiple scenarios share one endpoint, one `test()` per scenario; match existing EnergoTS style and the **playwright instructions** pack.

Reference: `.cursor/commands/hands-off.md` Step 4; `.cursor/rules/workflows/handsoff_playwright_report.mdc` §2.

## Test Creation Workflow [CRITICAL - MANDATORY]

**ABSOLUTE REQUIREMENT**: Before creating ANY test, you MUST follow this workflow:

### Step 1: Read Jira Task Information
- **ALWAYS** read Jira task title and description FIRST
- Use MCP Jira tools to fetch task details: `mcp_Jira_getJiraIssue(cloudId, issueIdOrKey)`
- Extract: task title, description, acceptance criteria, business requirements

### Step 2: Understand Requirements
- Analyze task description to understand what needs to be tested
- Identify: endpoints, HTTP methods, payloads, expected behavior, edge cases
- Determine: domain, fixtures needed, test steps required

### Step 3: Ask Clarifying Questions (if needed)
- **IF** you cannot determine exactly what needs to be tested from task description
- **THEN** ask clarifying questions BEFORE starting test creation:
  - What endpoint should be tested?
  - What HTTP method (POST, GET, PUT, DELETE)?
  - What is the expected behavior?
  - Are there specific test scenarios or edge cases?
  - What domain does this belong to?
  - What fixtures are needed?
  - Are there prerequisite steps (create customer, POD, contract, etc.)?

### Step 4: Start Test Creation
- **ONLY** after you have complete understanding of requirements
- **ONLY** after all clarifying questions are answered
- Use exact Jira task title as test name (see Test Naming Rule below)

### Example Workflow:
```
User: "Create test for REG-1027"
→ Step 1: Read Jira task REG-1027 (title + description)
→ Step 2: Analyze requirements
→ Step 3: If unclear → Ask: "What endpoint should be tested? What is the expected behavior?"
→ Step 4: After clarification → Create test with exact task title
```

## Test Naming Rule [CRITICAL]

**ABSOLUTE REQUIREMENT**: Test name MUST ALWAYS be EXACTLY the same as the Jira task title.

- When creating a test from Jira ticket (e.g., REG-1027):
  - **Jira Task Title**: "For Volumes - Contract/Customer/POD Level"
  - **Test Name**: `[REG-1027]: For Volumes - Contract/Customer/POD Level`
  - **NO modifications** to the task title
  - **NO additions** like "| Happy path" unless explicitly in the task title
  - **NO abbreviations** or simplifications

- Format: `test('[REG-XXX]: {Exact Jira Task Title}', async ({...}) => {`

- Example:
  ```typescript
  // ✅ CORRECT - Exact match with Jira task title
  test('[REG-1027]: For Volumes - Contract/Customer/POD Level', async ({...}) => {
  
  // ❌ WRONG - Modified title
  test('[REG-1027]: For Volumes | Happy path', async ({...}) => {
  ```

## Permissions (Rule 0.8.1)

**EXCEPTION TO RULE 0.8**: EnergoTSTestAgent can modify code files ONLY in `Cursor-Project/EnergoTS/tests/` directory.

- ✅ **ALLOWED**: Create/modify `.spec.ts` files in `EnergoTS/tests/`
- ❌ **FORBIDDEN**: Modify files outside `EnergoTS/tests/`
- ❌ **FORBIDDEN**: Modify Phoenix project files
- ❌ **FORBIDDEN**: Modify non-test files in EnergoTS

## Response Requirements

- State "**Agent:** EnergoTSTestAgent" at beginning
- Provide comprehensive answer with file paths and operations performed
- End with: "Agents involved: EnergoTSTestAgent" (and PhoenixExpert if consulted)

## Examples

**User**: "Create a new test for customer creation in EnergoTS"
**Action**: Use EnergoTSTestAgent.create_new_test() with customer specification

**User**: "Study the billing test in EnergoTS"
**Action**: Use EnergoTSTestAgent.study_test() with billing test path

**User**: "Copy the customer test and change Jira ID to REG-999"
**Action**: Use EnergoTSTestAgent.copy_and_convert_test() with conversion rules
