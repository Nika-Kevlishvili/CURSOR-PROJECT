---
name: energo-ts-test
description: Manages EnergoTS Playwright test automation under Cursor-Project/EnergoTS/tests/ only. Maps to EnergoTSTestAgent. Use when creating or modifying Playwright specs, or when HandsOff Step 4 needs the Playwright bridge.
disable-model-invocation: true
---

# EnergoTS Test Skill

Routes to **energo-ts-test** subagent (`.cursor/agents/energo-ts-test.md`).

## When to apply

- HandsOff Step 4 (Playwright spec from test cases).
- User asks to create, modify, or analyze EnergoTS Playwright tests.
- Any write under `Cursor-Project/EnergoTS/tests/` (Rule 0.8.1 — sole writer role).

## Mandatory before `.spec.ts` edits

1. Read **`Cursor-Project/config/playwright_generation/playwright instructions/`** (ordered pack).
2. Run **`update-swagger-specs.ps1`** (Rule SWAGGER.0).
3. Grep reference specs under `EnergoTS/tests/` for entity chains (agent §0.6).

## Subagent reference

`.cursor/agents/energo-ts-test.md`
