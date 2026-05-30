---
name: energo-ts-run
description: Runs specific Playwright tests from EnergoTS (local repo synced from GitHub) based on user prompt. Resolves which test to run (newly created, Jira key, file path, or domain) and executes npx playwright test. Use when the user asks to run a Playwright test, run a newly created test, or run a specific test from GitHub/EnergoTS.
---

# EnergoTS Playwright Test Run Skill

Use when the user wants to **run** (execute) Playwright tests from the local EnergoTS repo (**`cursor` branch only**, Rule ENERGOTS.0).

## When to Apply

- "Run newly created test", "run test REG-123", "run customer.spec.ts", "run all billing tests", "run Playwright from GitHub" (local clone).

## Workflow

1. **cursor branch** — in `Cursor-Project/EnergoTS/`, `git checkout cursor` if not already on cursor.
2. **Resolve target** (see table below).
3. **Execute** from `Cursor-Project/EnergoTS/`: `npx playwright test <path|grep|dir>`.
4. **Report** pass/fail; optional Chat reports file on user request (Rule 0.6).

## Resolution table

| User prompt | Action |
|-------------|--------|
| Newly created / latest | Most recent `.spec.ts` in `EnergoTS/tests/` (mtime or git status); ask if ambiguous |
| Jira key (REG-123, PDT-456) | Grep tests → `npx playwright test --grep "KEY"` or matching file(s) |
| File path / name | Normalize path under `tests/` and run |
| Domain (billing) | `npx playwright test tests/billing/` or equivalent |
| "From GitHub" | Clarify: run local clone; suggest sync if user wants latest remote |

## Setup (when fixtures missing)

From `EnergoTS/`: `npx playwright test --project=setup` (requires `.env` with PORTAL_USER, PASSWORD, DEVAUTHAPI or TESTAUTHAPI) → creates `fixtures/token.json`, `fixtures/envVariables.json`.

## Rule DPR.0 — detailed markdown

- **HandsOff / path 3:** orchestrator runs `node ../config/playwright/generate-detailed-report.mjs` and Slack-uploads **`playwright-report-detailed.md`** — not this agent's default.
- **Ad-hoc:** generate + upload only when user **explicitly** asks. Smart report template: **`Playwright_run_detailed_report_template.md`**.

## Error handling

| Condition | Action |
|-----------|--------|
| Not on `cursor` / checkout fails | Report error; do not run on other branch |
| EnergoTS or Playwright missing | Report path / `npm install` hint |
| No matching test | "No matching test found" + how you searched |
| Tests fail | Report failures; do not fix code unless user asked |

## Constraints

- **No code modification** (Rule 0.8). Summarize in English (Rule 0.7).
- **Rule 0.3:** no Python IntegrationService in this workspace.

## Agent

`.cursor/agents/energo-ts-run.md` (I/O contract only).

## Confidence Score (Rule CONF.1) [MANDATORY]

`**Confidence: XX%** Reason: …` — 90–100% = clean run; 70–89% = flakiness/env issues; 50–69% = partial; <50% = incomplete execution.

## Footer

`Agents involved: EnergoTS Playwright Test Runner`
