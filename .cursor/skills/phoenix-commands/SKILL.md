---
name: phoenix-commands
description: Maps user intent to Cursor commands and workflows (Phoenix query, consult, report, bug-validate, jira-bug, sync, cross-dependency-finder, test-case-generate, energo-ts-run, hands-off command). Use when the user asks how to run a workflow or which command to use. For HandsOff: there is no separate hands-off skill—use slash command **hands-off** or triggers **/HandsOff** / **!HandsOff** per Rule 37 and `.cursor/commands/hands-off.md`.
---

# Phoenix Commands and Workflows

Helps choose the right command or workflow for Phoenix-related tasks. Commands live in `.cursor/commands/`; rules in `.cursor/rules/`.

## When to Apply

- User asks how to ask Phoenix a question, consult, generate a report, validate a bug, or sync from GitLab.
- User mentions a command by name (phoenix, consult, report, bug-validate, sync).
- Need to align a request with the correct workflow.

## Command → Workflow

| User intent | Command / workflow | Rule / reference |
|-------------|--------------------|-------------------|
| Phoenix question (how/what/why, endpoints, logic) | **Phoenix** command → Route to PhoenixExpert (Rule 0.2) | phoenix.md |
| Before doing a task (validation, approval) | **Consult** → PhoenixExpert consultation (Rule 8) | consult.md |
| After any task | **Report** → Save agent + summary reports (Rule 0.6) | report.md |
| Validate a bug report | **Bug-validate** → BugFinderAgent (Rule 32) | bug-validate.md |
| Create/rewrite Jira bug (Experiments board only) | **Jira-bug** → jira-bug-template (Rule JIRA.0; NOT Phoenix delivery) | jira-bug.md |
| Production data analysis (liability offsets, receivable history) | **Production-data-reader** → ProductionDataReaderAgent (Rule PDR.0) | production-data-reader.md, production_data_reader.mdc |
| Fetch/update/checkout Phoenix repos from GitLab | **Sync** → git_sync_workflow (read-only) | sync.md, git_sync_workflow.mdc |
| Find cross-dependencies (what could break) | **Cross-dependency-finder** → CrossDependencyFinderAgent (Rule 35) | cross-dependency-finder.md |
| Generate test cases from bug/task | **Test-case-generate** → cross-dependency-finder FIRST, then TestCaseGeneratorAgent (Rule 35) | test-case-generate.md |
| Run Playwright test(s) from EnergoTS/GitHub (by prompt) | **Energo-ts-run** → Resolve test from prompt → run `npx playwright test` from local EnergoTS | energo-ts-run.md |
| Full HandsOff flow (Jira ticket → cross-deps → test cases → Playwright → run → report + Slack) | **Hands-off** → hands-off.md; route to hands-off orchestrator | hands-off.md |

## Phoenix (phoenix.md)

- **When:** Any Phoenix-related question.
- **Flow:** Confluence (MCP, fresh) → Codebase → PhoenixExpert answer (Rule 0.3: no Python IntegrationService).
- **Output:** Start with "**Expert:** PhoenixExpert", end with "Agents involved: PhoenixExpert", save reports.

## Consult (consult.md)

- **When:** Before any task that affects Phoenix (tests, Postman, GitLab, env access, etc.).
- **Flow:** Describe task → PhoenixExpert review → Approval required (in chat; Rule 0.3).
- **Output:** "Agents involved: PhoenixExpert, [others]". Consultation is binding (Rule 27).

## Report (report.md)

- **When:** After every task (Rule 0.6).
- **Flow:** Write markdown under `Cursor-Project/reports/YYYY-MM-DD/` (no Python ReportingService).
- **Location:** `Cursor-Project/reports/YYYY-MM-DD/` with current date.

## Bug-validate (bug-validate.md)

- **When:** User wants to validate or verify a bug report.
- **Flow:** Rule 32 in chat → Confluence → Codebase → analysis → report file (no `get_bug_finder_agent`).
- **Output:** Structured Bug Validation Analysis; "Agents involved: BugFinderAgent, PhoenixExpert".

## Jira-bug (jira-bug.md)

- **When:** User wants to create a Jira bug or rewrite an existing one on the **Experiments** board. **Not for Phoenix delivery** (Rule JIRA.0).
- **Triggers:** `!jira-bug`, "create Jira bug", "Experiments board bug".
- **Flow:** Confirm Experiments board → extract/ask details → fill template (Summary, Description, Steps, Expected, Actual, Environment, Technical details, Example).
- **Output:** Ready-to-paste Jira text; "Agents involved: jira-bug (Jira bug agent)".

## Production-data-reader (production-data-reader.md)

- **When:** User asks about production database data, liability offsets, receivable history, or how an entity was created.
- **Flow:** PostgreSQLProd MCP (readonly) → SELECT queries → step-by-step explanation (see `integrations/production_data_reader.mdc`).
- **Output:** Detailed analysis with offset sequence and creation process; "Agents involved: ProductionDataReaderAgent".

## Sync (sync.md)

- **When:** User wants to fetch/update/checkout Phoenix projects from GitLab.
- **Triggers:** `!sync`, `!update <branch>`, `!checkout <branch>`.
- **Rule:** Follow `.cursor/rules/integrations/git_sync_workflow.mdc` exactly; use git commands (no Python agent for sync). Read-only (fetch/checkout/merge only; no push).

## Cross-dependency-finder (cross-dependency-finder.md)

- **When:** User asks for cross-dependencies, what could break, or dependency analysis for a scope (bug/task/feature). Also run automatically before test case generation (Rule 35).
- **Flow (Rule 35a):** **Jira MCP + codebase + shallow Confluence** — **no** local merge/git for cross-dep. PhoenixExpert (if needed) → scope → output (upstream, downstream, what_could_break, **technical_details** from Jira + code) → optional JSON to `Cursor-Project/cross_dependencies/`.
- **Output:** Structured report including **technical_details**; "Agents involved: CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Test-case-generate (test-case-generate.md)

- **When:** User asks to generate test cases for a bug or task.
- **Flow:** Rule 35: (1) Run **cross-dependency-finder** first (including Rule 35a) → (2) **MANDATORY:** read **`Cursor-Project/config/playwright_generation/playwright instructions/`** (see `test-case-generate.md` step 0) → (3) Run **test-case-generator** with cross_dependency_data. Prefer **`Cursor-Project/test_cases/`** (Objects/ and Flows/ per `workspace/test_cases_structure.mdc`); `generated_test_cases/` is legacy.
- **Output:** Test cases in human-readable hierarchy; "Agents involved: TestCaseGeneratorAgent, CrossDependencyFinderAgent" (and PhoenixExpert if consulted).

## Energo-ts-run (energo-ts-run.md)

- **When:** User wants to run specific Playwright tests from EnergoTS based on prompt (e.g. "run newly created test", "run test REG-123", "run from GitHub").
- **Flow:** Resolve which test(s) from prompt → Run `npx playwright test <target>` from `Cursor-Project/EnergoTS/` (`cursor` branch only) → Report results.
- **Output:** Test run summary (passed/failed); report to `Cursor-Project/reports/YYYY-MM-DD/`; "Agents involved: EnergoTS Playwright Test Runner".
- **Note:** Tests run from local repo (synced from GitHub). Suggest `!sync` if user wants latest code first.

## Hands-off (hands-off.md)

- **Trigger (command-only):** Run via Cursor slash command **hands-off** (`.cursor/commands/hands-off.md`) and/or user text **/HandsOff** or **!HandsOff** with a Jira key (Rule 37). There is **no** `hands-off` entry under `.cursor/skills/`—do not rely on a dedicated HandsOff skill.
- **When:** User provides a **Jira ticket** (link, key e.g. REG-123, or name) and types **/HandsOff** or **!HandsOff** (or runs the **hands-off** command).
- **Flow:** Route to **hands-off** orchestrator. Full flow: (1) Get Jira ticket and description (Jira MCP); (2) Run cross-dependency-finder (Rule 35a); (3) Run test-case-generator with cross_dependency_data; (4) Create Playwright tests from test cases (bridge .md → spec → EnergoTSTestAgent); (5) Run Playwright tests (energo-ts-run, cursor branch); (6) Save report as **`Cursor-Project/reports/YYYY-MM-DD/{JIRA_KEY}.md`** with pass/fail and reasons; (7) Send report to Slack to the tester and duplicate to the AI report channel (user-slack MCP).
- **Output:** Summary of run; report file; Slack delivery. "Agents involved: HandsOff (orchestrator), CrossDependencyFinderAgent, TestCaseGeneratorAgent, EnergoTSTestAgent, EnergoTS Playwright Test Runner".

## Summary

- Phoenix questions → Phoenix command + PhoenixExpert.
- Before task → Consult + PhoenixExpert approval.
- After task → Report (agent + summary).
- Bug check → Bug-validate + BugFinderAgent.
- Jira bug (Experiments only) → Jira-bug command + jira-bug-template; never Phoenix delivery.
- Production data → Production-data-reader + ProductionDataReaderAgent.
- Git sync → Sync command + git_sync_workflow.mdc.
- Cross-dependencies → Cross-dependency-finder command + CrossDependencyFinderAgent.
- Test cases → Test-case-generate command (cross-dependency-finder first, then TestCaseGeneratorAgent).
- Run Playwright tests from EnergoTS by prompt → Energo-ts-run command (resolve test, run locally).
- Full HandsOff flow (Jira + /HandsOff or !HandsOff or **hands-off** slash command) → `.cursor/commands/hands-off.md` + hands-off orchestrator (no separate hands-off skill).

All commands assume rules are loaded first (Rule 0.0) from `.cursor/rules/`.
