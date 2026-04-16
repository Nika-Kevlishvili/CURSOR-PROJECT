---
name: test-case-generator
description: Generates test cases from bug or task descriptions. Rule 35: run cross-dependency-finder FIRST, then generate with cross_dependency_data. Save as TWO files — Backend/ and Frontend/ — under Cursor-Project/test_cases/ per test_cases_structure.mdc. Use when the user asks to generate test cases or scenarios.
---

# Test Case Generator Skill

Ensures test case generation follows Rule 35 (cross-dependency-finder first) and saves output as **two separate `.md` files** — one in **`Cursor-Project/test_cases/Backend/<Topic_name>.md`** (TC-BE-N only) and one in **`Cursor-Project/test_cases/Frontend/<Topic_name>.md`** (TC-FE-N only) — per `.cursor/rules/workspace/test_cases_structure.mdc`. Legacy `generated_test_cases/` is optional. READ-ONLY for Phoenix code except test-case markdown writes in allowed paths.

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

### 3. Precondition data completeness (MANDATORY — creation-step rule)

When writing **Preconditions** (both document-level "Test data" and per-TC), follow the **mandatory creation-step precondition rule** from `Cursor-Project/config/template/Test_case_template.md`:

- **ALWAYS describe HOW to create every entity** in the data chain — never just write "entity X exists."
- Every precondition step MUST include: the **API endpoint** (or UI action), **key parameters** (type, status, amount, dates, linked entities), and **references to earlier steps** (e.g. "customer ID from step 1").
- **FORBIDDEN:** "An active customer exists.", "A product contract is linked to the customer.", "A billing run has been executed." — these are too vague.
- **REQUIRED:** "Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE).", "Create a product contract via `POST /product-contract` linking customer from step 1, POD from step 2, product from step 3 (status: ACTIVE, entry-into-force date: 2025-01-01)."
- **Data layers to always include when relevant:** Customer, POD, Product, Terms, Price component, Product contract, Service contract, Energy data / billing profile, Billing run, Invoice, Payment, Payment package, Deposit. Each as its own numbered step with endpoint and parameters.
- **Rule of thumb:** Every entity that must exist for the test to run MUST have its own creation step. If a tester cannot set up the test without guessing how to create an entity, the precondition is incomplete.

### 4. Generate and save as TWO files — Backend and Frontend (comprehensive coverage)

**Coverage (CRITICAL):** Generate **exhaustive** test cases – **not** a random or minimal set. Cover **every scenario that could occur**: all positive (happy path, valid inputs), all negative (invalid inputs, errors, rejections), edge cases, boundaries, and regression from cross_dependency_data (what_could_break). Aim for the **maximum number** of test cases that **fully cover** the task or bug.

**Root folder:** `Cursor-Project/test_cases/`

**Structure:** Two sub-folders — `Backend/` and `Frontend/`.

- **Backend file:** `Cursor-Project/test_cases/Backend/<Topic_name>.md` — contains ONLY **Backend Test Cases** (`TC-BE-N`).
- **Frontend file:** `Cursor-Project/test_cases/Frontend/<Topic_name>.md` — contains ONLY **Frontend Test Cases** (`TC-FE-N`).
- Both files share the same `<Topic_name>` (e.g. `Invoice_cancellation.md`).
- Each file must have at least one Positive and one Negative TC. If a layer is not applicable, create the file with an N/A note.
- Use underscores for multi-word topic names.

Regression/impact cases (from what_could_break) go in whichever file (Backend or Frontend) is most relevant. Update `test_cases/README.md`, `test_cases/Backend/README.md`, and `test_cases/Frontend/README.md` when adding new files.

### 5. Output content

- Confluence references, codebase analysis, file paths where test cases were saved (e.g. `test_cases/Backend/Invoice_cancellation.md` and `test_cases/Frontend/Invoice_cancellation.md`).

## READ-ONLY for Phoenix

- Do not modify Phoenix/production code. Only write test-case markdown under `Cursor-Project/test_cases/` (or legacy `generated_test_cases/` if explicitly requested).
- All output in English (Rule 0.7).

## Integration

- **Rule 0.3:** no Python IntegrationService here.
- PhoenixExpert if needed (reuse context from cross-dependency-finder when provided).
- Optional markdown under `reports/` only if the user requests a saved run log (Rule 0.6 default; no Python ReportingService).
- End with: "Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Confidence Score (Rule CONF.1) [MANDATORY]

The final output MUST include a **Confidence Score** (0–100%). Format: `**Confidence: XX%** Reason: <explanation>`. Scoring: 90–100% = verified data + clear requirements; 70–89% = reasonable inference with assumptions (list them); 50–69% = significant info gaps; <50% = best-effort draft, flag prominently. When multiple test cases have varying confidence, include per-item scores alongside the overall score. Be honest — do not inflate.

## Command and references

- Command: `.cursor/commands/test-case-generate.md`
- Subagent: `.cursor/agents/test-case-generator.md`
- Hierarchy format: `Cursor-Project/docs/TEST_CASES_HIERARCHY_FORMAT.md`
- Agent doc: `Cursor-Project/docs/TEST_CASE_GENERATOR_AGENT.md`
- Rule 35: `.cursor/rules/workflows/workflow_rules.mdc`
