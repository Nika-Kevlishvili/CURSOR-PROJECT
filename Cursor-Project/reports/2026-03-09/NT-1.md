# NT-1 – Playwright test results

**Jira:** NT-1  
**Title:** Invoice cancellation - it is not possible to cancel an invoice if it's paid and the payment package is locked  
**Date:** 2026-03-09  
**Spec:** `EnergoTS/tests/cursor/NT-1-invoice-cancellation.spec.ts`  
**Run:** `npx playwright test --grep "NT-1"` or `npx playwright test tests/cursor/NT-1-invoice-cancellation.spec.ts` (cursor branch)  
**Assignee/Tester:** nika kevlishvili (n.kevlishvili@asterbit.io)

---

## Test 1: [NT-1] Invoice cancellation - create request

**What is verified**
- Endpoint `POST /invoice-cancellation` is called with body `{ invoiceNumbers: [] }`.
- Response status is not server error: `expect(res.status()).toBeLessThan(500)` (4xx allowed; 5xx fails).
- Invoice cancellation create API is reachable and does not return 5xx.

**Steps**
1. Create invoice cancellation request (POST /invoice-cancellation with payload).

**Result:** Passed  
**Reason:** —

---

## Test 2: [NT-1] Payment cancel - happy path - package UNLOCKED

**What is verified**
- Customer created (POST customer) – response OK.
- Collection channel created – response OK.
- Payment package created (UNLOCKED by default) – response OK.
- Payment created (initialAmount 100) – response OK.
- Second customer created (for payment reverse payload).
- Payment cancel (POST `/payment/cancel`) with created payment id and second customer id – response OK; reversal payment returned.
- Regression for NT-1: when package is UNLOCKED, payment cancel must succeed (no "Payment package not found with id X and lock status in UNLOCKED" error).

**Steps**
1. Create customer  
2. Create collection channel  
3. Create payment package  
4. Create payment  
5. Create second customer (for reverse payload)  
6. Payment cancel (package UNLOCKED – expect success)

**Result:** Passed  
**Reason:** —

---

## Run summary

| Test | Result | Reason (if failed/not run) |
|------|--------|----------------------------|
| [NT-1] Invoice cancellation - create request | Passed | — |
| [NT-1] Payment cancel - happy path - package UNLOCKED | Passed | — |

**Total:** 2 passed, 0 failed. Run command: `npx playwright test tests/cursor/NT-1-invoice-cancellation.spec.ts` from `EnergoTS/` (cursor branch).
