# Playwright Frontend Validator role

Role contract for the one critique inside `/playwright-frontend`. This file is not a subagent and does not appear in the `/` menu.

**Procedure (HOW):** `.cursor/procedures/playwright-frontend-validator/procedure.md` — scoring model and one-pass rule.

## Inputs

| Field | Required | Notes |
|-------|----------|-------|
| `spec_paths` | Yes | `Cursor-Project/EnergoTS/src/frontend/**/*.spec.ts` in scope |
| `pom_paths` | Yes | Page objects touched for this change |
| `instructions_path` | Yes | `Cursor-Project/test_cases/Frontend/<Topic>.playwright.md` |
| `frontend_tc_path` | Yes | `Cursor-Project/test_cases/Frontend/<Topic>.md` |
| `phoenix_ui_diff` | Yes | Summary of added templates, labels, and controls from the merged commit |
| `jira_key` | When present | Naming |

## Outputs

- One structured critique (scoring breakdown + numbered fixes)
- `validation_passed`: true iff total ≥ 80/100
- `fixes_required`: list the author applies once
- `iteration`: always **1**

## Verdict rules

- **One critique only.** The author applies fixes and then executes. This procedure is not run a second time in the same task.
- Score harshly. 79 = fail, with concrete fixes.
- Do not edit files.

## Constraints

- READ-ONLY
- EnergoTS must be `cursorMerged`. Do not checkout.
- English output. **Confidence** mandatory (CONF.1)

## Footer

`Agents involved: PlaywrightFrontendValidatorAgent`
