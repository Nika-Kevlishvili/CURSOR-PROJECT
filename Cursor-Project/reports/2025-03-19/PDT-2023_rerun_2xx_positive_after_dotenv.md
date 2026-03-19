## PDT-2023 – Playwright rerun (dotenv/baseURL fix)

- **Date**: 2026-03-19
- **Working dir**: `d:/Asterbit/CURSOR-PROJECT/Cursor-Project/EnergoTS/`
- **Branch**: `cursor`
- **Command**: `npx playwright test tests/cursor/PDT-2023-billing-run-termination.spec.ts`

### Totals

- **Total**: 55
- **Passed**: 2
- **Failed**: 43
- **Skipped**: 10
- **Duration**: ~10.4s (reported by Playwright)

### Key failure reasons (high-signal)

The failures are overwhelmingly the same root symptom:

- **List endpoint call is not OK**: `expect(listRes.ok()).toBe(true)` fails with **Received: false**.
  - This happens across many tests that start by calling `DevPage.request.get(Endpoints.list(...))`.
  - Example failing assertion locations:
    - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:41` (TC-1)
    - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:89` (TC-5)
    - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:78` (TC-4)
    - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:107` (TC-7)
    - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:287` (TC-24, filtered list)
    - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:552` (Billing_run_list_and_filters TC-1)
    - `tests/cursor/PDT-2023-billing-run-termination.spec.ts:577` (Billing_run_list_and_filters TC-4)

### Notes

- Since `listRes.ok()` is false, most tests fail **before** they can validate business rules (status transitions, buttons, locks, etc.). The next debugging step would be to capture the underlying HTTP status/body for the failing list calls (the spec already attempts `await listRes.json().catch(() => ({}))`).

