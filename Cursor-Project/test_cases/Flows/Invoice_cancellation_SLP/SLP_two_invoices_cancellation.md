# Invoice Cancellation – SLP Profile Two Invoices (PDT-2655)

**Jira:** PDT-2655 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** Invoice cancellation does not work correctly when the user cancels invoices that were generated from an SLP (Standard / measured) profile. For SLP flow with two "измерено" (measured) price components in different slots, the billing run produces two invoices; cancelling both must work correctly. Currently cancellation fails or behaves incorrectly.

**Scope:** This document covers the main scenario where a product contract uses the SLP flow with a product that has two measured price components (slot one and slot two). A billing run generates two invoices for this single measured profile. The user cancels both invoices. The expected behaviour is that both cancellations succeed and the system correctly reverts profile state (e.g. setInvoiced or delete) for both slots. The bug is that cancellation does not work correctly when applied to these SLP-generated invoices (e.g. only one cancellation succeeds, or revert logic affects both profiles when cancelling the first, so the second fails or double-reverts).

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Product contract:** A product contract exists for the SLP flow (standard/measured billing profile).
- **Product:** The product has two "измерено" (measured) price components: one in slot one and one in slot two.
- **Billing run:** A billing run has been started for this contract.
- **Invoices:** Two invoices have been generated for the single measured profile (one per slot or per component, as per SLP logic).
- **Invoice cancellation endpoint:** POST /invoice-cancellation is available; findInvoiceToCancel and processInvoice (STANDARD/SCALE profile) are used.

---

## TC-1 (Positive): Cancel both SLP-generated invoices in one request (happy path)

**Objective:** Verify that when both invoices generated from the SLP profile (slot one and slot two) are included in the cancellation request, the system creates cancellations for both and correctly reverts profile state for both slots without leaving one invoice uncancelled or double-reverting.

**Preconditions:**
1. A product contract for SLP flow exists with a product that has two measured price components (slot one and slot two).
2. A billing run has been executed; two invoices have been generated for this measured profile.
3. Both invoices are in a state that allows cancellation (e.g. not already cancelled).
4. findInvoiceToCancel returns both invoices when given the appropriate identifiers (e.g. invoice numbers or IDs).

**Steps:**
1. Obtain the invoice numbers (or IDs) for both SLP-generated invoices (e.g. from billing run result or API).
2. Call POST /invoice-cancellation with a payload that includes both invoice numbers (or both invoices in the list to cancel).
3. Observe the response: status code and body.
4. Verify that two cancellation records are created (or that the response indicates both invoices were cancelled).
5. Verify that profile revert was applied correctly for both slots (e.g. setInvoiced reverted or profile rows handled once per invoice, not once per profile in a way that breaks the second cancel).

**Expected result:** The request succeeds (e.g. HTTP 200 or 201). Both invoices are cancelled. The system creates cancellation records for both. Profile revert is applied correctly for slot one and slot two (no double-revert, no "already reverted" error on the second). The user can confirm both invoices are in cancelled state.

**Actual result (if bug):** Cancellation does not work correctly: e.g. only one invoice is cancelled, or the first cancel reverts both profiles so the second cancel fails or double-reverts; or the system returns an error when cancelling the second invoice.

**References:** PDT-2655; InvoiceCancellationProcessService; findInvoiceToCancel; processInvoice (STANDARD/SCALE); SLP two slots.

---

## TC-2 (Positive): Cancel first SLP invoice then second in two separate requests (sequential)

**Objective:** Verify that when the user cancels the first SLP-generated invoice and then the second in two separate API calls, both cancellations succeed and profile revert is applied correctly for each (one row per invoice; reverting the first does not incorrectly revert or lock the second).

**Preconditions:**
1. Same as TC-1: two invoices generated from SLP profile (slot one and slot two).
2. Both invoices are in cancellable state.

**Steps:**
1. Call POST /invoice-cancellation with only the first invoice number (e.g. the one for slot one).
2. Verify the first cancellation succeeds and profile state for slot one is reverted as expected.
3. Call POST /invoice-cancellation with the second invoice number (e.g. the one for slot two).
4. Verify the second cancellation succeeds; profile state for slot two is reverted; no error due to "already reverted" or missing row.

**Expected result:** First request succeeds; first invoice is cancelled and its profile/slot is reverted. Second request succeeds; second invoice is cancelled and its profile/slot is reverted. No double-revert and no failure on the second cancel due to shared profile handling.

**Actual result (if bug):** Second cancellation fails (e.g. because first cancel reverted both profiles) or second cancel causes double-revert or inconsistent state.

**References:** PDT-2655; one row per invoice vs per profile; PROFILE revert (delete vs setInvoiced).

---

## TC-3 (Negative): Cancellation with only one of the two SLP invoices in the request

**Objective:** Verify that when the user sends a cancellation request that includes only one of the two SLP-generated invoices, the system cancels that one correctly and does not attempt to cancel or revert the other invoice; the second invoice remains active and can be cancelled in a subsequent request.

**Preconditions:**
1. Two SLP-generated invoices exist; both are active (not cancelled).

**Steps:**
1. Call POST /invoice-cancellation with only one invoice number (e.g. slot one).
2. Verify the response indicates success for that invoice.
3. Verify via API or database that the other invoice is still active (not cancelled).
4. Optionally call POST /invoice-cancellation again with the second invoice number and verify it still cancels successfully.

**Expected result:** The first request cancels only the specified invoice. The second invoice remains in active state. A subsequent request with the second invoice number cancels it without error.

**Actual result (if bug):** System may incorrectly revert both profiles when cancelling one invoice, or the second invoice may become uncancellable.

**References:** PDT-2655; validInvoiceMap; processInvoice per row.

---

## TC-4 (Negative): Cancellation when one invoice is already cancelled

**Objective:** Verify that when one of the two SLP invoices has already been cancelled and the user requests cancellation for both (or for the already-cancelled one), the system returns a clear error for the already-cancelled invoice and does not create a duplicate cancellation or corrupt state.

**Preconditions:**
1. Two SLP-generated invoices exist.
2. One of them has already been cancelled (e.g. via a previous request).

**Steps:**
1. Call POST /invoice-cancellation with both invoice numbers (including the already-cancelled one), or with only the already-cancelled invoice number.
2. Observe the response: status code and message.

**Expected result:** The system returns an error (e.g. 400 or 409) indicating that the invoice is already cancelled or not in a valid state for cancellation. No duplicate cancellation record is created. The other invoice (if included and not yet cancelled) is either cancelled or the whole request is rejected consistently (as per product design).

**Actual result (if bug):** System may accept the request and create duplicate cancellation, or return a misleading error.

**References:** PDT-2655; ValidInvoice validation; invalid state.

---

## TC-5 (Negative): Cancellation with non-existent or invalid invoice identifier

**Objective:** Verify that when the user sends an invoice number (or ID) that does not exist or does not belong to an SLP-generated invoice in cancellable state, the system returns a clear validation or "not found" error and does not create any cancellation.

**Preconditions:**
1. User has access to POST /invoice-cancellation.
2. At least one valid SLP contract and invoices may exist in the system (optional).

**Steps:**
1. Call POST /invoice-cancellation with a non-existent invoice number (e.g. "INV-999999") or invalid format.
2. Observe the response: status code and message.

**Expected result:** The system returns an error (e.g. 400 Bad Request or 404 Not Found). No cancellation record is created. The error message clearly indicates that the invoice was not found or is invalid.

**References:** Request validation; ValidInvoice; findInvoiceToCancel.

---

## References

- **Jira:** PDT-2655 – [Backend] Invoice Cancelation does not work correctly, when user cancel invoice which is generated from SLP profile.
- **Entry points:** POST /invoice-cancellation; InvoiceCancellationProcessService; InvoiceCancellationService.processInvoice (STANDARD/SCALE); InvoiceCancellationInvoicesRepository.findInvoiceToCancel.
- **Related:** SLP billing run; two measured price components; slot one and slot two; profile revert (delete vs setInvoiced); one row per invoice vs per profile.
