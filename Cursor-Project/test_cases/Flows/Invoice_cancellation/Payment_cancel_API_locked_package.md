# Invoice Cancellation Flow – Payment Cancel API When Package Is Locked (NT-1 Regression)

**Jira:** NT-1 (AI Experiments)  
**Type:** Regression / what_could_break  
**Summary:** Ensure payment cancel API and invoice cancellation flow do not break each other when the payment package is locked.

**Scope:** From cross-dependency analysis: **what_could_break** includes invoice cancellation when package is locked and **payment cancel API** behaviour. This file covers regression tests for `POST /payment/cancel` and interaction with locked payment packages.

---

## Entry points

- **API:** `POST /payment/cancel`, `POST /invoice-cancellation`
- **Services:** `PaymentService.cancel`, `InvoiceCancellationService.processDebitNoteInvoice`
- **Upstream:** `PaymentService.cancel` currently enforces payment package in **UNLOCKED**; when locked, lookups may fail with "Payment package not found with id X and lock status in UNLOCKED".

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **Invoice:** Generated and paid.
- **Payment:** Exists and is linked to the invoice.
- **Payment package:** Lock status = **LOCKED** (e.g. id 1100).

---

## TC-1: POST /payment/cancel when payment package is locked – current behaviour

**Objective:** Document and verify current behaviour of `POST /payment/cancel` when the payment package is locked. After NT-1 fix, ensure invoice cancellation is allowed; payment cancel API behaviour should be explicitly defined (allowed vs rejected with clear error).

**Preconditions:**
1. Invoice generated and paid.
2. Payment package for the payment is **locked** (LOCKED).

**Steps:**
1. Call `POST /payment/cancel` with the relevant payment/package identifier (or payload as per API spec).
2. Observe response: success or error and message.

**Expected result (to be defined):** Either (a) cancellation is allowed and payment cancel succeeds, or (b) API returns a clear, expected error when package is locked (no generic "Payment package not found with id X and lock status in UNLOCKED" if the intent is to allow cancellation in another flow). Regression: after NT-1 fix, invoice cancellation must be possible; payment cancel API behaviour must remain consistent and documented.

---

## TC-2: Invoice cancellation vs payment cancel – no mutual break

**Objective:** After NT-1 fix (invoice cancellation allowed when package locked), ensure that using invoice cancellation does not break subsequent or prior use of payment cancel API, and vice versa.

**Preconditions:** Same as TC-1 (paid invoice, locked payment package).

**Steps:**
1. (Optional) Call `POST /payment/cancel` and record result.
2. Create invoice cancellation (e.g. `POST /invoice-cancellation`) and assert success.
3. (Optional) Call `POST /payment/cancel` again if applicable and assert expected behaviour (no unexpected errors).
4. Alternatively: create invoice cancellation first, then verify payment cancel API still behaves as specified.

**Expected result:** Invoice cancellation succeeds when package is locked; payment cancel API behaviour remains as specified; no mutual break between the two flows.

---

## References

- **Jira:** NT-1 – Invoice cancellation when paid and payment package locked.
- **What could break:** Invoice cancellation when package locked; payment cancel API.
- **Integration:** POST /payment/cancel, POST /invoice-cancellation; PaymentService.cancel; InvoiceCancellationService.processDebitNoteInvoice.
