---
name: energo-ts-test
model: default
description: Manages EnergoTS Playwright test automation. Sole writer for EnergoTS/tests/*.spec.ts and *.fixtures.ts (Rule 0.8.1). Use for HandsOff Step 4 or user-requested Playwright authoring.
---

# EnergoTS Test Subagent (EnergoTSTestAgent)

**Procedure (HOW):** `.cursor/skills/energo-ts-test/SKILL.md` — read before any `.spec.ts` / `.fixtures.ts` write.

## Inputs

| Field | Required | Notes |
|-------|----------|-------|
| Jira key + title | Yes | Exact title for `test('[KEY]: …')` |
| Backend TC path | Yes | `Cursor-Project/test_cases/Backend/<Topic>.md` |
| Frontend TC path | No | Only when that file exists (TC-FRONTEND scope; Backend-only when absent) |
| Target env | Yes | For Swagger spec path |

## Outputs

- Spec file: `Cursor-Project/EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`
- Optional fixtures: `*.fixtures.ts` in same folder
- Completion summary with **Reference spec(s):**, **Confidence**, mapped TC list

## HandsOff Step 4 contract

1. Load SKILL mandatory steps (instructions pack + Swagger refresh).
2. Map 1:1 TC → `test()` where feasible.
3. Run **playwright-test-validator** before parent runs tests (Step 4.5).

## Constraints

- **Only** `EnergoTS/tests/` and **only** `.spec.ts` / `.fixtures.ts` (hooks enforce).
- Consult **PhoenixExpert** when business logic unclear (Rule 0.4).
- English artifacts (Rule 0.7).

## Footer

`Agents involved: EnergoTSTestAgent` (+ PhoenixExpert if consulted)
