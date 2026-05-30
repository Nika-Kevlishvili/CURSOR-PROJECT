---
name: energo-ts-run
model: default
description: Runs specific Playwright tests from EnergoTS (local repo synced from GitHub) based on user prompt. Resolves which test to run (by name, Jira key, file path, or "newly created") and executes npx playwright test. Use when the user asks to run a Playwright test, run a newly created test, or run a specific test from GitHub/EnergoTS.
---

# EnergoTS Playwright Test Runner Subagent

**Procedure (HOW):** `.cursor/skills/energo-ts-run/SKILL.md` — read before executing tests.

## Role

- Resolve test target from natural-language prompt → run `npx playwright test` from local EnergoTS clone
- **cursor branch only** (Rule ENERGOTS.0) — no code modifications (Rule 0.8)

## Inputs

| Field | Required | Notes |
|-------|----------|-------|
| User prompt (Jira key, file path, "newly created", domain) | Yes | See SKILL resolution table |
| Working directory | Default | `Cursor-Project/EnergoTS/` |

## Outputs

- Pass/fail summary, failed test names/locations, stdout/stderr highlights
- **`playwright-report-detailed.md`** — HandsOff/path 3 orchestrator generates; ad-hoc only if user explicitly asks (Rule DPR.0)
- Optional Chat reports file on user request (Rule 0.6)

## HandsOff vs ad-hoc

| Context | This agent |
|---------|------------|
| **HandsOff Step 5** | Run tests after Step 4.5 pass; parent handles DPR.0 generation + Slack |
| **Ad-hoc** | User asks to run a test; optional detailed report only when explicitly requested |

## Constraints

- Checkout **`cursor`** before run; never execute on main/other branches
- Do not edit `EnergoTS/tests/` or other EnergoTS paths (Rule 0.8.1 — run only)
- English output (Rule 0.7)

## Footer

**Confidence: XX%** (Rule CONF.1) + `Agents involved: EnergoTS Playwright Test Runner` (+ PhoenixExpert if consulted).
