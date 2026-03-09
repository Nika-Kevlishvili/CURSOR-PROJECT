# Invoice Cancellation – Paid Invoice with Locked Payment Package (NT-1)

**Jira:** NT-1 (AI Experiments)  
**Type:** Task  
**Summary:** Invoice cancellation – it must be possible to cancel an invoice when it is paid and the payment package is locked.

**Scope:** Create invoice cancellation when the invoice has been paid and the payment package for that payment is in LOCKED status. Current behaviour returns an error; expected behaviour is that cancellation is allowed.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **Invoice:** Generated and in a state that allows cancellation (e.g. not already cancelled).
- **Payment:** Invoice has been paid (payment exists and is linked to the invoice).
- **Payment package:** The payment package for this payment is **locked** (lock status = LOCKED).

---

## TC-1: Invoice cancellation when paid and payment package is locked (main scenario)

**Objective:** Verify that a user can create an invoice cancellation when the invoice is paid and the payment package is locked. The system must not block cancellation with "Payment package not found with id X and lock status in UNLOCKED".

**Preconditions:**
1. Invoice has been generated.
2. Invoice has been paid (payment recorded).
3. Payment package for this payment is **locked** (lock status LOCKED).

**Steps:**
1. Generate an invoice (or use existing test invoice).
2. Pay the invoice (record payment against the invoice).
3. Lock the payment package for this payment (set payment package lock status to LOCKED).
4. Create invoice cancellation (via UI or API, as per product flow).

**Expected result:** Invoice cancellation is created successfully. The system allows cancellation even when the payment package is locked. No error "Payment package not found with id X and lock status in UNLOCKED".

**Actual result (current bug):** Error returned: "Payment package not found with id 1100 and lock status in UNLOCKED". Cancellation is blocked.

**References:** NT-1 description; payment package lock status; invoice cancellation flow.

---

## TC-2: Assertion – no UNLOCKED check blocking cancellation

**Objective:** Ensure the cancellation flow does not require the payment package to be in UNLOCKED status. Cancellation should be permitted when the package is LOCKED.

**Preconditions:** Same as TC-1 (paid invoice, locked payment package).

**Steps:**
1. Confirm payment package lock status is LOCKED (e.g. via API or DB).
2. Call or perform the invoice cancellation (API or UI).
3. Verify response: success and cancellation record created; no error message containing "lock status in UNLOCKED".

**Expected result:** Cancellation succeeds. Backend does not restrict cancellation to UNLOCKED payment packages only.

---

## Entry points and upstream (cross-dependency)

- **API:** `POST /invoice-cancellation`, `POST /payment/cancel`
- **Services:** `InvoiceCancellationService.processDebitNoteInvoice`, `PaymentService.cancel`
- **Upstream:** `PaymentService.cancel` enforces payment package in **UNLOCKED**; when package is LOCKED, cancellation path fails with "Payment package not found with id X and lock status in UNLOCKED".
- **What could break:** Invoice cancellation when package is locked; payment cancel API behaviour when package is locked.

---

## TC-3: API-level invoice cancellation when payment package is locked

**Objective:** Verify that `POST /invoice-cancellation` allows creating a cancellation when the related payment package is locked (no UNLOCKED-only check).

**Preconditions:** Same as TC-1 (invoice generated, paid, payment package locked).

**Steps:**
1. Ensure invoice is paid and payment package is LOCKED (e.g. id 1100).
2. Call `POST /invoice-cancellation` with the invoice/cancellation payload.
3. Assert response: success (e.g. 200/201) and cancellation record created; no error "Payment package not found with id … and lock status in UNLOCKED".

**Expected result:** Invoice cancellation is created via API; backend does not require payment package to be UNLOCKED for this flow.

**Actual result (current bug):** Error "Payment package not found with id 1100 and lock status in UNLOCKED".

---

## References

- **Jira:** NT-1 – Invoice cancellation when paid and payment package locked.
- **Error (current):** "Payment package not found with id 1100 and lock status in UNLOCKED".
- **Integration:** Invoice cancellation endpoint/service; payment package entity and lock status; payment–invoice linkage.
- **Cross-dependency:** Entry points POST /invoice-cancellation, POST /payment/cancel; InvoiceCancellationService.processDebitNoteInvoice; PaymentService.cancel (enforces UNLOCKED).
