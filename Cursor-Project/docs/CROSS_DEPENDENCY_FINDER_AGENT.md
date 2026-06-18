# Cross-Dependency Finder Agent – Design and Integration

> **Saved work pattern (Jira + codebase + deep Confluence exploration; no local merge/git):** **`CROSS_DEPENDENCY_WORK_PATTERN.md`** in this folder. **Rule 35a** in `workflow_rules.mdc` forbids mandatory local merge/git for cross-dep.

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

### 0. Jira-anchored scope (Rule 35a) [when user gives a Jira/bug/task]

- **Jira MCP:** Fetch issue (key, summary, description, relevant fields).
- **Codebase:** Search/read under `Cursor-Project/Phoenix/**` (READ-ONLY) for services, callers, integration points tied to the ticket.
- **Do NOT (default):** local `git log` / merge history / `git show` keyed by Jira; **GitLab MR** lists; conditional **git sync** solely because cross-dep ran. Optional GitLab (read-only) **only** if the user **explicitly** asks.
- **technical_details:** Short notes from Jira + codebase (paths, class names, behaviour) — **not** mandatory merge-commit or MR enumeration.

### 1. Before running

- **Rule 0.3:** In Cursor chat there is no Python `IntegrationService`; use MCP/Jira/GitLab steps when the task needs external context.
- **Consult PhoenixExpert** when you need to study the project or scope (Rule 8). The finder may use Confluence + codebase for backend services, APIs, schemas, or impact; enrich dependencies and **what_could_break**. Return the report to the parent for test-case-generator (Rule 35).

### 2. Define scope

- Parse the prompt to identify entry points (e.g. REST endpoint, UI flow, scheduler job).
- List modules or services in scope.

### 3. Discover dependencies

| Source        | What to find                                                                 |
|---------------|-------------------------------------------------------------------------------|
| **Codebase**  | Imports, API clients, DB access, event producers/consumers, shared libs.    |
| **Confluence**| **Deep exploration:** multiple CQL queries allowed; read **full pages**; walk descendants and related pages to find detailed descriptions, business rules, validation logic, and documented dependencies. Stop when pages become unrelated or sufficient evidence is gathered. |

#### 3a. Confluence deep exploration procedure

Confluence is a **primary evidence source** alongside codebase and Jira. The finder MUST actively search and read Confluence to find documented business rules, validation logic, and dependencies that code alone cannot reveal.

**Search strategy:**

1. **Broad CQL queries:** Run multiple searches using different angles — Jira key, service/module name, feature phrase, related entity names (e.g. "contract creation", "billing run", "POD linking"). Do not stop at a single query.
2. **Full page reads:** For every relevant search hit, call `getPage` to read the **full content** — not just snippets or titles. Extract: business rules, validation rules, expected behavior, status transitions, entity relationships, integration contracts.
3. **Walk related pages:** Follow descendant pages, "see also" links, and related pages referenced in the content when they are likely to contain upstream/downstream service descriptions, validation rules, or integration specs.
4. **Stop condition:** Stop expanding when pages become clearly unrelated to the scope or when sufficient evidence has been gathered for `what_could_break` and the dependency map.

**What to extract from Confluence:**

- Business rules and validation logic (e.g. "contract must be ACTIVE before billing", "POD must be linked")
- Entity lifecycle and status transitions (e.g. contract statuses, invoice states)
- Integration contracts between services (e.g. "billing calls receivable service after invoice creation")
- Documented dependencies and data flows (e.g. "disconnection request requires liability check")
- Expected behavior that code alone does not document (product owner intent, edge case rules)

**Output:** Populate `confluence_evidence` in the output payload with: page title, page ID, and key findings extracted from each page.

#### 3b. Codebase discovery

Collect from codebase:

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
  "technical_details": "Jira key + codebase-derived notes (paths, services); merge/MR lists only if user explicitly requested GitLab review",
  "confluence_evidence": [
    { "page_title": "...", "page_id": "...", "key_findings": ["business rule X", "validation Y", "dependency Z"] }
  ]
}
```

- Save under **Cursor-Project/cross_dependencies/** (e.g. `YYYY-MM-DD_<scope_slug>.json`) when applicable.
- Same structure can be returned in the agent response for immediate handoff.

### 5. Integration with Test Case Generator

**Rule 35 (workflow_rules.mdc):** When the user requests test case creation, the parent MUST run **cross-dependency-finder** first, then pass its output to **test-case-generator**. Never skip the cross-dependency step.

When generating test cases for the **same scope**:

1. Run **cross-dependency-finder** first (mandatory; it may consult PhoenixExpert to study the project). Obtain the report including `what_could_break`.
2. Call **test-case-generator** (subagent/skill) with the same scope and structured **`cross_dependency_data`** (plus Confluence/codebase findings as available in chat).
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
- Reporting per Rule 0.6 when required: save markdown under **Chat reports** per **`Cursor-Project/reports/README.md`** (no Python ReportingService).

## File organisation

- **Agent descriptor:** `.cursor/agents/cross-dependency-finder.md`
- **Design doc:** `Cursor-Project/docs/CROSS_DEPENDENCY_FINDER_AGENT.md` (this file)
- **Output (optional):** `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`

## Summary

- **Cross-Dependency Finder** discovers upstream, downstream, shared, and data-entity dependencies for a scope.
- Its output is **shared with TestCaseGeneratorAgent** via `context['cross_dependency_data']`.
- Test cases become **better covered** with integration-boundary, contract, and data-entity-aware scenarios.
