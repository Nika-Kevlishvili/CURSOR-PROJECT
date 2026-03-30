# Invoice Cancellation – Async Processing, Retries, and Document Generation (NT-1)

**Jira:** NT-1 (AI Experiments)  
**Type:** Bug  
**Summary:** Ensure invoice cancellation remains robust when parts of the flow are asynchronous (process/notification) and when document generation fails or is delayed, and verify correct partial-failure handling.

**Scope:** Cross-dependency data highlights process/notification and document generation as integration points for invoice cancellation. This document focuses on resilience: retries, duplicate submissions, and behaviour when downstream components fail (e.g. document generation service errors). It also verifies that the core cancellation result is either fully committed or cleanly rolled back, with clear user-visible outcomes.

---

## Test data (preconditions)

- **Environment:** Test (or as per NT-1).
- **User/permissions:** A user exists with permission to create invoice cancellations.
- **Invoice:** A paid invoice exists and is eligible for cancellation (not already cancelled).
- **Payment:** The invoice is linked to a payment.
- **Payment package:** The payment package can be set to **LOCKED** (primary NT-1 scenario) and **UNLOCKED** (baseline).
- **System observability:** You can verify whether the cancellation exists (UI, API read, or database), and whether any documents/notifications are produced (where applicable).

---

## TC-1 (Positive): Cancellation completes successfully and triggers downstream processing (paid invoice, LOCKED package)

**Objective:** Verify that, for the NT-1 scenario, cancellation succeeds and downstream processing (e.g. notifications/doc generation) is triggered as expected.

**Preconditions:**
1. Paid invoice exists and is eligible for cancellation.
2. Payment package lock status is **LOCKED**.

**Steps:**
1. Create invoice cancellation for the invoice.
2. Verify cancellation record exists and invoice reflects cancellation.
3. Verify downstream artefacts are triggered/created (e.g. notification record/event, generated cancellation document), if the product includes them.

**Expected result:** Cancellation succeeds and downstream processing is initiated/completed according to the product flow. No error referencing “lock status in UNLOCKED” is shown.

---

## TC-2 (Negative): Document generation fails, but cancellation outcome is well-defined (no silent partial failure)

**Objective:** Ensure the system handles a document-generation failure predictably and does not leave the invoice in an inconsistent state.

**Preconditions:**
1. Paid invoice exists; payment package is **LOCKED**.
2. Document generation can be forced to fail (e.g. by simulating service outage, invalid template configuration, or test toggle) in the test environment.

**Steps:**
1. Force document generation to fail for invoice cancellation (test setup).
2. Create invoice cancellation.
3. Verify whether the cancellation exists and check the user-visible response.

**Expected result:** The system responds in one of these acceptable ways (must be consistent with product design):
1. **Atomic behaviour:** Cancellation is not created and the user gets a clear error indicating document generation failed (no partial cancellation).
2. **Non-atomic but explicit behaviour:** Cancellation is created, but the system clearly indicates document generation failed and will be retried (e.g. cancellation status “Created, document pending/failed”), with a retry mechanism and no silent failure.

In both cases, the system must not return misleading lock-status errors for the invoice-cancellation flow.

---

## TC-3 (Positive): Retry after document generation failure eventually succeeds without duplicate cancellation

**Objective:** Ensure retries (automatic or manual) do not create duplicate cancellations and eventually produce the missing document/notification.

**Preconditions:**
1. TC-2 has been executed and resulted in either “cancellation created but document failed” or “cancellation rejected due to document failure”.
2. Document generation service is restored to healthy state.

**Steps:**
1. If cancellation was created but document failed: trigger the retry mechanism (e.g. scheduled retry, manual re-generate action, or re-run process).
2. If cancellation was rejected atomically: submit the cancellation request again after the service is restored.
3. Verify cancellation count and document presence.

**Expected result:** Exactly one invoice cancellation exists for the invoice. Document generation eventually succeeds (or the system clearly indicates permanent failure with appropriate error handling). No duplicate cancellations are created by retries.

---

## TC-4 (Negative): Duplicate/rapid submissions do not create duplicate cancellations (idempotency / concurrency)

**Objective:** Ensure that concurrent or repeated submissions (e.g. user double-clicks “Cancel”, or client retries due to timeouts) do not create multiple cancellation records.

**Preconditions:**
1. Paid invoice exists; payment package is **LOCKED**.

**Steps:**
1. Submit two cancellation requests for the same invoice in rapid succession (e.g. two API calls close together, or repeated UI action).
2. Verify persistence and responses.

**Expected result:** Only one cancellation is created. The second request is either idempotently successful (returns the existing cancellation) or rejected with a clear “already cancelled” message.

---

## TC-5 (Negative): Transient downstream failure does not return misleading “payment package not found in UNLOCKED” error

**Objective:** Ensure that failures in downstream steps (process/notification/document generation) are reported correctly and do not masquerade as the original NT-1 lock-status error.

**Preconditions:**
1. Paid invoice exists; payment package is **LOCKED**.
2. A transient downstream failure can be simulated (e.g. notification service timeout).

**Steps:**
1. Force a transient downstream failure.
2. Create invoice cancellation.
3. Observe the error message and verify system state.

**Expected result:** Any error message is accurate about the failing component (e.g. “document generation failed” or “notification timeout”) and does not mention a requirement for an UNLOCKED payment package. System state is either fully rolled back or explicitly marked for retry.

---

## References

- **Jira:** NT-1 – cancellation must work for paid invoice with locked payment package.
- **Integration points (given):** process/notification; document generation.
