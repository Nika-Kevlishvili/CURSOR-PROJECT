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

0. **Step 0 – Environment (Rule PHOENIX-SWITCH.0):** Before any Phoenix code read for this workflow, resolve target env via **`environment-resolver`** (`dev` … `experiments`). Align every `Cursor-Project/Phoenix/*` repo using `.cursor/commands/switch-phoenix-branches.ps1 -Environment <env>` (`-ConfirmProd` for `prod` only after explicit user acknowledgement). Pass resolved env + alignment exit code to cross-dependency-finder / generator prompts so subagents reuse alignment when the parent already ran the script (Rule PHOENIX-SWITCH.0 §7a). Details: `.cursor/rules/integrations/phoenix_branch_switching.mdc`.
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

### 2b. Process diagrams (expected flow alignment)

Use **`prompt_type`** (`'bug'` vs `'task'`) from inputs — behavior differs.

**Bugs (`prompt_type: 'bug'`):**

- When the ticket has **no** diagram attachment and no diagram URL in scope yet, consult **`Cursor-Project/config/Diagrams/`** (`Bundle 4`–`6`): pick matching `.svg` files, align steps/expected results, cite paths under **`## References`** (same as prior bug-validation alignment).
- When Jira/Confluence provides downloadable diagram assets, save read-only under **`Cursor-Project/config/confluence/diagrams/<pageId-or-issueKey>/`** and cite in **References**.
- **Authority:** **code + Confluence** override contradictory diagrams; note conflicts in **References**.

**Tasks / non-bugs (`prompt_type: 'task'`) — user-supplied scope wins:**

1. **Diagram already in scope** (task/Jira description, linked Confluence page text/media from that ticket, diagram URLs or attachments in **chat**, explicit diagram URLs the user or ticket provided): treat **written description + that diagram** as **primary**. Do **not** prioritize or substitute **`Cursor-Project/config/Diagrams/`** over what the task already contains. Local library is **out of scope** unless the user asks to compare or the description is silent and you are in case (2).
2. **No diagram in task description or linked pages:** search **`Cursor-Project/config/Diagrams/`** for plausible `.svg` matches. **Mandatory gate:** if any candidate fits, **do not** write TC files based on that diagram until the user confirms — ask explicitly whether to use it, naming **each candidate by full workspace path** (and one-line why it might match). If the user declines or multiple candidates remain ambiguous, omit local diagram from TC scope until clarified.
3. **Authority:** same as bugs — **code + Confluence** override contradictory diagrams; document discrepancies in **References**.

### 3. Precondition reuse — DRY (MANDATORY)

Precondition duplication is a **forbidden pattern**. Before writing any TC's `Preconditions:` block, follow this workflow:

1. **Build the full shared chain once** in `## Test data (preconditions)` — every entity, every endpoint, every key parameter.
2. **Cluster TCs by shared slice** — identify which steps every TC needs vs. which steps only a subset needs.
3. **Per TC: reference + deltas only.** Each TC's `Preconditions:` MUST start with `Apply Test data steps 1–N.` and then list **only the deltas** for that TC (different status, different amount, skipped entity, additional entity).
4. **Self-check before writing files:** scan your draft for duplicated `POST /` or "Create … via" lines across multiple TCs. If the same line appears in ≥2 TCs, move it to `Test data` and replace each occurrence with a step reference.

Do NOT re-state entity creation steps that already appear in `Test data`. Violation = duplicated preconditions = incomplete self-check.

**Reference:** `Cursor-Project/config/template/Test_case_template.md` § "Reuse model — DRY preconditions".

### 3b. Case-specific preconditions (MANDATORY — NEVER OMIT)

For **every** TC, make the scenario-specific setup explicit in `Preconditions:`. Use this shape:

1. `Apply Test data steps X–Y.`
2. `Delta: ...` (what differs in this TC only).

Allowed deltas: changed status/state, skipped entity, extra entity, different amount/date/value, alternative user role/permission.

Rules:
- If no special setup exists, still write: `Delta: none (shared setup unchanged).`
- Negative TCs MUST include a concrete failing delta (not generic text).
- Two TCs must not have identical `Preconditions:` blocks unless they are true duplicates (duplicates are forbidden; merge/remove instead).
- Do not leave TC-specific setup only in Description/Steps; it must appear in `Preconditions:`.

### 3a. Precondition data completeness (MANDATORY — creation-step rule)

When writing **Preconditions** (both document-level "Test data" and per-TC), follow the **mandatory creation-step precondition rule** from `Cursor-Project/config/template/Test_case_template.md`:

- **ALWAYS describe HOW to create every entity** in the data chain — never just write "entity X exists."
- Every precondition step MUST include: the **API endpoint** (or UI action), **key parameters** (type, status, amount, dates, linked entities), and **references to earlier steps** (e.g. "customer ID from step 1").
- **FORBIDDEN:** "An active customer exists.", "A product contract is linked to the customer.", "A billing run has been executed." — these are too vague.
- **REQUIRED:** "Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE).", "Create a product contract via `POST /product-contract` linking customer from step 1, POD from step 2, product from step 3 (status: ACTIVE, entry-into-force date: 2025-01-01)."
- **Data layers to always include when relevant:** Customer, POD, Product, Terms, Price component, Product contract, Service contract, Energy data / billing profile, Billing run, Invoice, Payment, Payment package, Deposit. Each as its own numbered step with endpoint and parameters.
- **Rule of thumb:** Every entity that must exist for the test to run MUST have its own creation step. If a tester cannot set up the test without guessing how to create an entity, the precondition is incomplete.

### 3.1 Playwright Automation Compatibility (CRITICAL)

Test cases MUST be written so that Playwright automation can implement them **without assuming any data exists**:

- **NEVER** write preconditions that assume data already exists in the test environment
- **ALWAYS** include creation steps for ALL entities in the data chain
- **Follow dependency order:** Terms → Price components → Products → Customers → PODs → Contracts → Billing runs → Invoices → Payments
- Each entity creation step MUST specify enough detail that Playwright can use `GeneratePayload` methods to create it
- **Identify shared vs test-specific preconditions:** Mark which preconditions are common to many tests (→ helper functions) vs which are unique to specific tests (→ inline creation)

**Example of Playwright-compatible preconditions:**

```markdown
**Preconditions (shared across many tests):**
1. Create terms via `POST /terms` (type: PERIOD, value: 100, periodType: DAY_DAYS).
2. Create electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId: from envVariables).
3. Create product via `POST /products` with:
   - termId: from step 1
   - priceComponentIds: [id from step 2]
   - status: ACTIVE
   - availableForSale: true
   - isIndividual: false
   - globalSalesChannel: true (ALL channels)
   - globalSalesArea: true (ALL areas)
   - globalSegment: true (ALL segments)
   - contractTypes: [SUPPLY_ONLY]
   - paymentGuarantees: [NO]
```

**Downstream Playwright code MUST:**
- Create **helper functions** at the top of the spec file for shared preconditions (e.g., `sharedTerm()`, `sharedProduct()`)
- Call helper functions within each test via `test.step('Precondition: Create shared entities', ...)`
- For test-specific data (e.g., INACTIVE product), use shared helpers for dependencies and create the specific entity inline
- **DO NOT use `test.beforeAll()`** — fixtures may not be available there
- Store created entities in `Responses` arrays (not describe-level `let` variables)
- Never query existing data instead of creating new data

### 4. TC quality — apply rubric before saving (MANDATORY)

Before writing the final `.md` files, score each TC against the quality rubric in `Cursor-Project/docs/test_case_quality_rubric.md`. Score axes (each 0–2): Intent uniqueness, Observable expected result, Endpoint specificity, Delta clarity, Risk coverage from cross_dep, Readability. Minimum passing score: **8/12**.

- TCs scoring <8 MUST be rewritten (max 2 passes).
- After rewriting, re-score; any TC still below 8 after 2 passes is flagged to the user with the specific axis that failed — do not silently drop or keep weak TCs.
- After generation completes, invoke the **test-case-quality-validator** subagent (`.cursor/agents/test-case-quality-validator.md`) to do a second-pass verification. If it returns rewrite suggestions, apply them (max 2 rounds) before final file write.

### 5. Generate and save as TWO files — Backend and Frontend (comprehensive coverage)

**Coverage (CRITICAL):** Generate **exhaustive** test cases – **not** a random or minimal set. Cover **every scenario that could occur**: all positive (happy path, valid inputs), all negative (invalid inputs, errors, rejections), edge cases, boundaries, and regression from cross_dependency_data (what_could_break). Aim for the **maximum number** of test cases that **fully cover** the task or bug.

**Negative-scope rule (CRITICAL):**
- Default negative cases must validate **business/domain rejections** for the target feature.
- Do NOT include irrelevant technical negatives (unauthorized/unauthenticated auth failures, wrong URL/path, generic connectivity failures) unless the user explicitly requested auth/routing/infrastructure testing.
- For each negative TC, define exact expected rejection semantics: intended status, error code/message fragment, and the field/constraint that failed.
- `HTTP 400` is valid when business validation is expected, but `HTTP 400` alone is never a passing expectation.
- `HTTP 403` should be expected only in permission/authorization test scenarios.
- If code/spec defines a specific error contract for a scenario, use that exact expected error in the TC (status + error semantics), not a generic 4xx.

**Root folder:** `Cursor-Project/test_cases/`

**Structure:** Two sub-folders — `Backend/` and `Frontend/`.

- **Backend file:** `Cursor-Project/test_cases/Backend/<Topic_name>.md` — contains ONLY **Backend Test Cases** (`TC-BE-N`).
- **Frontend file:** `Cursor-Project/test_cases/Frontend/<Topic_name>.md` — contains ONLY **Frontend Test Cases** (`TC-FE-N`).
- Both files share the same `<Topic_name>` (e.g. `Invoice_cancellation.md`).
- Each file must have at least one Positive and one Negative TC. If a layer is not applicable, create the file with an N/A note.
- Use underscores for multi-word topic names.

Regression/impact cases (from what_could_break) go in whichever file (Backend or Frontend) is most relevant. Update `test_cases/README.md`, `test_cases/Backend/README.md`, and `test_cases/Frontend/README.md` when adding new files.

### 6. Output content

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

## References

- Subagent: `.cursor/agents/test-case-generator.md`
- Hierarchy format: `Cursor-Project/docs/TEST_CASES_HIERARCHY_FORMAT.md`
- Agent doc: `Cursor-Project/docs/TEST_CASE_GENERATOR_AGENT.md`
- Rule 35: `.cursor/rules/workflows/workflow_rules.mdc`
