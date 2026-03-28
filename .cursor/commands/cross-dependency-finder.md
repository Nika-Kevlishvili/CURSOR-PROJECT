# Cross-Dependency Finder

Find cross-dependencies for a scope and share output with Test Case Generator (Rule 35). Route to **cross-dependency-finder** subagent / CrossDependencyFinderAgent.

## When to Use

Use this command when the user asks about:
- Cross dependencies for a bug, task, or feature
- What could break as a result of changes
- Dependency analysis for a scope
- Upstream/downstream dependencies, integration points
- Preparing context for test case generation (run this BEFORE generating test cases)

## Mandatory Workflow

0. **Merge-first and conditional sync (Rule 35a)** – When the user provides a **Jira/bug/task** key:
   - **First** look up **merge history** for that key (local git + GitLab if available): which branch(es), MR(s), merge commit(s), and changed files/modules.
   - **If a merge exists** for this Jira on a target branch: run a **targeted sync** for that branch only (same safe read-only flow as `!update <branch>` per `git_sync_workflow.mdc`). If no merge found, skip sync.
   - Add merge-derived info to the output as **technical_details** (MR/merge, changed files/modules, short summary).
1. **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed.
2. **PhoenixExpert** – Consult when you need to study the project or scope (Rule 8). The finder may turn to the expert; return the report to the parent for test-case-generator.
3. **Define scope** – From bug/task/feature description: entry points, modules, services in scope.
4. **Find cross-dependencies** – Codebase (imports, API clients, DB, callers, consumers) + Confluence (MCP) for architecture/docs. Identify **what could break** (callers, consumers, contract usage).
5. **Output** – Structured report: scope, entry_points, upstream, downstream, shared, data_entities, integration_points, **what_could_break**, **technical_details** (from merges when Jira/bug/task was provided).
6. **Report** – If Rule 0.6 applies, save markdown under `Cursor-Project/reports/YYYY-MM-DD/` after the run (no Python ReportingService).

## Output Format

Produce (and optionally save to `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`):

- **scope**, **entry_points**
- **upstream** (api|db|service|lib), **downstream** (api|consumer|ui)
- **shared**, **data_entities**, **integration_points**
- **what_could_break**: list of `{ "item", "location", "reason" }` for regression/impact tests
- **technical_details**: merge/MR info for the Jira key when provided (Rule 35a): which MR/merge, changed files/modules, short summary

This output is passed to **test-case-generator** as `context['cross_dependency_data']` when the user requests test cases (Rule 35).

## Constraints

- **READ-ONLY:** Only read Confluence and codebase; do not modify production code.
- All output in **English** (Rule 0.7).

## Response Requirements

- State "**Agent:** CrossDependencyFinderAgent" at beginning when applicable.
- Return the cross-dependency report (and file path if saved).
- End with: "Agents involved: CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Generate Reports (Rule 0.6)

- Save to `Cursor-Project/reports/YYYY-MM-DD/CrossDependencyFinderAgent_{HHMM}.md`
- Save summary to `Cursor-Project/reports/YYYY-MM-DD/Summary_{HHMM}.md` when required.

## Example Triggers

- "Find cross dependencies for the billing profile change"
- "What could break if we change the customer API?"
- "Dependency analysis for REG-1234"
- "Run cross-dependency finder for BUG-1234" (merge lookup + conditional sync + technical_details applied)
- "Run cross-dependency finder for this bug before writing test cases"
