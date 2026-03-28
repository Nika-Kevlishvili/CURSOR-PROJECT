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

1. **Step 1 – Cross-dependency-finder:** Same scope (bug/task/feature). Finder MUST follow Rule 35a when user gives Jira/bug/task: merge lookup → conditional sync if merge exists → technical_details in output. Finder may consult PhoenixExpert. Obtain structured output (including what_could_break and technical_details).
2. **Step 2 – Test-case-generator:** Call with `context['cross_dependency_data'] = <finder output>` (includes technical_details from merges when applicable), plus Confluence data and codebase_findings.

## Workflow (test-case-generator part)

### 1. Inputs (from parent)

- Prompt (bug or task description).
- prompt_type: 'bug' | 'task'.
- confluence_data (from MCP Confluence search).
- context: { codebase_findings, **cross_dependency_data** } (cross_dependency_data is mandatory when user requested test cases; may include technical_details from merge/MR for the Jira key per Rule 35a).

### 2. Confluence + codebase

- Confluence: cloudId → search → collect title, content, pageId, spaceId.
- Codebase: codebase_search (and grep) for terms from prompt; collect findings.

### 3. Generate and save in hierarchical format (comprehensive coverage)

**Coverage (CRITICAL):** Generate **exhaustive** test cases – **not** a random or minimal set. Cover **every scenario that could occur**: all positive (happy path, valid inputs), all negative (invalid inputs, errors, rejections), edge cases, boundaries, and regression from cross_dependency_data (what_could_break). Aim for the **maximum number** of test cases that **fully cover** the task or bug.

**Root folder:** `Cursor-Project/test_cases/` with top-level **`Objects/`** and **`Flows/`** (siblings).

**Structure:**
- **`Objects/<Entity_name>/`** — entity-based scenarios (e.g. `Objects/Product_contract/Create.md`).
- **`Flows/<Flow_name>/`** — flow-based scenarios (e.g. `Flows/Contract_termination/Multi_version_termination_date.md`).
- **Leaf:** One `.md` file per logical group (e.g. `Create.md`, `Profile.md`). Each file: clear title, steps, expected result per case. Use underscores for multi-word names (e.g. `For_volumes.md`).

Map: entities → Objects; flows → Flows. Regression/impact cases (from what_could_break) under the most relevant path. Update the folder README tables when adding new entity/flow folders.

Full spec: `Cursor-Project/docs/TEST_CASES_HIERARCHY_FORMAT.md`.

### 4. Output content

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
