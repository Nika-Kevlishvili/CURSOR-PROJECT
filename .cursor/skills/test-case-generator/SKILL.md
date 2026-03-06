---
name: test-case-generator
description: Generates test cases from bug or task descriptions. Rule 35: run cross-dependency-finder FIRST, then generate with cross_dependency_data. Saves to hierarchical format (Object/Flows) in generated_test_cases/ for maximum human readability. Use when the user asks to generate test cases, create test scenarios from a bug/task, or derive tests.
---

# Test Case Generator Skill

Ensures test case generation follows Rule 35 (cross-dependency-finder first) and saves output in the mandatory hierarchical, human-readable format under `Cursor-Project/generated_test_cases/`. READ-ONLY for Phoenix code; only generated test-case files are written.

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

### 3. Generate and save in hierarchical format

**Root folder:** `Cursor-Project/generated_test_cases/`

**Structure:**
- **Object/** – Domain entities and actions (e.g. customer → Create, Edit, Delete, View; contract → …).
- **Flows/** – Business flows and variants (e.g. Billing → Standard → For_volumes → scale, Profile; interim; …).
- **Leaf:** One `.md` file per logical group (e.g. `Create.md`, `Profile.md`). Each file: clear title, steps, expected result per case. Use underscores for multi-word names (e.g. `For_volumes.md`).

Map: entities/actions → Object; flows/variants → Flows. Regression/impact cases (from what_could_break) under the most relevant path.

Full spec: `Cursor-Project/docs/TEST_CASES_HIERARCHY_FORMAT.md`.

### 4. Output content

- Confluence references, codebase analysis, mapping of test-case groups to paths (e.g. Object/customer/Create.md, Flows/Billing/Standard/For_volumes/Profile.md).

## READ-ONLY for Phoenix

- Do not modify Phoenix/production code. Only write generated test-case files under `generated_test_cases/`.
- All output in English (Rule 0.7).

## Integration

- IntegrationService before task.
- PhoenixExpert if needed (reuse context from cross-dependency-finder when provided).
- ReportingService after generation (Rule 0.6).
- End with: "Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Command and references

- Command: `.cursor/commands/test-case-generate.md`
- Subagent: `.cursor/agents/test-case-generator.md`
- Hierarchy format: `Cursor-Project/docs/TEST_CASES_HIERARCHY_FORMAT.md`
- Agent doc: `Cursor-Project/docs/TEST_CASE_GENERATOR_AGENT.md`
- Rule 35: `.cursor/rules/workflow_rules.mdc`
