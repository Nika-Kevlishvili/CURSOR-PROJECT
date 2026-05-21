# PDT-2880 Detailed Test Report

Run context
- Jira key: PDT-2880
- Date: 2026-05-19
- Branch: cursor
- Spec path: `Cursor-Project/EnergoTS/tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts`
- Source JSON: `Cursor-Project/EnergoTS/playwright-report.json`
- Command: `npx playwright test tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts --project=main`
- Environment: Dev API `http://10.236.20.11:8091`, Portal `http://10.236.20.11:8080`

Summary
- Total in scope: 1
- Passed: 0
- Failed: 1
- Skipped: 0
- Duration: ~170s (~2.7 min)
- Test case IDs: TC-BE-1
- Verdict: **Bug reproduced** (failure on assertion is expected for open defect PDT-2880)

## Jira reference

- **Title:** Customer - "POD is disconnected" incorrect label
- **Reproduce (prod):** POD `32Z430011909019C`; Customer A `4506098796` (LOST); Customer B `7710028805`; RFD id=1048; DPS id=1134
- **Expected:** Former customer must **not** show "POD is disconnected" after disconnect for the new customer
- **Actual (prod):** Label shown on old customer `4506098796`

## Precondition flow (automated)

| Step | Result |
|------|--------|
| Termination `DEACTIVATION_OF_POINTS_OF_DELIVERY` + group | OK |
| Private Customer A + B, shared POD | OK |
| Contract A: ENTERED_INTO_FORCE → ACTIVE_IN_PERPETUITY → POD deactivate (yesterday) | OK |
| `/ttest/pod-termination` | HTTP 200 |
| Customer A status | **LOST** |
| Contract B + POD (tomorrow), billing, reminder, RFD, DPS | OK |

## Test cases

### TC-BE-1

- **Status:** FAILED (bug reproduction)
- **Title:** isPodDisconnected shown on former LOST customer after DPS for new customer
- **Expected Result:** `GET /customer/{customerAId}` → `status=LOST`, `isPodDisconnected=false`
- **Actual Result:** `status=LOST`, `isPodDisconnected=true`
- **Assertion error:** Expected `false`, Received `true` — former LOST customer inherits POD disconnect flag from another customer's DPS workflow
- **Portal data links:**
  - Customer A (former / LOST): http://10.236.20.11:8080/customers/preview/basic?id=6069112 (identifier `7306020590`)
  - Customer B (current / ACTIVE): http://10.236.20.11:8080/customers/preview/basic?id=6069113 (identifier `5305040109`)
  - Billing run: id `19387` (completed)
  - RFD / DPS: created via API chain (see test attachments)

## Notes

- Test failure **confirms** PDT-2880; re-run after fix should **pass** on `isPodDisconnected=false` for Customer A.
- Customer B `ACTIVE` verification did not run (failed on Customer A assertion first).
- Machine report from JSON: `Cursor-Project/EnergoTS/playwright-report-detailed.md` (may reference prior run entity IDs in links section if JSON was from an earlier attempt).
