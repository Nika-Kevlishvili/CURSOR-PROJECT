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

### 0. Environment prerequisite (Rule TC-ENV-ASK.0 / PHOENIX-SWITCH.0) [MANDATORY before Phoenix codebase]

- **Parent orchestrator** MUST resolve environment (`dev` … `experiments`) **before** invoking cross-dependency-finder when this run is part of **Rule 35** test-case generation (or any Phoenix code read).
- If the prompt does not include `Target environment: <env>` (or equivalent) from a completed **TC-ENV-ASK.0** / **`environment-resolver`** step, **STOP** and return `PROCESS BLOCKED: environment not resolved — ask user (six options) before Phoenix reads` to the parent. Do **not** grep Phoenix or run `switch-phoenix-branches` on inferred Test/Dev.
- **Jira-only fetch** may already exist from the parent; that does not satisfy environment resolution.
- When environment is provided: align Phoenix via `.cursor/commands/switch-phoenix-branches.ps1 -Environment <env>` unless parent already aligned same env (Rule PHOENIX-SWITCH.0 §7a).

### 0b. Jira-anchored analysis (Rule 35a) [when user gives a Jira/bug/task]

- **Jira:** Load issue (key, summary, description, links, key custom fields) via **Jira MCP** first; if MCP fails after retries, use **REST read fallback** per **`.cursor/rules/integrations/jira_rest_fallback.mdc`** (disclose `Jira source: REST fallback` in output to parent).
- **Codebase:** Search/read Phoenix code (READ-ONLY) for entry points, callers, **what_could_break** — **only after** step 0 environment + alignment.
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
- **Confluence (MCP) — deep exploration:** Confluence is a **primary evidence source** alongside Jira and codebase. The agent MUST actively search for and read relevant wiki pages to find **detailed descriptions, business rules, validation logic, and documented behavior** for the topic. If Confluence MCP fails after retries, use **read-only REST** per **`.cursor/rules/integrations/confluence_rest_fallback.mdc`** (disclose **`Confluence source: REST fallback …`** to the parent).
  - **Search broadly:** Use multiple CQL queries if needed (Jira key, service/module name, feature phrase, related entity names) to find all relevant pages.
  - **Read full pages:** When search results return relevant hits, use `getPage` to read the **full content** of each relevant page — not just snippets and titles.
  - **Walk related pages:** Follow descendants, "see also" links, and related pages when they are likely to contain dependency or integration details (e.g. upstream/downstream service descriptions, validation rules, status transitions, integration specs).
  - **Extract business context:** From Confluence pages, extract: business rules, validation logic, expected behavior, entity relationships, integration contracts, status flows, and any documented dependencies that the codebase alone cannot reveal.
  - **Stop condition:** Stop expanding Confluence reads when pages become clearly unrelated to the topic or when sufficient evidence has been gathered for `what_could_break` and dependency mapping.
  - If no relevant Confluence pages are found after broad search, note `confluence_deep: no_relevant_pages_found` in `technical_details`.
- **Collect:** upstream, downstream, shared, data_entities, integration_points, **what_could_break** (item, location, reason).

### 4. Output format

Produce structured payload for Test Case Generator:

- **scope**, **entry_points**
- **upstream** (type: api|db|service|lib, name, usage)
- **downstream** (type: api|consumer|ui, name, usage)
- **shared**, **data_entities**, **integration_points**
- **what_could_break**: `[{ "item", "location", "reason" }]`
- **technical_details**: Jira key + codebase-derived pointers/notes when user provided a Jira/bug/task (Rule 35a); merge/MR lists only if user explicitly requested GitLab review.
- **confluence_evidence**: list of Confluence pages read with title + page ID + key findings extracted from each.

Optionally save to `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`.

### 5. Handoff

- When test cases are requested, parent passes this output as `context['cross_dependency_data']` to test-case-generator.

## READ-ONLY

- Only read Confluence and codebase; do not modify production code.
- All output in English (Rule 0.7).

## Integration

- PhoenixExpert when studying project/scope.
- Optional: save markdown under `reports/` if the user asks; not required after every run (Rule 0.6; no Python ReportingService).
- End with: "Agents involved: CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Confidence Score (Rule CONF.1 — Three-Zone) [MANDATORY]

The final output MUST include an **evidence-based Confidence Score**: `**Confidence: XX% (ZONE)**` with evidence factors. Compute from the **Cross-Dependency Discovery (Rule 35a)** factor table in **`.cursor/rules/scoring/confidence_scoring_matrix.mdc`**: base 40, add/subtract per evidence factor. Key factors: Phoenix code cited (+20), Confluence deep exploration completed (+15), Jira loaded (+10), what_could_break populated (+5), no Confluence found (-10), single source only (-15). Zones: **GO** (≥ 85%), **CAUTION** (55–84% + assumptions + verify list), **STOP** (< 55% — do not deliver final analysis, ask user). Be honest — do not inflate.

## Command reference

- **Saved pattern (stable):** `Cursor-Project/docs/CROSS_DEPENDENCY_WORK_PATTERN.md`
- Subagent: `.cursor/agents/cross-dependency-finder.md`
- Design: `Cursor-Project/docs/CROSS_DEPENDENCY_FINDER_AGENT.md`
