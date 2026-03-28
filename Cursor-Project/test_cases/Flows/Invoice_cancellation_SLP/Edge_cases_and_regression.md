# Invoice Cancellation SLP – Edge Cases and Regression (PDT-2655)

**Jira:** PDT-2655 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** This document covers edge cases and regression scenarios for invoice cancellation when applied to SLP (measured profile) invoices: interim and debit/credit flows, billing run and SLP invoice creation consistency, and behaviour when both slots lead to two invoices and cancellation order or list composition varies. It also covers regression from "what could break" in the cross-dependency analysis.

**Scope:** Edge cases: two slots and two invoices; both profiles reverted by first cancel; interim flows; debit/credit flows. Regression: ValidInvoice validation; PROFILE revert (delete vs setInvoiced); one row per invoice vs per profile; billing run/SLP invoice creation; integration with findInvoiceToCancel and processInvoice. Ensure that existing STANDARD profile cancellation and other callers of the cancellation flow are not broken by SLP-specific logic.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Product contract:** SLP flow with two measured price components (slot one, slot two).
- **Billing run:** Capable of generating two invoices for the measured profile.
- **Related flows:** Interim billing; debit/credit note flows if they use the same cancellation or profile revert logic.

---

## TC-1 (Positive): findInvoiceToCancel returns both SLP invoices for the same profile

**Objective:** Verify that findInvoiceToCancel (or equivalent repository method) returns both invoices when the cancellation request references the contract or billing run that produced two SLP invoices (slot one and slot two). Both must be present in the list so that validInvoiceMap and processInvoice can handle both.

**Preconditions:**
1. Billing run has been executed; two invoices exist for the SLP profile (two slots).
2. The cancellation flow uses findInvoiceToCancel to resolve which invoices to cancel (e.g. by contract, billing run, or invoice IDs).

**Steps:**
1. Invoke the cancellation flow (e.g. POST /invoice-cancellation) with the appropriate identifiers so that the backend calls findInvoiceToCancel (e.g. with contract ID, billing run ID, or both invoice numbers).
2. Verify (e.g. via logs, debug, or outcome) that the list of invoices to cancel contains both SLP invoices (slot one and slot two).
3. Verify that both are then processed (validInvoiceMap includes both; processInvoice is called for each).

**Expected result:** findInvoiceToCancel returns both SLP-generated invoices. Both are included in the cancellation list and both are cancelled successfully. No invoice is missing from the list due to slot or profile filtering.

**Actual result (if bug):** findInvoiceToCancel may return only one invoice (e.g. one per profile instead of two per slots), so the second invoice is never cancelled or is left in active state.

**References:** PDT-2655; InvoiceCancellationInvoicesRepository.findInvoiceToCancel; SLP two slots: two invoices, both must be in cancel list.

---

## TC-2 (Negative): Cancellation when billing run or SLP invoice creation is incomplete

**Objective:** Verify that when only one of the two expected SLP invoices exists (e.g. billing run partially failed or one slot did not produce an invoice), the system cancels only the existing invoice and returns a clear outcome (e.g. success for one, or error if the request expected two). No crash or inconsistent state.

**Preconditions:**
1. Contract has SLP with two slots, but due to data or run state only one invoice was generated (e.g. one slot has no volume or run was partial).

**Steps:**
1. Call POST /invoice-cancellation with the one existing invoice number (or with both if the request allows partial success).
2. Verify the existing invoice is cancelled and no error occurs due to "missing second invoice" in business logic.
3. If the request included a non-existent second invoice ID, verify the system returns an error for that one and still processes the valid one (or rejects the whole request consistently).

**Expected result:** The system cancels the one existing invoice successfully. If the request included a non-existent invoice, the response clearly indicates which invoice was cancelled and which was invalid or missing. No inconsistent profile state (e.g. one slot reverted, one slot never invoiced).

**References:** Billing run/SLP invoice creation; edge case: only one invoice.

---

## TC-3 (Positive): Interim and debit/credit flows not broken by SLP cancellation

**Objective:** Regression: Verify that cancelling SLP invoices does not break interim billing or debit/credit note flows that share the same cancellation service or profile revert logic. After cancelling both SLP invoices, interim runs or debit/credit operations for the same contract (or other contracts) still work as expected.

**Preconditions:**
1. SLP contract with two invoices; both can be cancelled.
2. Interim billing or debit/credit flows use the same InvoiceCancellationService or profile revert (setInvoiced/delete).

**Steps:**
1. Cancel both SLP invoices (e.g. in one request or two).
2. Run an interim billing (or create a debit/credit note) for the same contract or for another contract that uses the same cancellation/revert logic.
3. Verify that the interim run or debit/credit flow completes without error and that profile state is consistent (e.g. no "already reverted" or missing row errors).

**Expected result:** SLP cancellation does not corrupt shared state. Interim and debit/credit flows continue to work. No regression in profile revert or invoice state for other flows.

**Actual result (if bug):** Shared state (e.g. one row per profile) may cause interim or debit/credit to fail after SLP cancellation, or vice versa.

**References:** Cross-dependency: interim and debit/credit flows; what could break.

---

## TC-4 (Negative): Request with two invoice IDs where one is not SLP (mixed)

**Objective:** Verify that when the user sends a request with two invoice identifiers where one is an SLP invoice and one is from a different profile (e.g. STANDARD), the system validates both and processes each according to its profile type. SLP invoice is cancelled with correct revert (per slot); non-SLP invoice is cancelled with its own revert logic. No cross-contamination (e.g. STANDARD revert logic applied to SLP slot).

**Preconditions:**
1. One SLP-generated invoice (e.g. slot one) and one STANDARD (or other) profile invoice exist and are both in cancellable state.

**Steps:**
1. Call POST /invoice-cancellation with both invoice numbers.
2. Verify both are cancelled; verify SLP slot revert is applied only to the SLP invoice and STANDARD (or other) revert is applied to the other. No incorrect revert on the wrong profile type.

**Expected result:** Both invoices are cancelled. Profile revert is applied correctly per invoice type (SLP per slot; STANDARD/other per its logic). No wrong revert (e.g. SLP revert applied to STANDARD row or vice versa).

**References:** processInvoice (STANDARD PROFILE/SCALE); SLP vs STANDARD; integration points.

---

## TC-5 (Positive): Order of cancellation (slot one first vs slot two first) both succeed

**Objective:** Verify that regardless of whether the user cancels slot-one invoice first or slot-two invoice first (when doing two separate requests), both sequences succeed and revert is applied correctly for each slot. No dependency on order.

**Preconditions:**
1. Two SLP invoices (slot one and slot two) in cancellable state.

**Steps:**
1. In one test run: cancel slot-one invoice first, then slot-two. Verify both succeed and both slots are reverted correctly.
2. In another test run (or with fresh data): cancel slot-two invoice first, then slot-one. Verify both succeed and both slots are reverted correctly.

**Expected result:** Both orders (slot one → slot two and slot two → slot one) result in both invoices cancelled and both slots reverted. No "second cancel fails" depending on order.

**Actual result (if bug):** One order may work and the other may fail (e.g. if revert logic assumes slot one is always first).

**References:** PDT-2655; two slots; both must be in cancel list; reverting first may revert both profiles.

---

## TC-6 (Negative): Double-revert prevented – same invoice cancelled twice

**Objective:** Verify that when the user attempts to cancel the same SLP invoice twice (e.g. two requests with the same invoice number), the second request fails with a clear "already cancelled" or "invalid state" error and does not perform a second profile revert (no double-revert). Profile row is updated only once.

**Preconditions:**
1. One SLP invoice (e.g. slot one) has been cancelled in a previous request.

**Steps:**
1. Call POST /invoice-cancellation again with the same invoice number.
2. Verify the response is an error (e.g. 400 or 409); verify in the database that the profile row for that slot was not updated again (e.g. setInvoiced not toggled twice).

**Expected result:** Second request fails. No duplicate cancellation record. No second revert on the same profile/slot row.

**References:** ValidInvoice; already cancelled; double-revert; PROFILE revert (delete vs setInvoiced).

---

## TC-7 (Positive): STANDARD profile cancellation still works (regression)

**Objective:** Regression: Verify that cancelling an invoice from a STANDARD (non-SLP) profile still works after any changes for SLP. POST /invoice-cancellation with a STANDARD invoice identifier succeeds; processInvoice for STANDARD profile runs correctly; no regression for existing callers.

**Preconditions:**
1. A contract with STANDARD (non-SLP) billing profile exists and has generated at least one invoice in cancellable state.

**Steps:**
1. Call POST /invoice-cancellation with the STANDARD invoice number.
2. Verify the cancellation succeeds and the STANDARD profile revert (e.g. setInvoiced or delete) is applied correctly. No error introduced by SLP-specific code paths.

**Expected result:** STANDARD invoice cancellation succeeds. Existing behaviour is unchanged. No regression for STANDARD profile.

**References:** processInvoice (STANDARD PROFILE/SCALE); integration; what could break.

---

## References

- **Jira:** PDT-2655 – Invoice Cancelation does not work correctly for SLP profile.
- **What could break:** ValidInvoice validation; PROFILE revert (delete vs setInvoiced); one row per invoice vs per profile; billing run/SLP invoice creation; interim and debit/credit flows.
- **Integration:** Create cancellation → findInvoiceToCancel → validInvoiceMap → processInvoice per row; SLP two slots: two invoices, both must be in cancel list; reverting first may revert both profiles, second then fails or double-reverts.
