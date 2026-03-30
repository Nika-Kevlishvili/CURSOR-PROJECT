---
name: cross-dependency-finder
description: Finds cross-dependencies (modules, services, APIs, DB) for a scope and identifies what could break. Consults PhoenixExpert when needed; output is shared with Test Case Generator. Use when the user asks for dependency analysis, cross-dependencies, what could break, or before generating test cases (Rule 35).
---

# Cross-Dependency Finder Skill

Ensures cross-dependency discovery follows the CrossDependencyFinderAgent workflow: define scope → codebase + Confluence → upstream/downstream/what_could_break → structured output for Test Case Generator. READ-ONLY; no code changes.

## When to Apply

- User asks for cross-dependencies, dependency analysis, or "what could break" for a bug, task, or feature.
- User wants to understand impact of changes (callers, consumers, integration points).
- Before generating test cases: Rule 35 requires running cross-dependency-finder first and passing its output to test-case-generator.
- User mentions "cross dependency", "dependencies", "what could break", or "integration points".

## Mandatory: Use Cross-Dependency Finder

Route to cross-dependency-finder subagent / CrossDependencyFinderAgent. Do not do ad-hoc dependency analysis without the structured output format.

## Workflow

### 0. Jira-anchored analysis (Rule 35a) [when user gives a Jira/bug/task]

- **Jira MCP:** Load issue (key, summary, description).
- **Codebase:** Search/read Phoenix code (READ-ONLY) for entry points, callers, **what_could_break**.
- **Prohibited (default):** local **`git log` / merge lookup** for the ticket key; **`git show`** archaeology; **git sync** triggered only because cross-dep ran; any removed **git snapshot** script.
- **GitLab MR/merge:** **Only** if the user **explicitly** asks; read-only MCP.
- **technical_details:** From Jira + codebase (paths, services, notes) — **not** mandatory MR/merge-commit lists.

### 1. Before running

- **Rule 0.3:** no Python IntegrationService in this workspace; use MCP/Jira when the task needs external context.
- Consult **PhoenixExpert** when you need to study the project or scope (Rule 8). Return the report to the parent for test-case-generator.

### 2. Define scope

- From bug/task/feature: entry points, modules, services in scope.
- Output: clear scope (entry_points, modules, services).

### 3. Find cross-dependencies

- **Codebase:** Imports, references, API clients, DB access, event producers/consumers, shared libs. In code links/references, identify **what could break** (callers, consumers, contract users).
- **Confluence (MCP) — shallow only (default):** Do **not** dig deeply. **Primary evidence** remains **Jira + codebase**; Confluence is **light context** only.
  - **At most one** search or **one** CQL query for the topic (Jira key, service name, or feature phrase).
  - Use **search snippets and titles** from the first page of results; treat them as sufficient unless **exactly one** hit is obviously the owning spec — then **at most one** `getPage` for that page.
  - **Forbidden for this workflow:** walking descendants, footers, many related pages, multi-hop “see also”, or full long-page reads. Extract only what is **surface-visible** (title, snippet, top headings if one page is opened).
  - If top results are weak or redundant with git/Jira, **stop** — note `confluence_shallow: skipped_or_snippets_only` in `technical_details` or a short note in the narrative output.
- **Collect:** upstream, downstream, shared, data_entities, integration_points, **what_could_break** (item, location, reason).

### 4. Output format

Produce structured payload for Test Case Generator:

- **scope**, **entry_points**
- **upstream** (type: api|db|service|lib, name, usage)
- **downstream** (type: api|consumer|ui, name, usage)
- **shared**, **data_entities**, **integration_points**
- **what_could_break**: `[{ "item", "location", "reason" }]`
- **technical_details**: Jira key + codebase-derived pointers/notes when user provided a Jira/bug/task (Rule 35a); merge/MR lists only if user explicitly requested GitLab review.

Optionally save to `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`.

### 5. Handoff

- When test cases are requested, parent passes this output as `context['cross_dependency_data']` to test-case-generator.

## READ-ONLY

- Only read Confluence and codebase; do not modify production code.
- All output in English (Rule 0.7).

## Integration

- PhoenixExpert when studying project/scope.
- Save markdown reports after run if Rule 0.6 applies (no Python ReportingService).
- End with: "Agents involved: CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Command reference

- **Saved pattern (stable):** `Cursor-Project/docs/CROSS_DEPENDENCY_WORK_PATTERN.md`
- `.cursor/commands/cross-dependency-finder.md`
- Subagent: `.cursor/agents/cross-dependency-finder.md`
- Design: `Cursor-Project/docs/CROSS_DEPENDENCY_FINDER_AGENT.md`
