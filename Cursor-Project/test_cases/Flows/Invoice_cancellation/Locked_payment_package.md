# Invoice Cancellation – Paid Invoice with Locked Payment Package (NT-1)

**Jira:** NT-1 (AI Experiments)  
**Type:** Bug  
**Summary:** Invoice cancellation must be possible for a paid invoice even when the related payment package is **LOCKED**. Currently the system blocks the cancellation flow with an error requiring an **UNLOCKED** package.

**Scope:** This document covers the paid-invoice cancellation flow when the payment belongs to a **locked payment package** (e.g. already reconciled/closed). The expected behaviour is that invoice cancellation is still allowed, and the implementation must propagate an “invoice cancellation” intent so that the usual payment-package lock restriction is bypassed for this specific flow. It also includes negative validation cases to ensure only valid requests can create cancellations and that error messages are clear.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **User/permissions:** A user exists with permission to create invoice cancellations (unless the test case is explicitly about access control).
- **Invoice:** An invoice exists, is generated successfully, and is not already cancelled.
- **Payment:** The invoice is paid (a payment exists and is linked to the invoice).
- **Payment package:** The payment is grouped into a payment package, and the payment package lock status is **LOCKED**.

---

## TC-1 (Positive): Create invoice cancellation for a paid invoice when the payment package is LOCKED (main NT-1 scenario)

**Objective:** Verify that a user can create an invoice cancellation even when the related payment package is locked. This is the core NT-1 expectation and must not fail with a message requiring UNLOCKED status.

**Preconditions:**
1. An invoice has been generated and is not cancelled.
2. The invoice has been paid and the payment is linked to the invoice.
3. The payment belongs to a payment package whose lock status is **LOCKED**.

**Steps:**
1. Ensure the invoice is paid and the payment package lock status is **LOCKED** (e.g. lock the package through the product flow or test data setup).
2. Create an invoice cancellation for this invoice (e.g. via UI flow or `POST /invoice-cancellation`).
3. Observe the response and the persisted state of the invoice/cancellation.

**Expected result:** The invoice cancellation is created successfully. The system does not require the payment package to be UNLOCKED for this flow, and the user does not see an error message like “Payment package not found with id … and lock status in UNLOCKED”.

**Actual result (current bug):** The system returns an error: “Payment package not found with id 1100 and lock status in UNLOCKED”, and the cancellation is not created.

**References:** NT-1 description; paid invoice cancellation; locked payment package.

---

## TC-2 (Positive): API-level cancellation succeeds and explicitly bypasses lock restriction only for invoice cancellation

**Objective:** Verify that the API path (e.g. `POST /invoice-cancellation`) succeeds for the locked-package scenario and that the backend treats it as an invoice-cancellation flow (i.e. the “invoice cancellation intent/flag” is propagated so the lock restriction is bypassed only for this flow).

**Preconditions:**
1. Same as TC-1: paid invoice and payment package is **LOCKED**.

**Steps:**
1. Call `POST /invoice-cancellation` for the paid invoice whose payment package is LOCKED.
2. Verify the request succeeds and a cancellation is created.
3. Verify there is no error message that suggests the package must be UNLOCKED.

**Expected result:** The API request succeeds and a cancellation is created. The lock restriction is bypassed for invoice cancellation only (the system behaviour is consistent with the expected “invoiceCancellation == true” bypass described in cross-dependency data).

**Actual result (current bug):** The API call fails with “Payment package not found with id … and lock status in UNLOCKED”.

---

## TC-3 (Negative): Reject cancellation when invoice identifier is missing or invalid (input validation)

**Objective:** Ensure the system validates the request payload and does not create cancellations for invalid input, returning a clear error.

**Preconditions:**
1. A user has access to the cancellation endpoint/flow.

**Steps:**
1. Attempt to create an invoice cancellation with a missing invoice identifier (e.g. empty invoice number) or an invalid format (e.g. non-existent invoice number).
2. Observe the response and verify persistence.

**Expected result:** The system rejects the request with a clear validation or “not found” error. No cancellation record is created and the invoice state is unchanged.

---

## TC-4 (Negative): Reject cancellation when the payment package is missing or not linked to the payment (data integrity)

**Objective:** Ensure the system behaves safely and predictably if the payment exists but the system cannot resolve a payment package (e.g. missing linkage, inconsistent data), and that the error is clear and not misleading.

**Preconditions:**
1. An invoice is paid and linked to a payment.
2. The payment package reference/link for that payment is missing or inconsistent (test data setup / controlled negative scenario).

**Steps:**
1. Attempt to create invoice cancellation for the paid invoice.
2. Observe error message and confirm no cancellation is created.

**Expected result:** The system rejects the cancellation with a clear error indicating that required payment-package data is missing/inconsistent. It must not incorrectly claim the package exists but is “not found with lock status in UNLOCKED”.

---

## References

- **Jira:** NT-1 – Invoice cancellation - it is not possible to cancel an invoice if it's paid and the payment package is locked.
- **Cross-dependency (given):** Entry point `POST /invoice-cancellation`; `InvoiceCancellationService` processing; `PaymentService.cancel` invoked from invoice cancellation. Regression risks: invoiceCancellation flag propagation; normal payment cancellation remains restricted; partial failure behaviour; accounting periods; process/notification; document generation.
