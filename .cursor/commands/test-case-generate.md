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

1. Run **cross-dependency-finder** for the same scope (bug/task/feature). It MUST follow **Rule 35a**: **Jira MCP + codebase + shallow Confluence** — **no** local merge/git or sync solely for cross-dep; **technical_details** from Jira + codebase (MR/merge only if user explicitly asked GitLab).
2. Cross-dependency-finder may consult **PhoenixExpert** to study the project.
3. Obtain the structured output (including **what_could_break** and **technical_details**).
4. Pass this output to the next step as `context['cross_dependency_data']`.

### Step 2: Test Case Generator

0. **Playwright instructions (MANDATORY):** Before generating test-case content, **read** **`Cursor-Project/config/playwright_generation/playwright instructions/`** in order: `project-description.md` → `general-rules.md` → `test-writing-rules.instructions.md` → `SKILL.md`; then any other `*.md` in that folder alphabetically. **Ignore** `__MACOSX` / `._*`. Generated cases MUST follow that pack so steps/expectations **bridge** to EnergoTS Playwright specs (`test.step`, HTTP/status/body checks per instructions, no contradictions with `general-rules.md`). Still use **`Cursor-Project/config/template/Test_case_template.md`** for document shape.
1. **Rule 0.3** — No Python `IntegrationService` in this workspace; follow MCP/Jira when needed (see `.cursor/rules/main/core_rules.mdc`).
2. **PhoenixExpert** – Consult if needed (Rule 8); reuse context from cross-dependency-finder if already provided.
3. **Confluence (MCP)** – Search and collect relevant docs (title, content, pageId, spaceId).
4. **Codebase** – Search for terms from the prompt; collect codebase_findings.
5. **Generate** – Call TestCaseGeneratorAgent with `prompt`, `prompt_type` ('bug'|'task'), `confluence_data`, and `context={'codebase_findings': ..., 'cross_dependency_data': <from Step 1>}` (generator MUST have loaded the Playwright instruction pack in step 0).
6. **Save** – Write test cases as a **single file** to `Cursor-Project/test_cases/<Topic_name>.md` with Backend/Frontend split (see below).
7. **Report** – Generate reports (Rule 0.6).

## Output – Structure and content (MANDATORY)

**Comprehensive coverage (CRITICAL):** Test case generation MUST produce **exhaustive** coverage of the task or bug – **not** a random or minimal set. Generate the **maximum number of test cases** that **fully cover** the scope:
- **All positive scenarios:** happy path(s), valid inputs, expected success, main flows.
- **All negative scenarios:** invalid inputs, missing/invalid IDs, wrong state, expected errors and rejections.
- **Edge cases and boundaries:** empty lists, zero amounts, max values, dates at boundaries, already-processed state, duplicate actions.
- **Regression/impact:** every scenario from `cross_dependency_data` (what_could_break, integration points) that could be affected.
Do **not** limit to 2–3 test cases; aim for **all plausible scenarios** so the task or bug is covered entirely.

**Content template:** Every test case document MUST follow the **Test Case Template**: **`Cursor-Project/config/template/Test_case_template.md`**. Use that template’s structure (header, Summary, Scope, Test data, TC-1/TC-2/… with Test title in the heading, Description, Preconditions, Test steps, Expected test case results, Actual result if bug, References). **Include both positive and negative scenarios:** at least one **Positive** (valid input, happy path, expected success) and at least one **Negative** (invalid input, error condition, expected rejection); label each TC as **(Positive)** or **(Negative)**. Write in **maximally detailed**, **human-readable** language (full sentences where helpful, no unexplained jargon, plain English). Each file has **Backend Test Cases** (`TC-BE-N`) and **Frontend Test Cases** (`TC-FE-N`) sections. If a section is not applicable, keep the heading with a note.

**Folder:** Save as a single `.md` file per topic under:

**Root:** `Cursor-Project/test_cases/`

**Structure:**
- **Path:** `Cursor-Project/test_cases/<Topic_name>.md` (e.g. `Invoice_cancellation.md`, `Product_contract_create.md`).
- **Internal split:** Each file has **Backend Test Cases** (`TC-BE-N`) and **Frontend Test Cases** (`TC-FE-N`) sections.
- Use underscores for multi-word topic names.

This applies to both generic flow and HandsOff — all test cases go to `Cursor-Project/test_cases/<Topic_name>.md`. Content spec: `Cursor-Project/config/template/Test_case_template.md`.

## Constraints

- **READ-ONLY** for Phoenix code; do not modify production code.
- All output in **English** (Rule 0.7).
- Never skip the cross-dependency-finder step when the user requested test cases.

## Response Requirements

- State "**Agent:** TestCaseGeneratorAgent" at beginning when applicable.
- Return the generated test cases and the path where they were saved (e.g. `test_cases/Invoice_cancellation.md`).
- End with: "Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Generate Reports (Rule 0.6)

- Save to `Cursor-Project/reports/YYYY-MM-DD/TestCaseGeneratorAgent_{HHMM}.md`
- Save summary to `Cursor-Project/reports/YYYY-MM-DD/Summary_{HHMM}.md`

## Example Triggers

- "Generate test cases for this bug"
- "Create test scenarios for REG-1234"
- "Write test cases for the billing profile feature"
- "Derive test cases from this task description"
