# Cross-Dependency Finder

Find cross-dependencies for a scope and share output with Test Case Generator (Rule 35). Route to **cross-dependency-finder** subagent / CrossDependencyFinderAgent.

**Saved pattern:** `Cursor-Project/docs/CROSS_DEPENDENCY_WORK_PATTERN.md` — **no** local merge/git for cross-dep (Rule 35a).

## When to Use

Use this command when the user asks about:
- Cross dependencies for a bug, task, or feature
- What could break as a result of changes
- Dependency analysis for a scope
- Upstream/downstream dependencies, integration points
- Preparing context for test case generation (run this BEFORE generating test cases)

## Mandatory Workflow

0. **Jira-anchored cross-dependency (Rule 35a)** – When the user provides a **Jira/bug/task** key:
   - Use **Jira MCP** + **codebase** (Phoenix READ-ONLY) + **shallow Confluence** (step 4 below).
   - **Do not** run local **git log** / merge history / git snapshot scripts for the ticket as part of this workflow; **do not** trigger **git sync** only because cross-dep ran.
   - **technical_details:** from Jira + codebase. **GitLab MR** only if the user **explicitly** asks.
1. **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed.
2. **PhoenixExpert** – Consult when you need to study the project or scope (Rule 8). The finder may turn to the expert; return the report to the parent for test-case-generator.
3. **Define scope** – From bug/task/feature description: entry points, modules, services in scope.
4. **Find cross-dependencies** – Codebase (imports, API clients, DB, callers, consumers) + **Confluence (MCP) shallow only:** one search/CQL, snippets/titles; at most **one** full page if clearly primary — **no** deep wiki walks. Identify **what could break** (callers, consumers, contract usage).
5. **Output** – Structured report: scope, entry_points, upstream, downstream, shared, data_entities, integration_points, **what_could_break**, **technical_details** (from merges when Jira/bug/task was provided).
6. **Report (optional)** – Save markdown under **Chat reports** (`YYYY/<english-month>/<DD>/` per **`Cursor-Project/reports/README.md`**) only if the user asks for a file; otherwise return the structured output in chat (Rule 0.6 default; no Python ReportingService).

## Output Format

Produce (and optionally save to `Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`):

- **scope**, **entry_points**
- **upstream** (api|db|service|lib), **downstream** (api|consumer|ui)
- **shared**, **data_entities**, **integration_points**
- **what_could_break**: list of `{ "item", "location", "reason" }` for regression/impact tests
- **technical_details**: Jira + codebase notes when key provided (Rule 35a); MR/merge only if user explicitly requested GitLab

This output is passed to **test-case-generator** as `context['cross_dependency_data']` when the user requests test cases (Rule 35).

## Constraints

- **READ-ONLY:** Only read Confluence and codebase; do not modify production code.
- All output in **English** (Rule 0.7).

## Response Requirements

- State "**Agent:** CrossDependencyFinderAgent" at beginning when applicable.
- Return the cross-dependency report (and file path if saved).
- End with: "Agents involved: CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Example Triggers

- "Find cross dependencies for the billing profile change"
- "What could break if we change the customer API?"
- "Dependency analysis for REG-1234"
- "Run cross-dependency finder for BUG-1234" (Rule 35a: Jira + codebase + shallow Confluence)
- "Run cross-dependency finder for this bug before writing test cases"
