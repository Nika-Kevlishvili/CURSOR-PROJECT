---
name: energo-ts-run
description: Runs specific Playwright tests from EnergoTS (local repo synced from GitHub) based on user prompt. Resolves which test to run (newly created, Jira key, file path, or domain) and executes npx playwright test. Use when the user asks to run a Playwright test, run a newly created test, or run a specific test from GitHub/EnergoTS.
---

# EnergoTS Playwright Test Run Skill

Use this skill when the user wants to **run** (execute) Playwright tests from the EnergoTS project based on a natural-language prompt. Tests run from the **local** EnergoTS repo (synced from GitHub), **only from the `cursor` branch** (Rule ENERGOTS.0).

## When to Apply

- User says: "run the newly created test", "run test REG-123", "run this test from GitHub", "run Playwright test", "run customer.spec.ts", "run all billing tests".
- User asks to execute a specific test or set of tests in EnergoTS/Playwright.
- User mentions running tests "from GitHub" (interpret as: run from local clone; suggest sync if they want latest code).

## Workflow

1. **Ensure `cursor` branch**: In `Cursor-Project/EnergoTS/`, if current branch is not `cursor`, run `git checkout cursor`. Tests must run only from `cursor`.
2. **Resolve test from prompt**
   - "Newly created" → find most recent `.spec.ts` in `Cursor-Project/EnergoTS/tests/`.
   - Jira key (e.g. REG-123) → grep in tests, run matching file(s) or `npx playwright test --grep "REG-123"`.
   - File path/name → run that path relative to `EnergoTS/`.
   - Domain (e.g. "billing") → run `tests/billing/` or equivalent.
3. **Execute** from `Cursor-Project/EnergoTS/`: `npx playwright test <path|grep|dir>`.
4. **Report** results; optional save under **Chat reports** per **`Cursor-Project/reports/README.md`**; end with "Agents involved: EnergoTS Playwright Test Runner".

## Agent / Command

- **Agent**: `.cursor/agents/energo-ts-run.md` (EnergoTS Playwright Test Runner).
- **Command**: `.cursor/commands/energo-ts-run.md`.

## Rules

- **Rule 0.3:** no Python IntegrationService in this workspace; follow MCP/Jira when needed.
- No code modification; only run tests (Rule 0.8).
- Summarize test run in chat (English). Save a file under `reports/` only if the user requests it (Rule 0.6 default, Rule 0.7 for on-disk text).
