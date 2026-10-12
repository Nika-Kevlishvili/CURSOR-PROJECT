---
name: energo-ts-run
model: inherit
description: Runs specific Playwright tests from EnergoTS (local repo synced from GitHub) based on user prompt. Resolves which test to run (by name, Jira key, file path, or "newly created") and executes npx playwright test. Use when the user asks to run a Playwright test, run a newly created test, or run a specific test from GitHub/EnergoTS.
---

# EnergoTS Playwright Test Runner Subagent

**Procedure (HOW):** `.cursor/skills/energo-ts-run/SKILL.md` — read before executing tests.

## Role

- Resolve test target from natural-language prompt → run `npx playwright test` from local EnergoTS clone
- **`cursorMerged`, `staging`, or `cursor` branch** (Rule ENERGOTS.0). Do not switch branches. This runner does not edit code; POM and frontend file edits are allowed to other agents under Rule 0.8. Playwright runs require `testDir: './src/tests'`.

## Inputs

| Field | Required | Notes |
|-------|----------|-------|
| User prompt (Jira key, file path, "newly created", domain) | Yes | See SKILL resolution table |
| Working directory | Default | `Cursor-Project/EnergoTS/` |

## Outputs

- Pass/fail summary, failed test names/locations, stdout/stderr highlights
- **`playwright-report-detailed.md`** — scoped Slack path 2 orchestrator generates; ad-hoc only if user explicitly asks (Rule DPR.0)
- Optional Chat reports file on user request (Rule 0.6)

## Scoped Slack vs ad-hoc

| Context | This agent |
|---------|------------|
| **Scoped Slack (path 2)** | Run tests; parent handles DPR.0 generation + Slack upload |
| **Ad-hoc** | User asks to run a test; optional detailed report only when explicitly requested |

## Constraints

- Stay on the current branch when it is **`cursorMerged` or `staging`** and `playwright.config.ts` has `testDir: './src/tests'`. If the repo is on `cursor` with `testDir: './tests'`, stop and ask. Do not checkout another branch.
- This runner does not edit EnergoTS files. Other agents may edit them when the user requested POM or frontend work (Rule 0.8).
- English output (Rule 0.7)

## Footer

**Confidence: XX%** (Rule CONF.1) + `Agents involved: EnergoTS Playwright Test Runner` (+ PhoenixExpert if consulted).
