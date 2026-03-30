# Invoice Cancellation – Paid Invoice with UNLOCKED Payment Package (NT-1 Baseline)

**Jira:** NT-1 (AI Experiments)  
**Type:** Regression  
**Summary:** Baseline coverage: invoice cancellation must work for a paid invoice when the payment package is **UNLOCKED**. This must remain working after the NT-1 fix for the locked-package scenario.

**Scope:** This document covers the happy path for cancelling a paid invoice when the payment’s package is UNLOCKED. These tests protect against regressions where a fix for the LOCKED-package case accidentally breaks the normal flow.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **User/permissions:** A user exists with permission to create invoice cancellations.
- **Invoice:** An invoice exists, is generated successfully, and is not already cancelled.
- **Payment:** The invoice is paid (a payment exists and is linked to the invoice).
- **Payment package:** The payment belongs to a payment package whose lock status is **UNLOCKED**.

---

## TC-1 (Positive): Cancel a paid invoice when the payment package is UNLOCKED (happy path)

**Objective:** Verify that invoice cancellation succeeds for a paid invoice when the payment package is UNLOCKED.

**Preconditions:**
1. An invoice has been generated and is not cancelled.
2. The invoice has been paid and the payment is linked to the invoice.
3. The payment package lock status is **UNLOCKED**.

**Steps:**
1. Ensure the invoice is paid and the payment package lock status is UNLOCKED.
2. Create an invoice cancellation for that invoice (via UI or API).
3. Verify the invoice cancellation exists and the invoice state reflects cancellation.

**Expected result:** The cancellation is created successfully, and there is no error related to payment package lookup or lock status.

---

## TC-2 (Negative): Reject cancellation request when the invoice is already cancelled (no duplicates)

**Objective:** Verify that the system prevents duplicate cancellations for the same invoice and returns a clear error.

**Preconditions:**
1. A paid invoice exists whose payment package is UNLOCKED.
2. The invoice already has an invoice cancellation created (it is already cancelled).

**Steps:**
1. Attempt to create another invoice cancellation for the same invoice.
2. Observe the response and verify persistence.

**Expected result:** The system rejects the request with a clear message (e.g. “Invoice is already cancelled” or equivalent). No additional cancellation is created, and the existing cancellation remains unchanged.

---

## TC-3 (Positive): Regression check after NT-1 fix – UNLOCKED case still works via API

**Objective:** After the NT-1 fix is delivered, confirm the UNLOCKED-package baseline still works through the API entry point as well.

**Preconditions:**
1. Same as TC-1: paid invoice, payment package UNLOCKED.

**Steps:**
1. Call `POST /invoice-cancellation` for the paid invoice with an UNLOCKED payment package.
2. Verify the response indicates success and the cancellation is created.

**Expected result:** The API request succeeds and the cancellation is created; no regression is introduced by the NT-1 change.

---

## References

- **Jira:** NT-1 – baseline behaviour when payment package is UNLOCKED.
- **Integration:** Invoice cancellation flow; payment package lock status handling.
