---
name: hands-off-playwright-report
description: HandsOff reporting and quality lessons (Rule 37). TC on disk, Step 3.5/4.5 gates, energo-ts-test, Slack paths, DPR.0 machine report. Canonical detail for handsoff_playwright_report.mdc.
---

# HandsOff Playwright Report Skill

**Orchestration steps:** `.cursor/commands/hands-off.md`  
**Machine report (DPR.0):** `.cursor/rules/workflows/playwright_detailed_reporting.mdc`

## 1. Test cases — on disk, template, coverage

- Verify **`test_cases/Backend/<Topic>.md`** exists always; **Frontend** only when TC-FRONTEND scope = Yes.
- **Coverage:** exhaustive positive/negative/edge/regression — see **test-case-generator/SKILL.md** §5.
- **STANDALONE:** per-TC full preconditions — **`test_cases_structure.mdc`** § TC-STANDALONE-PRE.0, **`Test_case_template.md`**.
- **Playwright instructions pack** before writing `.md` — see **test-case-generator/SKILL.md**.
- Do not trust "subagent created it" without disk verification.

## 2. Playwright spec — energo-ts-test only

- Spec via **energo-ts-test/SKILL.md** — Swagger refresh (Rule 41), no `beforeAll` (Rule 40), 1:1 TC coverage.
- Path: `EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`; **cursor** branch only.

## 2a. Step 4.5 — playwright-test-validator

- **≥80/100**; max 3 iterations; **BLOCK** before run unless user opts out.
- Procedure: **playwright-test-validator/SKILL.md**

## 2b. Precondition data creation

- Create all entities — no query-existing-data, no hardcoded IDs.
- Helper functions + `test.step('Precondition: …')` — **`precondition-data-creation.instructions.md`**.

## 3. Reports

- Smart: **`Playwright_run_detailed_report_template.md`** → `{JIRA_KEY}.md` under HandsOff reports.
- Machine: **`generate-detailed-report.mjs`** → `playwright-report-detailed.md` when JSON exists.

## 4. Slack (path 2)

- **Three-block** text only — **`Slack_report_summary_short_template.md`**
- **Upload** both `.md` files to Tester (`customfield_10095` only) + **#ai-report** (`C0AK96S1D7X`) — never Assignee.
- Index: **`Slack_reporting_paths.md`**

## 5–6. Run tests + fixtures

- Global setup when token/env missing — see **energo-ts-run/SKILL.md**.
- Document run command in report if local run blocked.

## 7. Agent questions after report

- `[AgentName]: question` follow-up after report to same Slack recipients.

## Summary checklist

- [ ] Swagger refresh (Rule 41)
- [ ] TC files on disk + README updates
- [ ] Step 3.5 TC quality ≥80 (BLOCK if fail after 3)
- [ ] Spec 1:1 TC count; no beforeAll preconditions
- [ ] Step 4.5 spec validation pass (BLOCK if fail after 3)
- [ ] Smart + machine reports; Slack uploads
- [ ] Agent questions with attribution
