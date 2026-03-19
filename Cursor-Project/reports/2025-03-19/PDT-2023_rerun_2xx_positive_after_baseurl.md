# PDT-2023 — Playwright rerun after baseURL + endpoint fixes

## Run details
- **Working directory**: `d:/Asterbit/CURSOR-PROJECT/Cursor-Project/EnergoTS/`
- **Branch**: `cursor`
- **Command**: `npx playwright test tests/cursor/PDT-2023-billing-run-termination.spec.ts`
- **Fixtures present**: `fixtures/token.json`, `fixtures/envVariables.json`

## Totals
- **Total tests**: 55
- **Passed**: 0
- **Failed**: 45
- **Skipped**: 10

## Key failure reason(s)
### 1) `TypeError: apiRequestContext.get: Invalid URL`
- **Impact**: This error is blocking most API calls in the spec (e.g. `DevPage.request.get(...)`), so many tests fail at the first request.
- **Typical call site**: `DevPage.request.get(Endpoints.list({ ... }))`
- **Example occurrences**:
  - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:40:43` (TC-1)
  - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:55:43` (TC-2)
  - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:551:39` (Billing_run_list_and_filters TC-1)
  - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:568:39` (Billing_run_list_and_filters TC-3)

## Notes / next check
- This looks like the request context is still receiving a URL that Playwright treats as invalid (commonly caused by an empty/missing `baseURL`, or an endpoint string that’s not a valid absolute/relative URL for the configured context).

