# Invoice Cancellation – UNPAID Invoice (NT-1 Baseline)

**Jira:** NT-1 (AI Experiments)  
**Type:** Regression  
**Summary:** Baseline coverage: invoice cancellation must work when the invoice is unpaid (no payment exists, so no payment package is involved). This must remain stable after the NT-1 locked-package fix.

**Scope:** This document covers the simplest invoice cancellation scenario: cancelling an unpaid invoice. It protects against regressions where changes in paid-invoice logic (especially around payment cancellation and package-lock handling) accidentally impact unpaid cancellations.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **User/permissions:** A user exists with permission to create invoice cancellations.
- **Invoice:** An invoice exists, is generated successfully, has no payment recorded, and is not already cancelled.

---

## TC-1 (Positive): Cancel an unpaid invoice (baseline happy path)

**Objective:** Verify that invoice cancellation succeeds when there is no payment for the invoice, and therefore no payment package is involved.

**Preconditions:**
1. An invoice has been generated and is not cancelled.
2. The invoice is unpaid (no payment exists/linked to this invoice).

**Steps:**
1. Ensure the invoice is unpaid (do not create any payment for it).
2. Create an invoice cancellation for the invoice (via UI or API).
3. Verify the cancellation is created and the invoice reflects cancellation.

**Expected result:** The invoice cancellation is created successfully, and there are no errors related to payment or payment package.

---

## TC-2 (Negative): Reject cancellation when invoice identifier is not found (unpaid case)

**Objective:** Ensure the system does not create cancellations for non-existent invoices and returns a clear “not found” error.

**Preconditions:**
1. A user has access to the invoice cancellation endpoint/flow.

**Steps:**
1. Attempt to create an invoice cancellation with a non-existent invoice identifier.
2. Observe the response and verify persistence.

**Expected result:** The system returns a clear “invoice not found” error. No cancellation is created.

---

## TC-3 (Positive): Regression after NT-1 fix – unpaid invoice cancellation still works via API

**Objective:** After the NT-1 fix is delivered, confirm unpaid invoice cancellation still works through the API entry point as well.

**Preconditions:**
1. Same as TC-1: generated, unpaid invoice not yet cancelled.

**Steps:**
1. Call `POST /invoice-cancellation` for the unpaid invoice.
2. Verify success response and created cancellation.

**Expected result:** The API request succeeds and the cancellation is created; no regression is introduced by NT-1 changes.

---

## References

- **Jira:** NT-1 – baseline flow without payment.
- **Integration:** Invoice cancellation flow; unpaid path should not depend on payment cancellation logic.
