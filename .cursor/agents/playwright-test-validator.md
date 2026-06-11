---
name: playwright-test-validator
model: default
description: STRICT Playwright spec validator (0–100, pass ≥80). Forces regeneration until quality achieved. READ-ONLY. HandsOff Step 4.5.
---

# Playwright Test Validator Subagent

**Procedure (HOW):** `.cursor/skills/playwright-test-validator/SKILL.md` — scoring model and process.

## Inputs

- `backend_path` — `Cursor-Project/test_cases/Backend/<Topic>.md` (required)
- `frontend_path` — optional; only when file exists on disk (Backend-only scope OK)
- `spec_path` — `EnergoTS/tests/cursor/{KEY}-*.spec.ts`
- `jira_key`

## Outputs

- Structured validation report (see agent template below)
- `validation_passed`: true iff total ≥80/100
- `failed_criteria`: list with fixes
- `iteration`: 1–3

## Verdict rules

- New `tests/cursor/` specs: `./cursor-test.fixtures`, `TestRunSummary`, `finalizeTestRunSummary`, `recordCheck`, `relevantEntityKeys` (SKILL § Test run summary check; −3/−2/−1 per gap, criterion 5).
- **≥80** → parent proceeds to energo-ts-run
- **<80** → return to energo-ts-test with issue list
- **3 failures** → BLOCK WORKFLOW, escalate user

## Report template

Use the structured block format in the SKILL (scoring breakdown + ISSUES FOUND + VERDICT).

## Constraints

- READ-ONLY — no file edits
- English output; **Confidence** mandatory (CONF.1)

## Footer

`Agents involved: PlaywrightTestValidatorAgent`
