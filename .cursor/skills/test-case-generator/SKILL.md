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

## MANDATORY: Ask about Frontend Test Cases (Rule TC-FRONTEND-ASK.0)

**Before starting test case generation**, the agent MUST ask the user whether they want Frontend test cases generated.

**Question (use AskQuestion tool or equivalent):**

```
Do you want to generate Frontend (UI) test cases?

Options:
- Yes, generate both Backend and Frontend test cases
- No, generate only Backend (API) test cases
```

**Behavior based on answer:**

| User answer | Action |
|-------------|--------|
| **Yes** | Generate both `test_cases/Backend/<Topic>.md` (TC-BE-N) and `test_cases/Frontend/<Topic>.md` (TC-FE-N) |
| **No** | Generate ONLY `test_cases/Backend/<Topic>.md` (TC-BE-N). Do NOT create the Frontend file at all. |

**NEVER auto-generate Frontend test cases without asking first.**

**Exception:** If the user explicitly mentions "frontend", "UI tests", or "both backend and frontend" in their initial request, skip the question and include Frontend. If they say "only backend", "API tests only", or similar, skip the question and exclude Frontend.

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

### 3. Precondition reuse — DRY vs self-contained (MANDATORY)

**Rule TC-STANDALONE-PRE.0** (`.cursor/rules/workspace/test_cases_structure.mdc`) takes precedence for generated files:

- Each TC's **`Preconditions:`** MUST contain the **full numbered setup** for that scenario (tester does not open another TC).
- **FORBIDDEN:** `Apply Test data steps 1–N` without repeating steps in the same TC; `Delta from TC-BE-X`; multiple scenarios (run A/B) in one TC.
- **Optional** `## Test data` / appendix at file top = reference tables only; **not** a substitute for per-TC preconditions.

Duplicating `POST /customer`, `POST /pod`, etc. across TCs is **required** when each TC must stand alone (acceptable trade-off per project rule).

### 3b. Case-specific preconditions (MANDATORY — NEVER OMIT)

For **every** TC, **all** scenario-specific values (amounts, `billingType`, IAP `valueType`, contract type) MUST appear inside that TC's `Preconditions:` numbered list — not only in Description.

Rules:
- Negative TCs MUST state the exact parameter that yields rejection (e.g. `amountExcludingVat: 4.16` → total incl. VAT 4.99).
- Two TCs with identical `Preconditions:` are duplicates — merge or differentiate.
- Do not leave setup only in Description/Steps.

### 3c. No skip / log / audit observability TCs (MANDATORY — Rule TC-NOSKIP-OBS.0)

**FORBIDDEN:** TCs or expected results that primarily verify skip **logs**, interim row status without invoice, or "defect if observed" audit.

**REQUIRED for negatives:** No invoice in `POST /invoice/listing` (or cited API); no new liability; HTTP/status fields — not log file grep.

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

### 4. TC quality — apply rubric before saving (MANDATORY — STRICT 0-100 SCORING)

Before writing the final `.md` files, score each TC against the quality rubric in `Cursor-Project/docs/test_case_quality_rubric.md`. Score **10 axes** (0–100 total). **Minimum passing score: 80/100**.

- TCs scoring **<80** MUST be rewritten (max **3 iterations**).
- After rewriting, re-score; any TC still below 80 after 3 iterations is **escalated to the user** with all failing axes and reasons — do not silently drop or keep weak TCs.
- After generation completes, invoke the **test-case-quality-validator** subagent (`.cursor/agents/test-case-quality-validator.md`) to do a second-pass verification. The validator operates in **STRICT MODE** — harsh, uncompromising, no leniency. If it returns rewrite suggestions, apply them (max 3 rounds) before final file write.
- After 3 failed iterations: **BLOCK WORKFLOW** and escalate to user.

### 5. Generate and save test case files (Backend always, Frontend only if user confirmed)

**Coverage (CRITICAL):** Generate **exhaustive** test cases – **not** a random or minimal set. Cover **every scenario that could occur**: all positive (happy path, valid inputs), all negative (invalid inputs, errors, rejections), edge cases, boundaries, and regression from cross_dependency_data (what_could_break). Aim for the **maximum number** of test cases that **fully cover** the task or bug.

**Negative-scope rule (CRITICAL):**
- Default negative cases must validate **business/domain rejections** for the target feature.
- Do NOT include irrelevant technical negatives (unauthorized/unauthenticated auth failures, wrong URL/path, generic connectivity failures) unless the user explicitly requested auth/routing/infrastructure testing.
- For each negative TC, define exact expected rejection semantics: intended status, error code/message fragment, and the field/constraint that failed.
- `HTTP 400` is valid when business validation is expected, but `HTTP 400` alone is never a passing expectation.
- `HTTP 403` should be expected only in permission/authorization test scenarios.
- If code/spec defines a specific error contract for a scenario, use that exact expected error in the TC (status + error semantics), not a generic 4xx.

**Root folder:** `Cursor-Project/test_cases/`

**Structure (based on user choice from Rule TC-FRONTEND-ASK.0):**

| User choice | Files created |
|-------------|---------------|
| **Yes (Backend + Frontend)** | `Backend/<Topic>.md` + `Frontend/<Topic>.md` |
| **No (Backend only)** | `Backend/<Topic>.md` ONLY — do NOT create Frontend file |

- **Backend file:** `Cursor-Project/test_cases/Backend/<Topic_name>.md` — contains ONLY **Backend Test Cases** (`TC-BE-N`). **ALWAYS created.**
- **Frontend file:** `Cursor-Project/test_cases/Frontend/<Topic_name>.md` — contains ONLY **Frontend Test Cases** (`TC-FE-N`). **Created ONLY if user confirmed "Yes" to Frontend question.**
- Each file must have at least one Positive and one Negative TC.
- Use underscores for multi-word topic names.

Regression/impact cases (from what_could_break) go in the Backend file (or Frontend if user confirmed and the risk is UI-related). Update `test_cases/README.md` and `test_cases/Backend/README.md` when adding new files. Update `test_cases/Frontend/README.md` only if Frontend file was created.

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
