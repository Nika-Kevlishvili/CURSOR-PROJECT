# PDT-2599 — Playwright local run report (ad-hoc, no Slack)

**Jira:** PDT-2599  
**Title:** (not fetched from Jira this run — fill from ticket if needed)  
**Date (UTC):** 2026-05-03  
**Environment:** Local EnergoTS run; API base from log: `http://10.236.20.11:8091/`; frontend: `http://10.236.20.11:8080/`  
**Spec file(s):** `Cursor-Project/EnergoTS/tests/cursor/PDT-2599-be-service-contract-version.spec.ts`  
**Playwright command:** `npx playwright test --grep "PDT-2599"` (branch: `cursor`)  
**Totals:** 23 passed, 3 failed, 6 did not run (of 32 scheduled)  
**Slack:** **Not sent** — per user request (report files only on disk).

---

## Machine detailed report (JSON → Markdown)

**Path:** `Cursor-Project/EnergoTS/playwright-report-detailed.md` (regenerate with `node ../config/playwright/generate-detailed-report.mjs` from `EnergoTS/`)  
**Source:** `Cursor-Project/EnergoTS/playwright-report.json` after this run.

---

## Failed tests (summary)

All three failures share the same precondition error in **`pdt-2599-service-contract.fixtures.ts`** (billing chain):

| # | Playwright title (excerpt) | Error / first line |
|---|-----------------------------|---------------------|
| 1 | `[PDT-2599] TC-BE-24: PER_PIECE — first Signed window — invoices issued …` | `PDT-2599 billing: need formula from third tab and/or price components (settlement + billing) before POST /service-contract` — `expect(formula).toBeTruthy()` received `null` at `pdt-2599-service-contract.fixtures.ts:1206` |
| 2 | `[PDT-2599] TC-BE-27: OVER_TIME_ONE_TIME — first Signed window` | Same precondition (`fixtures.ts:1206`) |
| 3 | `[PDT-2599] TC-BE-30: OVER_TIME_PERIODICAL — first Signed window` | Same precondition (`fixtures.ts:1206`) |

**Interpretation:** Billing-related tests require a **formula** from the service contract third tab / price components before POST `/service-contract`; the fixture chain did not obtain a non-null `formula` in this environment.

---

## Did not run

Six tests did not complete as **passed** in the run output (Playwright reported **6 did not run** — typically skipped or not reached in the same run batch; see `playwright-report-detailed.md` for per-test status).

---

## Next steps (optional)

1. Fix or seed billing third-tab / price component data so `runPdt2599SignedServiceContractBillingChain` resolves `formula`, then re-run `--grep "PDT-2599"`.  
2. When ready for Slack: use **`Slack_report_summary_short_template.md`** + upload **`playwright-report-detailed.md`** and this (or HandsOff `{JIRA_KEY}.md`) per **`hands-off.md`** / path 3 — user asked to defer Slack.

---

## References

- Smart report structure template: `Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md`  
- Detailed reporting rule: `.cursor/rules/workflows/playwright_detailed_reporting.mdc`  
