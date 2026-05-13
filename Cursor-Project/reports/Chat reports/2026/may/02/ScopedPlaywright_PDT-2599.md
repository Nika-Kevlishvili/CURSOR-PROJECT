# Playwright run — detailed report (scoped)

| Field | Value |
|--------|--------|
| **Jira** | PDT-2599 |
| **Title** | Service Contract versioning – lifecycle & billing resolution (from `test_cases/Backend/Service_Contract_Versioning_PDT_2599.md`; Jira MCP search did not return issue ARI in this session) |
| **Date** | 2026-05-02 |
| **Environment / BASE_URL** | `http://10.236.20.11:8091/` (dev) |
| **Spec file** | `Cursor-Project/EnergoTS/tests/cursor/PDT-2599-be-service-contract-version.spec.ts` |
| **Test case file** | `Cursor-Project/test_cases/Backend/Service_Contract_Versioning_PDT_2599.md` |
| **Playwright command** | `npx playwright test tests/cursor/PDT-2599-be-service-contract-version.spec.ts` (full config: setup + main + send report; workers=6) |
| **Totals** | **22 passed**, **4 failed**, **6 skipped** (serial billing suites aborted after first failure in each block) |

---

## Per-test results

### Test 1: `[PDT-2599] TC-BE-1: BE - Service Contract - The system doesn't consider the contract version status`

| Field | Content |
|--------|---------|
| **Playwright title** | `[PDT-2599] TC-BE-1: BE - Service Contract - The system doesn't consider the contract version status` |
| **Covers test case(s)** | TC-BE-1 |
| **Created entities / links** | Portal preview URL in run output: `http://10.236.20.11:8080/service-contracts/preview?id=2349` (FRONTEND_BASE_URL resolved) |
| **Expected** | Per TC: version status handling / API behaviour as defined in test case doc |
| **Actual** | Test **passed** (~28.5s) |
| **Meets expectation** | **Yes** |

### Test 2: `[PDT-2599] TC-BE-2: POST /service-contract/list — created contract appears when searching by contract number`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-2 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2351` |
| **Actual** | **Passed** (~7.2s) |
| **Meets expectation** | **Yes** |

### Test 3: `[PDT-2599] TC-BE-3: GET /service-contract/{id} — non-existent id returns 400 or 404`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-3 |
| **Created entities / links** | No portal URL in stdout (no serviceContract link); API-only check |
| **Actual** | **Passed** (~212ms) |
| **Meets expectation** | **Yes** |

### Test 4: `[PDT-2599] TC-BE-4: GET /service-contract/{id} — invalid versionId returns error`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-4 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2352` |
| **Actual** | **Passed** (~19.8s) |
| **Meets expectation** | **Yes** |

### Test 5: `[PDT-2599] TC-BE-5: GET /service-contract/{id} — first version row has versionId 1`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-5 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2353` |
| **Actual** | **Passed** (~24.1s) |
| **Meets expectation** | **Yes** |

### Test 6: `[PDT-2599] TC-BE-6: GET /service-contract/third-tab-fields — missing serviceDetailId rejected`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-6 |
| **Created entities / links** | No portal URL built |
| **Actual** | **Passed** (~191ms) |
| **Meets expectation** | **Yes** |

### Test 7: `[PDT-2599] TC-BE-7: duplicate Valid start date on new version → 4xx (EN message)`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-7 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2348` |
| **Actual** | **Passed** (~11.5s) |
| **Meets expectation** | **Yes** |

### Test 8: `[PDT-2599] TC-BE-8: three Signed versions — end dates = day before next start`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-8 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2350` |
| **Actual** | **Passed** (~16.3s) |
| **Meets expectation** | **Yes** |

### Test 9: `[PDT-2599] TC-BE-9: insert Signed between two Signed — neighbour end dates recalc`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-9 |
| **Created entities / links** | (run exceeded timeout before stable preview line in truncated log) |
| **Expected** | Chain recalculation after middle Signed insert |
| **Actual** | **Failed** — `Test timeout of 60000ms exceeded` |
| **Meets expectation** | **No** |

### Test 10: `[PDT-2599] TC-BE-10: versionId ordinals 1..n after out-of-order chronological inserts`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-10 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2355` |
| **Actual** | **Passed** (~30.6s) |
| **Meets expectation** | **Yes** |

### Test 11: `[PDT-2599] TC-BE-11: add non-first Draft — distinct start, open end`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-11 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2356` |
| **Actual** | **Passed** (~16.7s) |
| **Meets expectation** | **Yes** |

### Test 12: `[PDT-2599] TC-BE-12: Draft duplicate start date → 4xx`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-12 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2357` |
| **Actual** | **Passed** (~17.5s) |
| **Meets expectation** | **Yes** |

### Test 13: `[PDT-2599] TC-BE-13: second Signed start earlier than first → 4xx`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-13 |
| **Actual** | **Passed** |
| **Meets expectation** | **Yes** |

### Test 14: `[PDT-2599] TC-BE-14: edit middle Signed start — chain stays coherent`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-14 |
| **Actual** | **Passed** |
| **Meets expectation** | **Yes** |

### Test 15: `[PDT-2599] TC-BE-15: first Signed cannot change start date (in-place PUT)`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-15 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2360` |
| **Actual** | **Passed** (~12.6s) |
| **Meets expectation** | **Yes** |

### Test 16: `[PDT-2599] TC-BE-16: status-update v1 to CANCELLED rejected`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-16 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2361` |
| **Actual** | **Passed** (~13.0s) |
| **Meets expectation** | **Yes** |

### Test 17: `[PDT-2599] TC-BE-17: non-first Draft → READY via status-update (when chain allows)`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-17 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2362` |
| **Actual** | **Passed** (~51.2s) |
| **Meets expectation** | **Yes** |

### Test 18: `[PDT-2599] TC-BE-18: GET versions — Signed group sorted by start; new version ids monotonic`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-18 |
| **Actual** | **Passed** |
| **Meets expectation** | **Yes** |

### Test 19: `[PDT-2599] TC-BE-19: validation error surfaces message text (sample from duplicate-date)`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-19 |
| **Actual** | **Passed** |
| **Meets expectation** | **Yes** |

### Test 20: `[PDT-2599] TC-BE-20: after Signed date edit, Draft row stays open-ended`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-20 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2365` |
| **Actual** | **Passed** (~30.4s) |
| **Meets expectation** | **Yes** |

### Test 21: `[PDT-2599] TC-BE-21: PUT with unknown versionId → 4xx, GET unchanged`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-21 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2366` |
| **Actual** | **Passed** (~29.0s) |
| **Meets expectation** | **Yes** |

### Test 22: `[PDT-2599] TC-BE-22: new Signed start strictly before first version start → 4xx`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-22 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2367` |
| **Actual** | **Passed** (~12.8s) |
| **Meets expectation** | **Yes** |

### Test 23: `[PDT-2599] TC-BE-23: invalid contractVersionStatus in PUT body → client error`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-23 |
| **Created entities / links** | `http://10.236.20.11:8080/service-contracts/preview?id=2368` |
| **Actual** | **Passed** (~11.2s) |
| **Meets expectation** | **Yes** |

### Test 24: `[PDT-2599] TC-BE-24: PER_PIECE — first Signed window — invoices issued (distinct drivers v1/v2)`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-24 |
| **Expected** | Billing chain completes; invoices for first Signed window |
| **Actual** | **Failed** — `PDT-2599 billing: need formula from third tab and/or price components (settlement + billing) before POST /service-contract` at `pdt-2599-service-contract.fixtures.ts:1206` (`expect(formula).toBeTruthy()` received `null`) |
| **Meets expectation** | **No** |

### Test 25: `[PDT-2599] TC-BE-25: PER_PIECE — second Signed window — billingDate aligns second driver fingerprints`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-25 |
| **Actual** | **Skipped** (serial `PER_PIECE` suite after TC-BE-24 failure) |
| **Meets expectation** | **Not run** |

### Test 26: `[PDT-2599] TC-BE-26 (negative): PER_PIECE — POST succeeds — no draft invoices`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-26 |
| **Actual** | **Skipped** |
| **Meets expectation** | **Not run** |

### Test 27: `[PDT-2599] TC-BE-27: OVER_TIME_ONE_TIME — first Signed window`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-27 |
| **Actual** | **Failed** — same formula resolution error (`fixtures.ts:1206`) |
| **Meets expectation** | **No** |

### Test 28: `[PDT-2599] TC-BE-28: OVER_TIME_ONE_TIME — second Signed window fingerprints`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-28 |
| **Actual** | **Skipped** |
| **Meets expectation** | **Not run** |

### Test 29: `[PDT-2599] TC-BE-29 (negative): OVER_TIME_ONE_TIME — POST succeeds — no invoices`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-29 |
| **Actual** | **Skipped** |
| **Meets expectation** | **Not run** |

### Test 30: `[PDT-2599] TC-BE-30: OVER_TIME_PERIODICAL — first Signed window`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-30 |
| **Actual** | **Failed** — same formula resolution error (`fixtures.ts:1206`) |
| **Meets expectation** | **No** |

### Test 31: `[PDT-2599] TC-BE-31: OVER_TIME_PERIODICAL — second Signed window fingerprints`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-31 |
| **Actual** | **Skipped** |
| **Meets expectation** | **Not run** |

### Test 32: `[PDT-2599] TC-BE-32 (negative): OVER_TIME_PERIODICAL — POST succeeds — no invoices`

| Field | Content |
|--------|---------|
| **Covers test case(s)** | TC-BE-32 |
| **Actual** | **Skipped** |
| **Meets expectation** | **Not run** |

---

## Footnotes

- **`playwright-report.json`** in EnergoTS root was overwritten by the subsequent **`send report`** / teardown project run (only `global-setup` remained). This report is sourced from the **CLI list output** of the full multi-project run.
- **NO_COLOR / FORCE_COLOR** warnings appeared in stderr; no functional impact observed.
