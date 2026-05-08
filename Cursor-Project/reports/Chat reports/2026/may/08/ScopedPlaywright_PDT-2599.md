# Playwright run — detailed report (scoped)

## Header (run metadata)

| Field | Value |
|--------|--------|
| **Jira** | PDT-2599 |
| **Title** | BE - Service Contract - The system doesn't consider the contract version status |
| **Date (local)** | 2026-05-08 |
| **Environment / BASE_URL** | API base from run output: `http://10.236.20.11:8091/`; frontend: `http://10.236.20.11:8080/` |
| **Spec file(s)** | `Cursor-Project/EnergoTS/tests/cursor/PDT-2599-be-service-contract-version.spec.ts` |
| **Test case files** (if applicable) | Not mapped in this scoped run |
| **Playwright command** | `npx playwright test tests/cursor/PDT-2599-be-service-contract-version.spec.ts --grep "\[PDT-2599\] TC-BE-(10|[1-9]):"` |
| **Totals** | 10 passed, 0 failed, 0 skipped |
| **Selection method** | Playwright has no native “first N tests”. **Method:** (1) `npx playwright test … --list --grep "PDT-2599"` defines discovery order (35 tests); **first 10** entries are TC-BE-1 … TC-BE-10. (2) Executed subset with regex `--grep "\[PDT-2599\] TC-BE-(10|[1-9]):"`, which matches only TC-BE-1 … TC-BE-10 and excludes TC-BE-11+. |
| **Branch** | `cursor` (Rule ENERGOTS.0) |

---

## Per-test sections

### Test 1: [PDT-2599] TC-BE-1: BE - Service Contract - The system doesn't consider the contract version status

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-1: BE - Service Contract - The system doesn't consider the contract version status` |
| **Covers test case(s)** | TC-BE-1 (from title) |
| **Created entities / links** | Portal preview logged in stdout (e.g. `service-contracts/preview?id=2883`). |
| **Expected** | Per TC-BE-1 / spec assertions (contract versioning behaviour). |
| **Actual** | Passed (58.7s total suite). |
| **Meets expectation** | **Yes** — test completed without assertion failure. |

### Test 2: [PDT-2599] TC-BE-2: POST /service-contract/list — created contract appears when searching by contract number

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-2: POST /service-contract/list — created contract appears when searching by contract number` |
| **Covers test case(s)** | TC-BE-2 |
| **Created entities / links** | Portal preview `…/preview?id=2884`. |
| **Expected** | Listed contract discoverable via POST list. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

### Test 3: [PDT-2599] TC-BE-3: GET /service-contract/{id} — non-existent id returns 400 or 404

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-3: GET /service-contract/{id} — non-existent id returns 400 or 404` |
| **Covers test case(s)** | TC-BE-3 |
| **Created entities / links** | No portal URL built (stderr helper noted missing ResponseLinker links for this path). |
| **Expected** | 400 or 404 for invalid id. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

### Test 4: [PDT-2599] TC-BE-4: GET /service-contract/{id} — invalid versionId returns error

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-4: GET /service-contract/{id} — invalid versionId returns error` |
| **Covers test case(s)** | TC-BE-4 |
| **Created entities / links** | Portal preview `…/preview?id=2885`. |
| **Expected** | Client/error response for bad versionId. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

### Test 5: [PDT-2599] TC-BE-5: GET /service-contract/{id} — first version row has versionId 1

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-5: GET /service-contract/{id} — first version row has versionId 1` |
| **Covers test case(s)** | TC-BE-5 |
| **Created entities / links** | Portal preview `…/preview?id=2886`. |
| **Expected** | First row `versionId === 1`. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

### Test 6: [PDT-2599] TC-BE-6: GET /service-contract/third-tab-fields — missing serviceDetailId rejected

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-6: GET /service-contract/third-tab-fields — missing serviceDetailId rejected` |
| **Covers test case(s)** | TC-BE-6 |
| **Created entities / links** | No portal URL built (same helper note as TC-BE-3). |
| **Expected** | Request rejected when `serviceDetailId` missing. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

### Test 7: [PDT-2599] TC-BE-7: duplicate Valid start date on new version → 4xx (EN message)

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-7: duplicate Valid start date on new version → 4xx (EN message)` |
| **Covers test case(s)** | TC-BE-7 |
| **Created entities / links** | Portal preview `…/preview?id=2887`. |
| **Expected** | 4xx + EN validation message. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

### Test 8: [PDT-2599] TC-BE-8: three Signed versions — end dates = day before next start

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-8: three Signed versions — end dates = day before next start` |
| **Covers test case(s)** | TC-BE-8 |
| **Created entities / links** | Portal preview `…/preview?id=2888`. |
| **Expected** | Coherent Signed chain end dates. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

### Test 9: [PDT-2599] TC-BE-9: insert Signed between two Signed — neighbour end dates recalc

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-9: insert Signed between two Signed — neighbour end dates recalc` |
| **Covers test case(s)** | TC-BE-9 |
| **Created entities / links** | Portal preview `…/preview?id=2889`. |
| **Expected** | Neighbour boundaries updated after insert. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

### Test 10: [PDT-2599] TC-BE-10: versionId ordinals 1..n after out-of-order chronological inserts

| Field | Content |
|--------|--------|
| **Playwright title** | `[PDT-2599] TC-BE-10: versionId ordinals 1..n after out-of-order chronological inserts` |
| **Covers test case(s)** | TC-BE-10 |
| **Created entities / links** | Portal preview `…/preview?id=2890`. |
| **Expected** | Monotonic version ordinals after out-of-order inserts. |
| **Actual** | Passed. |
| **Meets expectation** | **Yes**. |

---

## Footnotes

- **Stdout notices:** Several tests logged `[PDT-2599] No contract formula resolved from third-tab or price component; POST uses empty contractFormulas.` — informational from fixtures; run still green.
- **Machine report:** `Cursor-Project/EnergoTS/playwright-report-detailed.md` generated from `playwright-report.json` via `node ../config/playwright/generate-detailed-report.mjs`.
