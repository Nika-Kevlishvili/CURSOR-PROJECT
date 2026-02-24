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

1. **IntegrationService** – Call `IntegrationService.update_before_task()` FIRST (Rule 11).
2. **PhoenixExpert** – Consult when you need to study the project or scope (Rule 8). The finder may turn to the expert; return the report to the parent for test-case-generator.
3. **Define scope** – From bug/task/feature description: entry points, modules, services in scope.
4. **Find cross-dependencies** – Codebase (imports, API clients, DB, callers, consumers) + Confluence (MCP) for architecture/docs. Identify **what could break** (callers, consumers, contract usage).
5. **Output** – Structured report: scope, entry_points, upstream, downstream, shared, data_entities, integration_points, **what_could_break**.
6. **Report** – If Rule 0.6 applies, call ReportingService after the run.

## Output Format

Produce (and optionally save to `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`):

- **scope**, **entry_points**
- **upstream** (api|db|service|lib), **downstream** (api|consumer|ui)
- **shared**, **data_entities**, **integration_points**
- **what_could_break**: list of `{ "item", "location", "reason" }` for regression/impact tests

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
- "Run cross-dependency finder for this bug before writing test cases"
