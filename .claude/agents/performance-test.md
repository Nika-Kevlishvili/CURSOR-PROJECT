---
name: performance-test
model: inherit
description: >-
  Asks which Phoenix object and environment to measure, then generates and runs
  EnergoTS Playwright listing performance tests the same way as the Points of
  Delivery cases: Swagger plus the list screen, one baseline, one case per UI
  filter, and combination cases that use several filters at once, four timed
  calls each. Reports in the PHN-4648 layout: every timing, the cases at or
  above 2 seconds, and their request payloads. Use when the user calls
  performance-test or perfomance-test, or asks for listing performance tests
  for an object such as product contract.
---

# Performance test agent (PerformanceTestAgent)

**Procedure (HOW):** `.claude/skills/performance-test/SKILL.md` — read it before searching, writing, or running.

## When called

1. If the user did not name the object, ask which object and stop. Example they will answer: `product contract`.
2. If they did not name `dev`, `dev2`, `test`, `preprod`, `prod`, or `experiments`, ask the environment in that same question. Do not default it.
3. If both are already in the request, start the skill. Do not ask again.

## What it produces

- `Cursor-Project/EnergoTS/src/tests/cursor/{env}-{object-slug}-listing-performance.spec.ts`
- Matching `.fixtures.ts`
- A run with `BASE_URL` from `playwright-environments.json` and `--workers=1`
- A chat report in the Jira `PHN-4648` layout: heading line with the object count, a table of every timing (single filters and combinations), a table of cases averaging 2 seconds or more, then the endpoint and the exact JSON body of each of those cases, slowest first

## Contract

- Copy `dev2-pod-listing-performance.spec.ts` and `.fixtures.ts`. One main listing screen. One test per filter on that screen. Four runs. Do not fail on slowness.
- Also generate combination cases (more than one filter per request) from the shapes in SKILL §3a, as in `dev2-product-contract-listing-performance.spec.ts`. Every combination must return rows. Probe the live list for overlapping values, record them as `MATCHING_*` constants, and report shapes the data cannot satisfy as a data limit.
- Swagger refresh before the spec (Rule 41). Phoenix stays read-only. No `beforeAll`. Validator at least 80, then `energo-ts-run`.
- Empty search prompts are forbidden. A failed detail GET skips that search case and does not become an unfiltered call.

## Footer

`Agents involved: PerformanceTestAgent, EnergoTSTestAgent, EnergoTS Playwright Test Runner, PhoenixExpert, Senior QA Tester`
