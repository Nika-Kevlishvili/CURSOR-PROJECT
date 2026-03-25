# Invoice Cancellation SLP – Request Validation and ValidInvoice (PDT-2655)

**Jira:** PDT-2655 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** This document covers request validation and ValidInvoice checks for the invoice cancellation flow when applied to SLP (measured profile) invoices. The system must correctly build the validInvoiceMap and only process invoices that pass validation; invalid or already-processed invoices must be rejected with clear errors.

**Scope:** Entry flow: create cancellation request → findInvoiceToCancel → validInvoiceMap → processInvoice per row. For SLP, two invoices (two slots) must both appear in the cancel list and pass ValidInvoice validation. This document tests that validation does not incorrectly exclude SLP invoices, and that invalid requests (missing IDs, wrong state, duplicate) are rejected.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Product contract:** SLP flow contract with two measured price components (slot one, slot two).
- **Invoices:** Two invoices generated from the SLP profile; both in cancellable state unless otherwise stated.
- **Endpoint:** POST /invoice-cancellation; backend uses findInvoiceToCancel and ValidInvoice validation before processInvoice.

---

## TC-1 (Positive): ValidInvoice includes both SLP invoices when both are cancellable

**Objective:** Verify that when the cancellation request contains both SLP-generated invoice identifiers, findInvoiceToCancel returns both and the validInvoiceMap includes both, so that processInvoice is invoked for each row and both cancellations are created.

**Preconditions:**
1. Two SLP invoices exist and are in a state that allows cancellation (e.g. not cancelled, not reversed).
2. The request payload format is valid (e.g. invoice numbers or IDs as required by the API).

**Steps:**
1. Prepare a cancellation request with both SLP invoice numbers (or IDs) in the payload.
2. Call POST /invoice-cancellation with this payload.
3. Verify the response indicates success for both invoices (or a single success that implies both).
4. Verify (e.g. via API or database) that both invoices are in cancelled state and that two cancellation records exist (or one record covering both, as per design).

**Expected result:** Both invoices pass ValidInvoice validation. The system creates cancellations for both. No invoice is incorrectly excluded from the validInvoiceMap. The user sees a successful response.

**Actual result (if bug):** One or both invoices may be excluded from validInvoiceMap (e.g. due to profile type or slot logic), leading to only one cancellation or an error.

**References:** PDT-2655; validInvoiceMap; findInvoiceToCancel; processInvoice per row.

---

## TC-2 (Negative): Request rejected when payload is empty or missing invoice identifiers

**Objective:** Verify that when the user sends a request with no invoice numbers (or empty list), the system returns a validation error (e.g. 400 Bad Request) and does not create any cancellation or call processInvoice.

**Preconditions:**
1. User has access to POST /invoice-cancellation.

**Steps:**
1. Call POST /invoice-cancellation with an empty body, or with a payload that omits the required invoice identifier field(s) (e.g. empty array of invoice numbers).
2. Observe the response: status code and message.

**Expected result:** The system returns 400 Bad Request (or equivalent). The error message indicates that invoice identifiers are required or that the payload is invalid. No cancellation records are created. findInvoiceToCancel is not called with valid data (or returns empty); processInvoice is not invoked.

**References:** Request validation; API contract for /invoice-cancellation.

---

## TC-3 (Negative): Request rejected when invoice identifier format is invalid

**Objective:** Verify that when the user sends an invoice identifier in wrong format (e.g. non-numeric when numeric expected, or wrong length), the system returns a validation error and does not create a cancellation.

**Preconditions:**
1. User has access to POST /invoice-cancellation.

**Steps:**
1. Call POST /invoice-cancellation with an invoice identifier in invalid format (e.g. string "invalid", or wrong type).
2. Observe the response: status code and message.

**Expected result:** The system returns 400 Bad Request. The error message indicates invalid format or invalid identifier. No cancellation is created.

**References:** Request validation; ValidInvoice; findInvoiceToCancel.

---

## TC-4 (Negative): ValidInvoice excludes already-cancelled invoice

**Objective:** Verify that when one of the two SLP invoices has already been cancelled, the ValidInvoice logic (or equivalent) excludes it from the valid set, and the system either returns an error for that invoice or processes only the other invoice consistently (as per product design); no duplicate cancellation is created.

**Preconditions:**
1. Two SLP invoices exist; one has already been cancelled in a previous request.

**Steps:**
1. Call POST /invoice-cancellation with both invoice numbers (the cancelled one and the active one).
2. Observe the response and the state of both invoices after the request.

**Expected result:** The system does not create a second cancellation for the already-cancelled invoice. Either the request fails with a clear error (e.g. "Invoice already cancelled"), or the request succeeds only for the active invoice and the response or behaviour makes it clear that the cancelled one was skipped. No duplicate cancellation record for the same invoice.

**Actual result (if bug):** System may create duplicate cancellation or return success without excluding the already-cancelled invoice from processing.

**References:** PDT-2655; ValidInvoice validation; invalid state.

---

## TC-5 (Negative): ValidInvoice excludes invoice in invalid state (e.g. reversed or closed)

**Objective:** Verify that when an SLP invoice is in a state that does not allow cancellation (e.g. already reversed, or in a "closed" state), the system excludes it from validInvoiceMap and returns a clear error or skips it; no cancellation is created for that invoice.

**Preconditions:**
1. At least one SLP invoice exists in a state that does not allow cancellation (e.g. reversed, or state = CLOSED as per business rules).

**Steps:**
1. Call POST /invoice-cancellation with the identifier of that invoice (and optionally a valid second invoice).
2. Observe the response: status code and message.

**Expected result:** The system returns an error indicating that the invoice is not in a valid state for cancellation (e.g. "Invoice cannot be cancelled" or "Invalid state"). No cancellation record is created for that invoice. If a second valid invoice was included, behaviour is consistent (e.g. partial success or full reject as per design).

**References:** ValidInvoice validation; invoice state; processInvoice (STANDARD/SCALE).

---

## TC-6 (Positive): Mixed payload – one SLP invoice and one non-SLP invoice (if supported)

**Objective:** If the API supports cancelling multiple invoices from different sources in one request, verify that a request containing one SLP invoice and one non-SLP (e.g. standard profile) invoice is validated correctly and both are processed if they are in cancellable state; no SLP-specific validation incorrectly rejects the SLP invoice.

**Preconditions:**
1. One SLP-generated invoice and one non-SLP invoice exist and are both in cancellable state.
2. The product allows cancelling both in one request (if not, skip or adapt to two requests).

**Steps:**
1. Call POST /invoice-cancellation with both invoice numbers.
2. Verify both pass validation and both cancellations are created (or that the response clearly indicates which succeeded and which failed).

**Expected result:** Both invoices are validated and processed according to their profile type. No incorrect exclusion of the SLP invoice from validInvoiceMap. Both cancellations succeed.

**References:** validInvoiceMap; processInvoice; STANDARD vs SLP profile handling.

---

## References

- **Jira:** PDT-2655 – Invoice Cancelation does not work correctly for SLP profile.
- **Integration:** Create cancellation → findInvoiceToCancel → validInvoiceMap → processInvoice per row.
- **Related:** ValidInvoice validation; request validation; SLP two slots; invoice state.
