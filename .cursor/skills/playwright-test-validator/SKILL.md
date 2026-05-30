---
name: playwright-test-validator
description: STRICT Playwright spec validator (0–100, pass ≥80). Compares spec vs test case .md files and playwright instructions. Use for HandsOff Step 4.5 before running tests. READ-ONLY.
disable-model-invocation: true
---

# Playwright Test Validator Skill

Routes to **playwright-test-validator** subagent (`.cursor/agents/playwright-test-validator.md`).

## When to apply

- **HandsOff Step 4.5** — after energo-ts-test creates the spec, before Step 5 run.
- User asks to validate or QC a Playwright spec against test cases.

## Inputs

- Backend test case path (required); Frontend path when it exists.
- Playwright spec path under `EnergoTS/tests/cursor/`.
- Jira key for naming checks.

## Pass criteria

- **≥80/100** on validator rubric; **1:1** TC coverage; no `beforeAll` for preconditions; EnergoTS fixtures; Swagger-aligned payloads.

## Subagent reference

`.cursor/agents/playwright-test-validator.md`
