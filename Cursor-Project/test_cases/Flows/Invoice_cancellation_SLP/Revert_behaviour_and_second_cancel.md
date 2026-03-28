# Invoice Cancellation SLP – Revert Behaviour and Second Cancel (PDT-2655)

**Jira:** PDT-2655 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** When cancelling SLP-generated invoices (two invoices for two slots), the profile revert logic (e.g. delete vs setInvoiced) must apply correctly per invoice. Reverting the first invoice must not cause the second cancel to fail or to double-revert the same profile row. This document tests revert behaviour and the scenario where the user cancels the first invoice and then the second.

**Scope:** PROFILE revert: delete vs setInvoiced; one row per invoice vs per profile. Risk: reverting the first invoice may revert both profiles (if logic is per profile), so the second cancel then fails or double-reverts. Tests cover correct revert for each slot, and rejection/consistency when the second invoice is cancelled after the first.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Product contract:** SLP flow with two measured price components (slot one and slot two).
- **Billing run:** Executed; two invoices generated for the measured profile.
- **Backend:** InvoiceCancellationService.processInvoice (STANDARD/SCALE); profile revert uses delete or setInvoiced as per implementation.

---

## TC-1 (Positive): Revert applies to correct slot when cancelling first invoice only

**Objective:** Verify that when the user cancels only the first SLP invoice (e.g. slot one), the profile revert (e.g. setInvoiced flag or profile row update) is applied only for the slot or row associated with that invoice, and the second invoice and its profile/slot remain unchanged and still cancellable.

**Preconditions:**
1. Two SLP invoices exist (slot one and slot two).
2. Both are in cancellable state.

**Steps:**
1. Call POST /invoice-cancellation with only the first invoice number (slot one).
2. Verify the first invoice is cancelled.
3. Verify profile/slot one is reverted (e.g. setInvoiced cleared or corresponding row updated) and profile/slot two is not reverted (still invoiced or in expected state).
4. Verify the second invoice is still active and can be cancelled in a subsequent request.

**Expected result:** First cancellation succeeds. Revert affects only the slot/row for the first invoice. Second invoice and its slot remain in correct state; the user can cancel the second invoice in a follow-up request without error.

**Actual result (if bug):** Revert may apply to both slots (e.g. one row per profile instead of per invoice), so the second invoice becomes uncancellable or the second cancel causes double-revert.

**References:** PDT-2655; PROFILE revert (delete vs setInvoiced); one row per invoice vs per profile.

---

## TC-2 (Positive): Revert applies to correct slot when cancelling second invoice after first

**Objective:** Verify that when the user has already cancelled the first SLP invoice and then cancels the second, the system applies revert only to the second invoice’s slot/row. No double-revert on the first slot; no failure due to "already reverted" for the second slot.

**Preconditions:**
1. First SLP invoice (slot one) has already been cancelled and its profile/slot reverted.
2. Second SLP invoice (slot two) is still active.

**Steps:**
1. Call POST /invoice-cancellation with the second invoice number (slot two).
2. Verify the response indicates success.
3. Verify the second invoice is cancelled and only the slot two profile/row is reverted; slot one is not touched again (no double-revert).

**Expected result:** Second cancellation succeeds. Revert is applied only to slot two. No error such as "already reverted" or "profile not found" for slot two. Slot one remains in its post-first-cancel state.

**Actual result (if bug):** Second cancel fails (e.g. because first cancel reverted both profiles) or second cancel causes double-revert on a shared profile row.

**References:** PDT-2655; SLP two slots; both must be in cancel list; reverting first may revert both profiles.

---

## TC-3 (Negative): Second cancel fails or returns clear error when profile was already reverted by first cancel (current bug scenario)

**Objective:** Document the current bug: if the implementation reverts both profile rows when cancelling the first invoice (e.g. one row per profile), then when the user attempts to cancel the second invoice, the system may fail because the profile for slot two was already reverted, or may attempt a double-revert. Verify the actual behaviour and that the expected behaviour is: revert per invoice/slot so that the second cancel succeeds.

**Preconditions:**
1. Two SLP invoices; implementation currently may revert both profiles when cancelling the first (bug).

**Steps:**
1. Cancel the first SLP invoice via POST /invoice-cancellation.
2. Attempt to cancel the second SLP invoice via POST /invoice-cancellation.
3. Observe: does the second request succeed or fail? If it fails, record the error message and state. If it succeeds, verify that no double-revert occurred (e.g. profile row not updated twice).

**Expected result (desired):** Second cancel succeeds; revert is applied per invoice/slot. No double-revert.

**Actual result (if bug):** Second cancel fails with an error (e.g. "Profile already reverted" or "No row to update") or succeeds but causes inconsistent state (double-revert). This test case captures the current bug for regression.

**References:** PDT-2655; what could break: "reverting first may revert both profiles, second then fails or double-reverts".

---

## TC-4 (Positive): Both invoices cancelled in one request – revert applied once per invoice

**Objective:** Verify that when both SLP invoices are cancelled in a single request, the system applies revert exactly once per invoice (once for slot one, once for slot two). No shared revert that leaves the second slot unreverted; no double-revert on either slot.

**Preconditions:**
1. Two SLP invoices in cancellable state.

**Steps:**
1. Call POST /invoice-cancellation with both invoice numbers in one payload.
2. Verify both cancellations are created.
3. Verify profile/slot one is reverted exactly once and profile/slot two is reverted exactly once (e.g. setInvoiced cleared for both, or two distinct rows updated, not one row updated twice).

**Expected result:** Both invoices cancelled; revert applied once per invoice/slot. Data consistency: no duplicate updates, no missing revert for the second slot.

**Actual result (if bug):** Only one revert may be applied (e.g. one row per profile), or one slot may be reverted twice.

**References:** PDT-2655; one row per invoice vs per profile; processInvoice per row.

---

## TC-5 (Negative): Idempotency – duplicate request with same two invoices returns error or no-op

**Objective:** Verify that if the user sends a second cancellation request with the same two already-cancelled SLP invoices, the system does not create duplicate cancellation records and does not attempt to revert the same profile rows again; it returns an error (e.g. already cancelled) or idempotent success.

**Preconditions:**
1. Both SLP invoices have already been cancelled in a previous request.

**Steps:**
1. Call POST /invoice-cancellation again with the same two invoice numbers.
2. Observe the response and the database/API state (cancellation records, profile rows).

**Expected result:** The system returns an error indicating that the invoice(s) are already cancelled, or returns success without creating new cancellation records or re-running revert. No double-revert; no duplicate cancellation records.

**References:** ValidInvoice; already cancelled; idempotency.

---

## References

- **Jira:** PDT-2655 – Invoice Cancelation does not work correctly for SLP profile.
- **What could break:** PROFILE revert (delete vs setInvoiced); one row per invoice vs per profile; reverting first may revert both profiles, second then fails or double-reverts.
- **Related:** InvoiceCancellationService.processInvoice; SLP two slots; billing run/SLP invoice creation.
