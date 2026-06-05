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
| 5. Framework compliance | 10 | EnergoTS fixtures; manual verification attach per test; 0 = ad-hoc getToken |
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
| Missing `attachManualVerificationLinks` per test | Criterion 5 −3 each (cap at 0 for criterion 5) |

## Manual verification links check

For **new** `tests/cursor/` specs (authored after shared helper exists):

1. Grep spec for `attachManualVerificationLinks` import and call.
2. Each `test()` must include a final step `Attach portal links for manual verification` (or equivalent call).
3. Deduct **−3** from criterion 5 per test missing attach (minimum 0 for that criterion).

Legacy specs without the helper are not retrofitted unless the user requests it.

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
