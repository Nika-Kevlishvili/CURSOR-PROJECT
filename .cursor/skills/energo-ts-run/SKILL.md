---
name: energo-ts-run
description: Runs specific Playwright tests from EnergoTS (local repo synced from GitHub) based on user prompt. Resolves which test to run (newly created, Jira key, file path, or domain) and executes npx playwright test. Use when the user asks to run a Playwright test, run a newly created test, or run a specific test from GitHub/EnergoTS.
---

# EnergoTS Playwright Test Run Skill

Use when the user wants to **run** (execute) Playwright tests from the local EnergoTS repo. Stay on the current allowed branch (`cursorMerged`, `staging`, or `cursor`). Do not switch branches to run tests (Rule ENERGOTS.0).

## When to Apply

- "Run newly created test", "run test REG-123", "run customer.spec.ts", "run all billing tests", "run Playwright from GitHub" (local clone).

## Workflow

1. **Branch** — in `Cursor-Project/EnergoTS/`, read `git rev-parse --abbrev-ref HEAD` and `playwright.config.ts` `testDir`. Stay put when HEAD is `cursorMerged` or `staging` and `testDir` is `./src/tests`. If HEAD is `cursor` and `testDir` is still `./tests`, stop and ask the user to use `cursorMerged` or `staging`. Do not checkout another branch yourself. If HEAD is not one of those three, stop and ask.
2. **Resolve target** (see table below). Paths are relative to `Cursor-Project/EnergoTS/`.
3. **Execute** from `Cursor-Project/EnergoTS/`: `npx playwright test <path|grep>`. API specs live under `src/tests/`. Frontend specs use `--project=frontend` and paths under `src/frontend/`. Visual regression uses `--config=tests/regression/playwright.regression.config.ts`.
4. **Report** pass/fail; optional Chat reports file on user request (Rule 0.6). JSON output is `src/backend/playwright-report.json`.

## Resolution table

| User prompt | Action |
|-------------|--------|
| Newly created / latest | Most recent `.spec.ts` under `EnergoTS/src/tests/` (mtime or git status); ask if ambiguous |
| Jira key (REG-123, PDT-456) | Grep `src/tests/` → `npx playwright test --grep "KEY"` or the matching file |
| File path / name | Normalize under `src/tests/` or `src/frontend/` and run |
| Domain (billing) | `npx playwright test src/tests/billing/` |
| Frontend / POM spec | `npx playwright test --project=frontend src/frontend/<file>.spec.ts` |
| Visual regression | `npx playwright test --config=tests/regression/playwright.regression.config.ts` |
| "From GitHub" | Clarify: run local clone; suggest sync if user wants latest remote |

## Setup (when fixtures missing)

From `EnergoTS/`: `npx playwright test --project=setup` (requires `.env` with PORTAL_USER, PASSWORD, DEVAUTHAPI or TESTAUTHAPI) → writes `src/backend/fixtures/token.json`, `src/backend/fixtures/envVariables.json`, and `src/backend/fixtures/salesPortalToken.json`. Frontend runs use `--project=frontend`, which also runs `frontend-env-setup.ts` and writes `src/frontend/fixtures/frontendEnvVariables.json`.

## Rule DPR.0 — detailed markdown

- **Scoped Slack path 2:** orchestrator runs `node ../config/playwright/generate-detailed-report.mjs` and Slack-uploads **`playwright-report-detailed.md`** — not this agent's default.
- **Ad-hoc:** generate + upload only when user **explicitly** asks. Smart report template: **`Playwright_run_detailed_report_template.md`**.

## Error handling

| Condition | Action |
|-----------|--------|
| Not on `cursor`, `staging`, or `cursorMerged` | Stop and ask. Do not checkout another branch |
| `testDir` is not `./src/tests` | Stop. That checkout is the pre-merge layout. Ask before switching to `cursorMerged` or `staging` |
| EnergoTS or Playwright missing | Report path / `npm install` hint |
| No matching test | "No matching test found" + how you searched |
| Tests fail | Report failures; do not fix code unless user asked |

## Constraints

- **No code modification** (Rule 0.8). Summarize in English (Rule 0.7).
- **Rule 0.3:** no Python IntegrationService in this workspace.

## Agent

`.cursor/agents/energo-ts-run.md` (I/O contract only).

## Confidence Score (Rule CONF.1 — Three-Zone) [MANDATORY]

`**Confidence: XX% (ZONE)**` with evidence factors. Zones: **GO** (≥ 85% — clean run), **CAUTION** (55–84% — flakiness/env issues + assumptions list), **STOP** (< 55% — incomplete execution, ask user). See `.cursor/rules/scoring/confidence_scoring_matrix.mdc`.

## Footer

`Agents involved: EnergoTS Playwright Test Runner`
