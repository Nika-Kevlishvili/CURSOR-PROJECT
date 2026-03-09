# HandsOff Report: NT-1

**Jira:** NT-1  
**Title:** Invoice cancellation - it is not possible to cancel an invoice if it's paid and the payment package is locked

**Spec file:** `EnergoTS/tests/cursor/NT-1-invoice-cancellation.spec.ts`  
**How to run:** From `Cursor-Project/EnergoTS/`: `npx playwright test tests/cursor/NT-1-invoice-cancellation.spec.ts` or `npx playwright test --grep "NT-1"` (cursor branch only). If token/env missing locally, run `npx playwright test --project=setup` first (requires .env with PORTAL_USER, PASSWORD, DEVAUTHAPI/TESTAUTHAPI, BASE_URL), or run via GitHub/CI.

---

## Playwright test results

### Test 1: [NT-1]: Unpaid invoice cancellation – baseline

| Field | Content |
|-------|--------|
| **What is verified** | Unpaid invoice can be cancelled via POST /invoice-cancellation (baseline). Flow: create customer → collection channel + unlocked payment package → manual invoice → POST invoice-cancellation; response in 2xx. |
| **Steps** | Create customer (POST /customer); create collection channel and payment package (UNLOCKED); create manual invoice; POST /invoice-cancellation with invoiceId; assert response OK. |
| **Result** | **Failed** |
| **Failure reason** | 401 Unauthorized on first step (POST http://10.236.20.11:8091/customer). Authentication required; token or env setup may be missing. Run global setup (`npx playwright test --project=setup`) or ensure .env and token.json/envVariables.json exist. |

---

### Test 2: [NT-1]: Paid invoice with unlocked payment package – cancellation succeeds

| Field | Content |
|-------|--------|
| **What is verified** | Paid invoice with unlocked payment package: cancellation via POST /invoice-cancellation succeeds (happy path; regression after NT-1 fix). |
| **Steps** | Create customer; collection channel + unlocked payment package; manual invoice; create payment linked to invoice; POST /invoice-cancellation; expect 2xx. |
| **Result** | **Failed** |
| **Failure reason** | 401 Unauthorized on first step (POST /customer). Same auth/setup issue as Test 1. |

---

### Test 3: [NT-1]: Paid invoice with locked payment package – cancellation allowed (NT-1 fix)

| Field | Content |
|-------|--------|
| **What is verified** | Main NT-1 scenario: invoice paid, payment package LOCKED; create invoice cancellation. Expected (after fix): cancellation allowed. Current bug: "Payment package not found with id X and lock status in UNLOCKED". |
| **Steps** | Create customer; collection channel + payment package with lockStatus LOCKED; manual invoice; payment linked to invoice; POST /invoice-cancellation; expect 2xx (cancellation allowed). |
| **Result** | **Failed** |
| **Failure reason** | 401 Unauthorized on first step (POST /customer). Same auth/setup issue; test did not reach invoice cancellation step. |

---

## Summary

| Metric | Value |
|--------|--------|
| **Total tests** | 3 |
| **Passed** | 0 |
| **Failed** | 3 |
| **Not run** | 0 |

All three tests failed at the first API call (Create customer) with **401 Unauthorized**. The spec and scenarios are in place; to get green results, run in an environment where auth is configured (e.g. run global setup then tests locally, or run the same spec via GitHub/CI with proper secrets).
