# Cross-Dependency Finder Agent – Design and Integration

## Overview

The **Cross-Dependency Finder** agent discovers cross-dependencies (modules, services, APIs, databases, shared libs) for a given scope. Its output is shared with **TestCaseGeneratorAgent** so that generated test cases cover integration points, upstream/downstream behaviour, and data entities, making tests more robust.

## Goals

- **Find cross-dependencies** for a scope (feature, module, bug, or task).
- **Share structured data** with the Test Case Generator.
- **Improve test coverage** by adding integration-boundary, contract, and data-entity-aware test cases.

## Scope

- **Input:** Bug description, task/feature description, module name, or list of entry points (endpoint, screen, job).
- **Output:** Structured dependency report (upstream, downstream, shared, data entities, integration points) consumable by TestCaseGeneratorAgent.

## Workflow

### 0. Merge-first and conditional sync (Rule 35a) [MANDATORY when user gives a Jira/bug/task]

- **First:** Use the **Jira key** (e.g. BUG-1234) or task/bug identifier from the user.
- **Look up merge history** for that key: local git (commit/merge messages, branch names) and, where available, GitLab (MRs for that Jira, merged state, target branch). Identify which branch(es), commits/MRs, and files/modules changed.
- **If a merge exists for this Jira** on a target branch (e.g. dev, dev2): run a **targeted sync** for that branch only (same safe read-only flow as `!sync` / `!update <branch>` per `.cursor/rules/git_sync_workflow.mdc`). If no merge found, skip sync.
- **Technical details:** Add merge-derived info (MR/merge commit, changed files/modules, short summary) to the output as **technical_details** for the report and for test-case-generator.

### 1. Before running

- Call **IntegrationService.update_before_task()** (Rule 11).
- **Consult PhoenixExpert** when you need to study the project or scope (Rule 8). The finder may turn to the expert for backend services, APIs, schemas, or where changes could have impact; the expert’s response is used to enrich dependencies and impact risks. The finder then returns the report to the parent so it can be passed to the test-case-generator (Rule 35).

### 2. Define scope

- Parse the prompt to identify entry points (e.g. REST endpoint, UI flow, scheduler job).
- List modules or services in scope.

### 3. Discover dependencies

| Source        | What to find                                                                 |
|---------------|-------------------------------------------------------------------------------|
| **Codebase**  | Imports, API clients, DB access, event producers/consumers, shared libs.    |
| **Confluence**| Architecture docs, dependency diagrams, service contracts (via MCP).         |

Collect:

- **Upstream:** What the scope depends on (other services, DB tables, external APIs, config).
- **Downstream:** What depends on the scope (callers, subscribers, UI).
- **Shared:** Common libs, schemas, DTOs, contracts.
- **Data flow:** Key entities (e.g. contract, POD, liability) and tables.
- **What could break:** In code links/references, identify anything that could break as a result of changes (callers of changed code, consumers of changed APIs, contract users, dependent UI or jobs). This list is passed to the test-case-generator so it can add regression and impact tests.

### 4. Output format (for TestCaseGeneratorAgent)

Structured payload that the test-case-generator can use:

```json
{
  "scope": "short description",
  "entry_points": ["list of entry points"],
  "upstream": [
    { "type": "api|db|service|lib", "name": "...", "usage": "..." }
  ],
  "downstream": [
    { "type": "api|consumer|ui", "name": "...", "usage": "..." }
  ],
  "shared": ["lib or schema names"],
  "data_entities": ["contract", "POD", ...],
  "integration_points": ["list of integration points to test"],
  "what_could_break": [
    { "item": "caller/consumer/contract/user", "location": "file or service", "reason": "why change could affect it" }
  ],
  "technical_details": "merge/MR info when user provided Jira/bug/task (Rule 35a): which MR/merge, changed files/modules, short summary"
}
```

- Save under **Cursor-Project/cross_dependencies/** (e.g. `YYYY-MM-DD_<scope_slug>.json`) when applicable.
- Same structure can be returned in the agent response for immediate handoff.

### 5. Integration with Test Case Generator

**Rule 35 (workflow_rules.mdc):** When the user requests test case creation, the parent MUST run **cross-dependency-finder** first, then pass its output to **test-case-generator**. Never skip the cross-dependency step.

When generating test cases for the **same scope**:

1. Run **cross-dependency-finder** first (mandatory; it may consult PhoenixExpert to study the project). Obtain the report including `what_could_break`.
2. Call **test-case-generator** with:
   - `prompt=...`, `prompt_type='bug'|'task'`, `confluence_data=...`, `context={'codebase_findings': ..., 'cross_dependency_data': <cross-dependency output>}`.
3. Test-case-generator uses `cross_dependency_data` to:
   - Add **integration tests** at API/service boundaries.
   - Add **negative cases** for missing or invalid upstream data.
   - Add **contract/API compatibility** cases where relevant.
   - Add **regression/impact tests** for items in `what_could_break`.
   - Improve **data-entity coverage** (e.g. POD, contract, liability) in test scenarios.

## Cursor subagent descriptor

The agent is exposed as a Cursor subagent in:

- **File:** `.cursor/agents/cross-dependency-finder.md`
- **When to use:** User asks for dependency analysis, cross-dependencies, or more robust test coverage; or when generating test cases that should consider dependencies.

## Constraints

- **READ-ONLY:** No modification of production code; only read Confluence and codebase.
- All output in **English** (Rule 0.7).
- Reporting per Rule 0.6 when required (ReportingService after the run).

## File organisation

- **Agent descriptor:** `.cursor/agents/cross-dependency-finder.md`
- **Design doc:** `Cursor-Project/docs/CROSS_DEPENDENCY_FINDER_AGENT.md` (this file)
- **Output (optional):** `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`

## Summary

- **Cross-Dependency Finder** discovers upstream, downstream, shared, and data-entity dependencies for a scope.
- Its output is **shared with TestCaseGeneratorAgent** via `context['cross_dependency_data']`.
- Test cases become **better covered** with integration-boundary, contract, and data-entity-aware scenarios.
