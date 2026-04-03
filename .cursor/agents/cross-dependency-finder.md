---
name: cross-dependency-finder
model: inherit
description: Finds cross-dependencies (modules, services, APIs, DB) for a scope. Shares output with TestCaseGeneratorAgent so test cases are better covered. Use when the user asks for dependency analysis, cross-dependencies, or more robust test coverage.
---

# Cross-Dependency Finder Subagent

Find cross-dependencies for a given scope. Output is shared with test-case-generator for better test coverage.

## Before Running

1. Call IntegrationService.update_before_task() (Rule 11)
2. Consult PhoenixExpert when needed for backend services, APIs, schemas, entry points (Rule 8)

## Workflow

### 0. Merge-first (Rule 35a) [when user gives a Jira key]

- Look up merge history for the Jira key (local git + GitLab MRs)
- If merge exists on a target branch: run targeted sync for that branch (read-only fetch + merge)
- Add merge-derived info (MR, changed files, summary) as **technical_details** in output

### 1. Define scope

From bug/task/feature: identify entry points, modules, services in scope.

### 2. Find cross-dependencies

- **Codebase:** imports, references, API clients, DB access, events, shared libs. Identify what could break.
- **Confluence (MCP):** architecture docs, dependency diagrams, service contracts.
- **Collect:** upstream (dependencies), downstream (callers/consumers), shared libs, data entities, integration points, **what_could_break**.

### 3. Output (for test-case-generator)

Structured JSON: scope, entry_points, upstream, downstream, shared, data_entities, integration_points, what_could_break, technical_details. Save to `Cursor-Project/cross_dependencies/` or return to parent.

## Constraints

READ-ONLY. All output in English. End with "Agents involved: CrossDependencyFinderAgent" (+ PhoenixExpert if consulted).

Full workflow: `.cursor/commands/cross-dependency-finder.md`
