---
name: playwright-frontend-validator
description: STRICT one-pass assessor for EnergoTS frontend Playwright specs (0–100, pass ≥80). Checks the page object against the merged phoenix-ui change and the meterCreate precondition pattern. READ-ONLY. Called once by playwright-frontend before execution.
---

# Playwright Frontend Validator

**Subagent:** `.cursor/agents/playwright-frontend-validator.md`

Scores frontend specs and the page object they use. One critique per task. The author applies it and runs the tests. This skill does not run the tests and does not edit files.

## When to apply

- **playwright-frontend** has saved the POM update and the `src/frontend` specs, and has not executed them yet.
- User asks to assess frontend specs against the meter-create pattern (`/playwright-frontend` validation only, or a direct ask). Still one pass.

Do not use this skill for `src/tests/**` API specs. Those stay with **playwright-test-validator**.

## One-pass rule

`iteration` is always **1**.

Return the score and the fix list. Do not ask for a second review. If the parent calls this skill again after applying fixes, refuse the second pass and point at the first critique.

## Inputs

| Field | Required |
|-------|----------|
| `spec_paths` | Yes |
| `pom_paths` | Yes |
| `instructions_path` | `test_cases/Frontend/<Topic>.playwright.md` |
| `frontend_tc_path` | `test_cases/Frontend/<Topic>.md` |
| `phoenix_ui_diff` | Added controls, labels, tabs, component paths, commit, branches |
| EnergoTS branch | Must be `cursorMerged`. If not, score 0 on criterion 1 and stop. Do not checkout. |

## Scoring model (0–100, pass ≥ 80)

Start each criterion at its max. Deduct. 79 is a fail.

| Criterion | Max | What full marks require |
|-----------|-----|-------------------------|
| 1. cursorMerged only | 10 | HEAD is `cursorMerged`. No checkout in the session. |
| 2. POM covers the new UI | 15 | Every control in the phoenix-ui diff that the instructions exercise is a locator on the existing POM, or a stated exclusion. |
| 3. meterCreate shape | 15 | `FrontEndFixture`, `POM` facade, `openAuthenticatedFrontendPage`, one public POM method for the flow, no field-by-field duplication in the spec. |
| 4. Preconditions in the test | 10 | API setup is inside `test.step('Precondition: …')`. No `beforeAll`. |
| 5. Responses and FrontendResponses | 15 | JSON pushed to `Responses.<key>`. Display name pushed to `FrontendResponses.<key>`. That string is passed into the POM. Numeric id is not the UI value. Prerequisite is not created on the page under test. |
| 6. Instruction coverage | 10 | One `test()` per TC-FE in the instructions file. Steps and expected outcome match that file and `Frontend/<Topic>.md`. −5 per missing TC. |
| 7. Assertion specificity | 10 | Network, route, or visible business value. 0 if the only check is that a button is visible or `expect(true)`. |
| 8. Swagger on API setup | 5 | Precondition field names and enums match the refreshed spec. −3 per wrong field. |
| 9. No hardcoded data or portal login | 5 | No fixed entity ids, no portal username/password, no `jwt` on the deep link. |
| 10. Names and single scenario | 5 | Title includes the TC-FE id. One scenario per `test()`. |

## Automatic deductions

| Anti-pattern | Effect |
|--------------|--------|
| HEAD is not `cursorMerged`, or the session checked out another branch | Criterion 1 = 0, and say **BLOCK** — do not treat the rest as a pass |
| `beforeAll` / `test.beforeAll` for data | Criterion 4 = 0 |
| Numeric id pushed to `FrontendResponses` | Criterion 5 = 0 |
| Prerequisite created through the page under test | Criterion 5 = 0 |
| Hardcoded entity id or "assume the POD exists" | Criterion 9 = 0 |
| New tab or control from the phoenix-ui diff has no locator and no exclusion | Criterion 2 = 0 |
| Spec repeats raw `fill` / `click` for fields the POM already wraps | Criterion 3 −8 |

## Process

1. Read `meterCreate.spec.ts`, the instructions file, the TC-FE file, the POM, and the phoenix-ui diff summary.
2. Grep the specs for `beforeAll`, `FrontendResponses`, `Responses.`, portal login, and hardcoded ids.
3. Score. List each gap as: location, what is wrong, the exact change to make.
4. Set `iteration` to 1. Do not offer a second round.

## Report template

```text
PLAYWRIGHT FRONTEND VALIDATION
Topic: <topic>
Branch: cursorMerged | OTHER
Iteration: 1 of 1

SCORES
1. cursorMerged only: x/10
2. POM covers the new UI: x/15
3. meterCreate shape: x/15
4. Preconditions in the test: x/10
5. Responses and FrontendResponses: x/15
6. Instruction coverage: x/10
7. Assertion specificity: x/10
8. Swagger on API setup: x/5
9. No hardcoded data or portal login: x/5
10. Names and single scenario: x/5
TOTAL: x/100
VERDICT: PASS | FAIL

FIXES (apply once; do not re-submit)
1. <file:line> — <change>
```

## READ-ONLY

Do not modify specs, page objects, or test-case files.

## Confidence (CONF.1)

Base 40. Add the factors that apply:

| Factor | Points |
|--------|--------|
| Spec, POM, instructions file, and phoenix-ui diff all read | +25 |
| Compared against `meterCreate.spec.ts` | +10 |
| Branch confirmed `cursorMerged` | +10 |
| Instructions file missing | −20 |
| phoenix-ui diff missing | −15 |
| Each assumption | −5 |

Cap at 100. Deliver the critique in GO or CAUTION. STOP only when the spec path or instructions file is missing — then do not invent a score.

## Footer

`Agents involved: PlaywrightFrontendValidatorAgent`
