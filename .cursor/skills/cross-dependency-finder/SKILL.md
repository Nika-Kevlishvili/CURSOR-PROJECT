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

### 0. Merge-first and conditional sync (Rule 35a) [MANDATORY when user gives a Jira/bug/task]

- **First:** Use the **Jira key** (e.g. BUG-1234) or task/bug identifier from the user.
- **Look up merge history** for that key: local git (commit/merge messages, branch names) and, where available, GitLab (MRs for that Jira, merged state, target branch). Identify which branch(es), commits/MRs, and files/modules changed.
- **If a merge exists for this Jira** on a target branch (e.g. dev, dev2): run a **targeted sync** for that branch only (same safe read-only flow as `!sync` / `!update <branch>` per `git_sync_workflow.mdc`). If no merge found, skip sync.
- **Technical details:** Add merge-derived info (MR/merge commit, changed files/modules, short summary) to the output as **technical_details** for the report and for test-case-generator.

### 1. Before running

- Call `IntegrationService.update_before_task()` (Rule 11).
- Consult **PhoenixExpert** when you need to study the project or scope (Rule 8). The finder may turn to the expert; return the report to the parent for test-case-generator.

### 2. Define scope

- From bug/task/feature: entry points, modules, services in scope.
- Output: clear scope (entry_points, modules, services).

### 3. Find cross-dependencies

- **Codebase:** Imports, references, API clients, DB access, event producers/consumers, shared libs. In code links/references, identify **what could break** (callers, consumers, contract users).
- **Confluence (MCP):** Architecture docs, dependency diagrams, service contracts.
- **Collect:** upstream, downstream, shared, data_entities, integration_points, **what_could_break** (item, location, reason).

### 4. Output format

Produce structured payload for Test Case Generator:

- **scope**, **entry_points**
- **upstream** (type: api|db|service|lib, name, usage)
- **downstream** (type: api|consumer|ui, name, usage)
- **shared**, **data_entities**, **integration_points**
- **what_could_break**: `[{ "item", "location", "reason" }]`
- **technical_details**: merge/MR info for the Jira key (which MR/merge, changed files/modules, short summary) — always include when user provided a Jira/bug/task (Rule 35a).

Optionally save to `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`.

### 5. Handoff

- When test cases are requested, parent passes this output as `context['cross_dependency_data']` to test-case-generator.

## READ-ONLY

- Only read Confluence and codebase; do not modify production code.
- All output in English (Rule 0.7).

## Integration

- IntegrationService before task.
- PhoenixExpert when studying project/scope.
- ReportingService after run if Rule 0.6 applies.
- End with: "Agents involved: CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Command reference

- `.cursor/commands/cross-dependency-finder.md`
- Subagent: `.cursor/agents/cross-dependency-finder.md`
- Design: `Cursor-Project/docs/CROSS_DEPENDENCY_FINDER_AGENT.md`
