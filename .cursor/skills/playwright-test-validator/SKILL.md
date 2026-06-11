---
name: playwright-test-validator
description: STRICT Playwright spec validator (0–100, pass ≥80). Compares spec vs test case .md files and playwright instructions. HandsOff Step 4.5 before run. READ-ONLY.
---

# Playwright Test Validator Skill

**Subagent (report format):** `.cursor/agents/playwright-test-validator.md`

## When to apply

- **HandsOff Step 4.5** — after energo-ts-test creates spec, before Step 5 run.
- User asks to validate a Playwright spec against test cases.

## Inputs

- Backend TC path (required); Frontend path when it exists on disk.
- Spec path: `EnergoTS/tests/cursor/{KEY}-*.spec.ts`
- Jira key for naming checks.

**Coverage:** Expected `test()` count = TC count in **provided** `.md` files only. Do not require Frontend when no Frontend file.

## Scoring model (0–100, pass ≥80)

| Criterion | Max | Scoring |
|-----------|-----|---------|
| 1. Syntax correctness | 10 | 10 valid TS; 0 won't compile |
| 2. Coverage completeness | 15 | 15 = 1:1 TC:test(); −5 per missing |
| 3. TC-to-test alignment | 15 | Steps + expected match TC |
| 4. Assertion specificity | 15 | Status + body fields; 0 = expect(true) only |
| 5. Framework compliance | 10 | cursor-test.fixtures + TestRunSummary + finalize; 0 = ad-hoc getToken |
| 6. Hook ban (beforeAll) | 10 | 0 if any beforeAll for preconditions |
| 7. Precondition data creation | 10 | 0 = hardcoded IDs / assume exists |
| 8. Entity creation order | 5 | Matches precondition-data-creation.instructions.md |
| 9. Swagger compliance | 5 | Field names/enums from refreshed spec |
| 10. Test naming & structure | 5 | `[JIRA-KEY]`, TC ref in title |

## Automatic deductions

| Anti-pattern | Deduction |
|--------------|-----------|
| `beforeAll` for preconditions | Criterion 6 → 0 |
| Hardcoded ID / query existing row | Criterion 7 → 0 |
| Missing test for TC | Criterion 2 −5 each |
| `expect(true)` / no expect | Criterion 4 → 0 |
| `toBeOK()` only | Criterion 4 −10 |
| Ad-hoc getToken/apiRequest | Criterion 5 → 0 |
| Wrong Swagger field/enum | Criterion 9 −3 each |
| New cursor spec uses `baseFixture` instead of `./cursor-test.fixtures` | Criterion 5 −3 |
| Manual `reportGenerator` attach / extra API `afterEach` in new cursor spec | Criterion 5 −3 |

## Test run summary check (new tests/cursor/ specs)

Per **energo-ts-test SKILL § Manual verification links**. Specs on `baseFixture` only — legacy; not retrofitted unless user requests.

1. Import `./cursor-test.fixtures`; destructure `TestRunSummary` in each `test()`.
2. End each `test()` with `finalizeTestRunSummary` (or `attachManualVerificationLinks` + `testRunSummary`).
3. `TestRunSummary.registerPayload` for entities that drive pass/fail.
4. `TestRunSummary.recordCheck` with `expectedResult`, `actualResult`, `passed` for main verifications.
5. `relevantEntityKeys` in finalize options — portal links filtered to test-relevant buckets.
6. Final step title: `Attach test run summary` or equivalent.

| Gap | Deduction (criterion 5, min 0) |
|-----|----------------------------------|
| Missing finalize step per test | −3 each |
| No `TestRunSummary` fixture usage | −3 |
| No `registerPayload` for driving entities | −2 |
| No `recordCheck` with narrative expected/actual | −2 |
| Missing `relevantEntityKeys` | −1 |

## cursor-test fixtures check (new tests/cursor/ specs)

1. Import from `./cursor-test.fixtures`, not `../../fixtures/baseFixture`.
2. No duplicate manual API-response `afterEach` or `reportGenerator.setLinksToResponses` attach.
3. Deduct **−3** from criterion 5 per violation (minimum 0 for that criterion).

## Process

1. Read playwright instructions pack (at least test-writing-rules, SKILL, general-rules, precondition-data-creation).
2. Read TC file(s); extract every TC with steps + expected.
3. Read spec; count tests, grep forbidden patterns.
4. Score each criterion harshly (start at max, deduct).
5. Total ≥80 = PASS; else FAIL with numbered issues (location + fix).
6. Max **3** regeneration loops via energo-ts-test; then **BLOCK** and escalate.

## READ-ONLY

Do not modify spec or TC files.

## Footer

Structured report + **Confidence** + `Agents involved: PlaywrightTestValidatorAgent`
