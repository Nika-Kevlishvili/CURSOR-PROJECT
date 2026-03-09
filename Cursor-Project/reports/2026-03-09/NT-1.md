# HandsOff Report: NT-1

**Jira:** NT-1 – Invoice cancellation - it is not possible to cancel an invoice if it's paid and the payment package is locked  
**Spec file:** `Cursor-Project/EnergoTS/tests/cursor/NT-1-invoice-cancellation.spec.ts`  
**How to run:** `npx playwright test --grep "NT-1"` or `npx playwright test tests/cursor/NT-1-invoice-cancellation.spec.ts` (from EnergoTS directory, cursor branch)

---

## Playwright test results

### 1. NT-1: POST /payment/cancel endpoint is reachable and returns 4xx for unknown payment_id

- **What is verified:** POST `/payment/cancel` is called with a non-existent payment ID (999999); response status is asserted to be one of 400, 404, 422.
- **Steps:** Load token from fixtures; POST /payment/cancel with body `{ paymentId: 999999 }`; assert status in [400, 404, 422].
- **Result:** **Failed**
- **Reason:** Received status **401 Unauthorized** (expected 400, 404, or 422). Endpoint is reachable; 401 may be due to token/env (e.g. token for different environment or expired). For unknown payment_id the API may return 401 when auth fails before entity lookup.

### 2. NT-1: POST /payment/cancel with UNLOCKED package succeeds

- **What is verified:** POST `/payment/cancel` with a payment whose package is UNLOCKED; expects 200 and successful cancellation.
- **Steps:** Skip unless env `NT1_PAYMENT_ID_UNLOCKED` is set; POST /payment/cancel with that payment ID; assert status 200.
- **Result:** **Skipped**
- **Reason:** `NT1_PAYMENT_ID_UNLOCKED` not set (no test data for payment with UNLOCKED package).

### 3. NT-1: POST /payment/cancel with LOCKED package returns error about UNLOCKED

- **What is verified:** POST `/payment/cancel` with a payment whose package is LOCKED; expects 400 or 404 and response body containing "payment package not found", "unlocked", or "lock".
- **Steps:** Skip unless env `NT1_PAYMENT_ID_LOCKED` is set; POST /payment/cancel with that payment ID; assert status and body.
- **Result:** **Skipped**
- **Reason:** `NT1_PAYMENT_ID_LOCKED` not set (no test data for payment with LOCKED package).

---

**Summary:** 1 run, 1 failed, 2 skipped. To run the skipped tests, set `NT1_PAYMENT_ID_UNLOCKED` and `NT1_PAYMENT_ID_LOCKED` to valid payment IDs in the target environment and re-run.

---

**Slack:** Full report sent to tester (nika kevlishvili, DM). AI report channel was not found in workspace; duplicate was not sent.
