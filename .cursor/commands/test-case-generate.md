# Generate Test Cases

Generate test cases from a bug or task description. **Rule 35:** When the user requests test case creation, run **cross-dependency-finder** FIRST, then **test-case-generator** with the finder's output. Route to **test-case-generator** subagent / TestCaseGeneratorAgent.

## When to Use

Use this command when the user asks to:
- Generate test cases for a bug
- Create test scenarios from a task or feature
- Derive test cases from a description
- Write test cases for a Jira ticket or Confluence story

## Mandatory Workflow (Rule 35)

### Step 1: Cross-Dependency Finder (MANDATORY – do not skip)

1. Run **cross-dependency-finder** for the same scope (bug/task/feature). It MUST follow **Rule 35a**: (a) look up **merge history** for the Jira/bug/task key (local git + GitLab); (b) if a merge exists for that Jira, run a **targeted sync** for that branch only; (c) include merge-derived **technical_details** in the output.
2. Cross-dependency-finder may consult **PhoenixExpert** to study the project.
3. Obtain the structured output (including **what_could_break** and **technical_details** from merges).
4. Pass this output to the next step as `context['cross_dependency_data']`.

### Step 2: Test Case Generator

1. **Rule 0.3** — No Python `IntegrationService` in this workspace; follow MCP/Jira when needed (see `.cursor/rules/main/core_rules.mdc`).
2. **PhoenixExpert** – Consult if needed (Rule 8); reuse context from cross-dependency-finder if already provided.
3. **Confluence (MCP)** – Search and collect relevant docs (title, content, pageId, spaceId).
4. **Codebase** – Search for terms from the prompt; collect codebase_findings.
5. **Generate** – Call TestCaseGeneratorAgent with `prompt`, `prompt_type` ('bug'|'task'), `confluence_data`, and `context={'codebase_findings': ..., 'cross_dependency_data': <from Step 1>}`.
6. **Save** – Write test cases in the **hierarchical format** to `Cursor-Project/generated_test_cases/` (see below).
7. **Report** – Generate reports (Rule 0.6).

## Output – Structure and content (MANDATORY)

**Comprehensive coverage (CRITICAL):** Test case generation MUST produce **exhaustive** coverage of the task or bug – **not** a random or minimal set. Generate the **maximum number of test cases** that **fully cover** the scope:
- **All positive scenarios:** happy path(s), valid inputs, expected success, main flows.
- **All negative scenarios:** invalid inputs, missing/invalid IDs, wrong state, expected errors and rejections.
- **Edge cases and boundaries:** empty lists, zero amounts, max values, dates at boundaries, already-processed state, duplicate actions.
- **Regression/impact:** every scenario from `cross_dependency_data` (what_could_break, integration points) that could be affected.
Do **not** limit to 2–3 test cases; aim for **all plausible scenarios** so the task or bug is covered entirely.

**Content template:** Every test case document MUST follow the **Test Case Template**: **`Cursor-Project/config/template/Test_case_template.md`**. Use that template’s structure (header, Summary, Scope, Test data, TC-1/TC-2/… with Objective, Preconditions, Steps, Expected result, Actual result if bug, References). **Include both positive and negative scenarios:** at least one **Positive** (valid input, happy path, expected success) and at least one **Negative** (invalid input, error condition, expected rejection); label each TC as **(Positive)** or **(Negative)**. Write in **maximally detailed**, **human-readable** language (full sentences where helpful, no unexplained jargon, plain English). Same rules apply whether saving under `generated_test_cases/` or `test_cases/Flows/` / `test_cases/Objects/`.

**Folder (generic flow):** Save under:

**Root:** `Cursor-Project/generated_test_cases/`

**Structure:**
- **Object/** – Entities and actions (e.g. customer → Create, Edit, …; contract → …).
- **Flows/** – Business flows and variants (e.g. Billing → Standard → For_volumes → scale, Profile; interim; …).
- **Leaf:** One `.md` file per logical group (e.g. `Create.md`, `Profile.md`). Each file follows the template: clear title, Summary, Scope, Test data, then TC-1, TC-2, … with Objective, Preconditions, Steps, Expected result (and Actual result if bug).

**Folder (HandsOff):** When generating for HandsOff, save under **`Cursor-Project/test_cases/Flows/<Flow_name>/`** or **`Cursor-Project/test_cases/Objects/<Entity_name>/`** (see `.cursor/rules/workspace/test_cases_structure.mdc`). Content still MUST follow **`Cursor-Project/config/template/Test_case_template.md`**.

Full spec (hierarchy): `Cursor-Project/docs/TEST_CASES_HIERARCHY_FORMAT.md`. Content spec: `Cursor-Project/config/template/Test_case_template.md`.

## Constraints

- **READ-ONLY** for Phoenix code; do not modify production code.
- All output in **English** (Rule 0.7).
- Never skip the cross-dependency-finder step when the user requested test cases.

## Response Requirements

- State "**Agent:** TestCaseGeneratorAgent" at beginning when applicable.
- Return the generated test cases and the paths where they were saved (e.g. Object/customer/Create.md, Flows/Billing/Standard/…).
- End with: "Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Generate Reports (Rule 0.6)

- Save to `Cursor-Project/reports/YYYY-MM-DD/TestCaseGeneratorAgent_{HHMM}.md`
- Save summary to `Cursor-Project/reports/YYYY-MM-DD/Summary_{HHMM}.md`

## Example Triggers

- "Generate test cases for this bug"
- "Create test scenarios for REG-1234"
- "Write test cases for the billing profile feature"
- "Derive test cases from this task description"
