---
description: Author and run EnergoTS frontend Playwright specs for a merged phoenix-ui change. Stays on cursorMerged. Updates the page object, writes specs from the frontend Playwright instructions file, takes one validator critique, then executes.
---

# /playwright-frontend

**Usage:** `/playwright-frontend <JIRA_KEY or Topic> [environment]`

Examples:

```text
/playwright-frontend PDT-2971 dev
/playwright-frontend Customer_disconnections_tab test
```

## What this command does

1. Confirms `Cursor-Project/EnergoTS` is already on **`cursorMerged`**. If it is not, stop and say so. Do not checkout or switch.
2. Resolves `Cursor-Project/test_cases/Frontend/<Topic>.md` and `Cursor-Project/test_cases/Frontend/<Topic>.playwright.md`.
3. If the environment is missing, ask (dev, dev2, test, preprod, prod, experiments) and stop until it is named.
4. If the Playwright instructions file is missing, stop. Generate frontend cases first (test-case-generator with Backend+Frontend). That generator writes the instructions file. Do not invent the instructions inside this command.
5. Invokes **playwright-frontend** (`.cursor/agents/playwright-frontend.md`), which:
   - inspects `Cursor-Project/Phoenix/phoenix-ui` on remote branches without checking out;
   - updates the existing page object, using `playwright-cli` only when the merged UI added controls the POM does not expose;
   - writes `src/frontend` specs in the `meterCreate.spec.ts` shape;
   - takes **one** critique from **playwright-frontend-validator**;
   - applies those fixes;
   - runs `npx playwright test --project=frontend` and reports pass, fail, product code defect, and automation defect.

## What it does not do

- Does not checkout EnergoTS or phoenix-ui.
- Does not author API specs under `src/tests/` (that is energo-ts-test).
- Does not run the validator twice.
- Does not write Phoenix code.

## References

- Procedure: `.cursor/skills/playwright-frontend/SKILL.md`
- Assessor: `.cursor/skills/playwright-frontend-validator/SKILL.md`
- Instructions file: test-case-generator SKILL § Frontend Playwright instructions
