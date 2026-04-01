---
name: test-case-generator
description: Generates test cases from bug or task descriptions. Rule 35: run cross-dependency-finder FIRST, then generate with cross_dependency_data. Prefer Cursor-Project/test_cases/ (Objects/ and Flows/) per test_cases_structure.mdc. Use when the user asks to generate test cases or scenarios.
---

# Test Case Generator Skill

Ensures test case generation follows Rule 35 (cross-dependency-finder first) and saves output under **`Cursor-Project/test_cases/`** (**Objects/** and **Flows/** per `.cursor/rules/workspace/test_cases_structure.mdc`). Legacy `generated_test_cases/` is optional. READ-ONLY for Phoenix code except test-case markdown writes in allowed paths.

## When to Apply

- User asks to generate test cases for a bug, task, or feature.
- User wants test scenarios, test derivation from a description, or test cases for a Jira/Confluence item.
- User mentions "test cases", "test scenarios", "generate tests", or "derive tests".
- Command or request references test-case-generate or TestCaseGeneratorAgent.

## Mandatory: Rule 35 workflow

**Do not skip:** When the user requests test case creation, run **cross-dependency-finder** first, then **test-case-generator** with the finder's output.

1. **Step 1 – Cross-dependency-finder:** Same scope (bug/task/feature). Finder MUST follow Rule 35a when user gives Jira/bug/task: **Jira MCP + codebase + shallow Confluence** — **no** local merge/git. **Pattern:** `Cursor-Project/docs/CROSS_DEPENDENCY_WORK_PATTERN.md`. Finder may consult PhoenixExpert. Obtain structured output (including what_could_break and technical_details).
2. **Step 2 – Test-case-generator:** Call with `context['cross_dependency_data'] = <finder output>` (includes technical_details from merges when applicable), plus Confluence data and codebase_findings.

## Mandatory: Playwright instructions (`playwright_generation`)

**Before writing any test-case `.md`**, the generator MUST **read** (Read tool or equivalent) the user-provided Playwright/EnergoTS instruction pack so manual cases **align with how API specs will be written**:

- **Folder:** `Cursor-Project/config/playwright_generation/playwright instructions/`
- **Ignore:** `__MACOSX`, `._*` fragments, and paths outside that folder.

**Read order (mandatory):**

1. `project-description.md`
2. `general-rules.md`
3. `test-writing-rules.instructions.md`
4. `SKILL.md`

**If the user added more `*.md` files** in that same folder, read them **after** the four above, in **alphabetical** order, and apply their rules together with the template.

**Apply to test cases:** Steps and expected results should be **granular enough** to map to `test.step`, call out **HTTP method, endpoint, status, and response checks** where the instructions require (e.g. CheckResponse-style assertions), and avoid contradictions with **`general-rules.md`** (forbidden paths/patterns). This does **not** replace **`Cursor-Project/config/template/Test_case_template.md`** — use both.

**Related:** Downstream **energo-ts-test** agent reads the same folder when authoring `.spec.ts`; keep manual TCs consistent with that canon.

## Workflow (test-case-generator part)

### 1. Inputs (from parent)

- Prompt (bug or task description).
- prompt_type: 'bug' | 'task'.
- confluence_data (from MCP Confluence search).
- context: { codebase_findings, **cross_dependency_data** } (cross_dependency_data is mandatory when user requested test cases; technical_details from Jira + codebase per Rule 35a, not mandatory merge/MR lists).

### 2. Confluence + codebase

- Confluence: cloudId → search → collect title, content, pageId, spaceId.
- Codebase: codebase_search (and grep) for terms from prompt; collect findings.

### 3. Precondition data completeness (MANDATORY)

When writing **Preconditions** (both document-level "Test data" and per-TC), follow the **data completeness rule** from `Cursor-Project/config/template/Test_case_template.md`:

- List the **full data chain** from the top-level entity (e.g. customer) down to the entity under test (e.g. billing run, invoice, payment). Every entity that must exist for the scenario to be valid must be listed.
- **Specificity principle:** If the test works with any instance (e.g. any customer), write generically: "An active customer exists." If the test depends on a particular type, state, date, amount, or relationship, spell it out: "A private customer with customer manager X and status ACTIVE", "Product contract with entry-into-force date 2025-01-01 and termination date 2025-12-31", "Billing run of type STANDARD for period 2025-01-01 to 2025-01-31."
- **Dates and amounts:** When test outcome depends on timing (activation/deactivation, billing period boundaries, contract dates) or monetary values (thresholds, rounding, scale boundaries), those MUST appear explicitly in preconditions.
- **Data layers to consider:** Customer (type, status, manager), POD (identifier, type, dates), Product (term, price components, data delivery: scale/profile), Product contract (status, dates, linked POD/product), Service contract, Billing run (type, period, status), Invoice (status, amount), Payment (amount, linked invoice/package), Payment package (lock status).
- **Rule of thumb:** If removing a detail would make the test ambiguous or impossible to set up without guessing, that detail MUST be present.

### 4. Generate and save in hierarchical format (comprehensive coverage)

**Coverage (CRITICAL):** Generate **exhaustive** test cases – **not** a random or minimal set. Cover **every scenario that could occur**: all positive (happy path, valid inputs), all negative (invalid inputs, errors, rejections), edge cases, boundaries, and regression from cross_dependency_data (what_could_break). Aim for the **maximum number** of test cases that **fully cover** the task or bug.

**Root folder:** `Cursor-Project/test_cases/` with top-level **`Objects/`** and **`Flows/`** (siblings).

**Structure:**
- **`Objects/<Entity_name>/`** — entity-based scenarios (e.g. `Objects/Product_contract/Create.md`).
- **`Flows/<Flow_name>/`** — flow-based scenarios (e.g. `Flows/Contract_termination/Multi_version_termination_date.md`).
- **Leaf:** One `.md` file per logical group (e.g. `Create.md`, `Profile.md`). Each file: clear title, steps, expected result per case. Use underscores for multi-word names (e.g. `For_volumes.md`).

Map: entities → Objects; flows → Flows. Regression/impact cases (from what_could_break) under the most relevant path. Update the folder README tables when adding new entity/flow folders.

Full spec: `Cursor-Project/docs/TEST_CASES_HIERARCHY_FORMAT.md`.

### 5. Output content

- Confluence references, codebase analysis, mapping of test-case groups to paths (e.g. Object/customer/Create.md, Flows/Billing/Standard/For_volumes/Profile.md).

## READ-ONLY for Phoenix

- Do not modify Phoenix/production code. Only write test-case markdown under `Cursor-Project/test_cases/` (or legacy `generated_test_cases/` if explicitly requested).
- All output in English (Rule 0.7).

## Integration

- **Rule 0.3:** no Python IntegrationService here.
- PhoenixExpert if needed (reuse context from cross-dependency-finder when provided).
- Markdown reports after generation if Rule 0.6 applies (no Python ReportingService).
- End with: "Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Command and references

- Command: `.cursor/commands/test-case-generate.md`
- Subagent: `.cursor/agents/test-case-generator.md`
- Hierarchy format: `Cursor-Project/docs/TEST_CASES_HIERARCHY_FORMAT.md`
- Agent doc: `Cursor-Project/docs/TEST_CASE_GENERATOR_AGENT.md`
- Rule 35: `.cursor/rules/workflows/workflow_rules.mdc`
