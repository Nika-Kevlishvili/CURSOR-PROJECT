# Invoice Cancellation – Payment Cancel API Lock Restriction Regression (NT-1)

**Jira:** NT-1 (AI Experiments)  
**Type:** Regression  
**Summary:** Regression coverage for `POST /payment/cancel`: after enabling invoice cancellation for LOCKED packages (NT-1), normal payment cancellation must still remain restricted when the payment package is LOCKED (as per `what_could_break`).

**Scope:** NT-1 requires invoice cancellation to work even when the payment package is LOCKED, by propagating an invoice-cancellation intent (e.g. `invoiceCancellation == true`) to bypass the lock restriction in that specific flow. This document ensures that the bypass does not accidentally make the general payment-cancel API permissive: normal payment cancellations must still be blocked (or follow the existing product rule) when the payment package is LOCKED, with a clear and correct error response. It also covers interaction tests between invoice cancellation and payment cancellation to detect partial-failure or sequencing issues.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **User/permissions:** A user exists with permission to cancel payments (unless the test case is explicitly about access control).
- **Invoice:** An invoice exists and is paid.
- **Payment:** A payment exists and is linked to the invoice.
- **Payment package:** The payment belongs to a payment package whose lock status is **LOCKED**.

---

## TC-1 (Negative): Normal `POST /payment/cancel` is rejected when the payment package is LOCKED (lock restriction remains enforced)

**Objective:** Ensure that, outside the invoice cancellation flow, payment cancellation remains restricted when the payment package is LOCKED. This protects against an overly broad bypass introduced by the NT-1 change.

**Preconditions:**
1. A paid invoice exists and is linked to a payment.
2. The payment belongs to a payment package whose lock status is **LOCKED**.

**Steps:**
1. Call `POST /payment/cancel` to cancel the payment (use the normal payment-cancel API payload).
2. Observe the response and verify persistence (payment remains not cancelled/reversed as per the rule).

**Expected result:** The payment cancel request is rejected according to the product rule for LOCKED payment packages (e.g. 4xx with a clear message explaining the payment package is locked and the operation is not allowed). The system must not silently succeed.

---

## TC-2 (Positive): Invoice cancellation succeeds for a LOCKED package, while normal payment cancel stays restricted

**Objective:** Verify the intended split behaviour: invoice cancellation must succeed even with a LOCKED package, but normal payment cancellation must still be restricted.

**Preconditions:**
1. Same as TC-1: paid invoice, payment package is LOCKED.

**Steps:**
1. Create invoice cancellation for the paid invoice (e.g. `POST /invoice-cancellation`).
2. Verify invoice cancellation is created successfully.
3. Attempt normal `POST /payment/cancel` for the same payment.

**Expected result:** Step 1 succeeds (invoice cancellation created). Step 3 remains rejected according to the lock restriction (or behaves exactly as specified by the product rule), proving the bypass is scoped to invoice cancellation only.

---

## TC-3 (Negative): `POST /payment/cancel` rejected when required identifiers are missing (validation)

**Objective:** Ensure the payment-cancel endpoint validates input and returns a clear error without changing payment state.

**Preconditions:**
1. A user has access to the payment-cancel API.

**Steps:**
1. Call `POST /payment/cancel` with missing or invalid required identifiers (e.g. missing payment id).
2. Observe response and verify no payment state change.

**Expected result:** The request is rejected with a clear validation error; no payment is cancelled.

---

## TC-4 (Positive): Sequencing/interaction – invoice cancellation does not cause inconsistent payment-cancel behaviour (no partial failure)

**Objective:** Ensure there are no unexpected side effects or partial failures when performing invoice cancellation on a paid invoice with a LOCKED package, particularly around downstream processing and subsequent calls.

**Preconditions:**
1. Paid invoice exists, payment package is LOCKED.

**Steps:**
1. Create invoice cancellation for the invoice and verify it succeeds.
2. Re-run the same invoice cancellation request (idempotency check, if supported by the API/product).
3. Attempt normal `POST /payment/cancel` and verify it behaves as in TC-1 (restricted).

**Expected result:** Step 1 succeeds; Step 2 does not create a duplicate cancellation (it should be idempotent or clearly rejected as duplicate); Step 3 remains restricted. There are no inconsistent states caused by partial processing.

---

## References

- **Jira:** NT-1 – Invoice cancellation when paid and payment package locked.
- **Cross-dependency (given):** Regression risks include ensuring invoiceCancellation flag propagation and ensuring normal payment cancellation remains restricted.
