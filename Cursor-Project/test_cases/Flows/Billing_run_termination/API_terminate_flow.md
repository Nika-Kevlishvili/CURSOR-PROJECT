# Billing Run Termination – API and Cancel Flow (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** The termination flow is triggered via PATCH /billing-run/terminate and BillingRunService.cancel(). This document covers success and failure scenarios for the terminate API, status transitions (to IN_PROGRESS_TERMINATION and CANCELLED), lock cleanup, and invalid requests.

**Scope:** Entry points PATCH /billing-run/terminate and BillingRunService.cancel(). Termination is allowed only when the billing run status is in the set INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED. The run transitions to IN_PROGRESS_TERMINATION (and then to CANCELLED) or directly to CANCELLED as per implementation. The stored procedure terminate_billing_run is called; lock cleanup must occur. Invalid IDs and disallowed statuses must be rejected.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Billing run (terminatable):** At least one billing run exists in status GENERATED, PAUSED, DRAFT, INITIAL, IN_PROGRESS_DRAFT, or IN_PROGRESS_GENERATION.
- **Billing run (cancelled):** A billing run that has already been terminated (status CANCELLED).
- **Billing run (in progress termination):** If the implementation exposes this state, a run in IN_PROGRESS_TERMINATION (e.g. during the short window between terminate call and completion).
- **API access:** Caller has permission to call PATCH /billing-run/terminate (or equivalent) and the backend uses BillingRunService.cancel().

---

## TC-1 (Positive): PATCH /billing-run/terminate succeeds when billing run is in an allowed status

**Objective:** Verify that calling PATCH /billing-run/terminate (or the equivalent terminate endpoint) with a valid billing run ID and with the run in one of the allowed statuses (INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED) results in success and the run is terminated (status moves to IN_PROGRESS_TERMINATION and then CANCELLED, or directly to CANCELLED as per implementation).

**Preconditions:**
1. A billing run exists with ID known to the tester and its status is one of: INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, or PAUSED.
2. The caller has permission to terminate billing runs.
3. The terminate endpoint is available (e.g. PATCH /billing-run/terminate with billing run ID in path or body).

**Steps:**
1. Note the current status of the billing run (e.g. via GET billing run by ID).
2. Call PATCH /billing-run/terminate with the billing run ID (e.g. in path: PATCH /billing-run/{id}/terminate or as specified by the API).
3. Assert the response indicates success (e.g. HTTP 200 or 204).
4. Call GET billing run by ID again and verify the status is either IN_PROGRESS_TERMINATION (if exposed) or CANCELLED.
5. If the implementation first sets IN_PROGRESS_TERMINATION, after a short wait or after the stored procedure completes, verify the status is CANCELLED.

**Expected result:** The request succeeds. The billing run status transitions to IN_PROGRESS_TERMINATION (if applicable) and then to CANCELLED, or directly to CANCELLED. The response does not return an error. The run is no longer in a terminatable state for a second call.

**References:** PDT-2023; entry_points PATCH /billing-run/terminate; BillingRunService.cancel().

---

## TC-2 (Positive): Status transition to IN_PROGRESS_TERMINATION then CANCELLED (or direct to CANCELLED)

**Objective:** Verify that after a successful terminate call, the billing run ends in status CANCELLED and, if the implementation uses it, passes through IN_PROGRESS_TERMINATION so that other systems (UI, schedulers, resume) see the intermediate state and do not allow conflicting actions.

**Preconditions:**
1. A billing run exists in an allowed status (e.g. GENERATED or PAUSED).
2. API or database access to read the billing run status before and after the call.

**Steps:**
1. Call PATCH /billing-run/terminate for the billing run.
2. Immediately after (or in the same response), read the billing run status. If the implementation sets IN_PROGRESS_TERMINATION first, the status should be IN_PROGRESS_TERMINATION.
3. After the termination logic completes (e.g. stored procedure terminate_billing_run and status update), read the status again. It must be CANCELLED.
4. Optionally check the database for the billing_run record and confirm status = CANCELLED and that no inconsistent state exists (e.g. processStage cleared if required).

**Expected result:** The run eventually has status CANCELLED. If the design includes IN_PROGRESS_TERMINATION, that status appears transiently so that the UI can disable the terminate button and schedulers can exclude the run. The final state is CANCELLED only.

**References:** PDT-2023; integration_points – "status CANCELLED after termination"; BillingStatus IN_PROGRESS_TERMINATION and CANCELLED.

---

## TC-3 (Negative): PATCH /billing-run/terminate returns error when status is already IN_PROGRESS_TERMINATION

**Objective:** Verify that if the run is already in IN_PROGRESS_TERMINATION (e.g. a previous terminate request is being processed), a second call to PATCH /billing-run/terminate is rejected with a clear error so that duplicate termination is not triggered.

**Preconditions:**
1. A billing run exists and is in status IN_PROGRESS_TERMINATION (e.g. first terminate call has set this status and the run has not yet transitioned to CANCELLED, or the run was set to this status for test).
2. The terminate endpoint is available.

**Steps:**
1. Call PATCH /billing-run/terminate with the ID of the billing run that is in IN_PROGRESS_TERMINATION.
2. Observe the response: status code and body.
3. Verify that the billing run status remains IN_PROGRESS_TERMINATION or has transitioned to CANCELLED (but that no duplicate processing or error on the backend occurs).

**Expected result:** The API returns an error (e.g. HTTP 400 or 409) with a message indicating that termination is already in progress or that the run cannot be terminated in its current state. No second termination flow is executed. The run is not left in an inconsistent state.

**References:** PDT-2023; what_could_break – "Terminate button/API expectations".

---

## TC-4 (Negative): PATCH /billing-run/terminate returns error when status is already CANCELLED

**Objective:** Verify that calling PATCH /billing-run/terminate for a billing run that is already CANCELLED returns an error and does not attempt to run termination again.

**Preconditions:**
1. A billing run exists and its status is CANCELLED (already terminated).
2. The terminate endpoint is available.

**Steps:**
1. Call PATCH /billing-run/terminate with the ID of the cancelled billing run.
2. Observe the response: status code and message.
3. Verify that the run status remains CANCELLED and that no side effects (e.g. duplicate stored procedure call, lock changes) occur.

**Expected result:** The API returns an error (e.g. HTTP 400 or 409) with a message such as "Termination available only for followed statuses" or "Billing run is already cancelled". The run remains CANCELLED. No duplicate termination logic runs.

**References:** PDT-2023; BillingStatus.availableStatusesForTermination does not include CANCELLED.

---

## TC-5 (Negative): PATCH /billing-run/terminate returns 404 or error for non-existent billing run ID

**Objective:** Verify that when the client sends a terminate request with a non-existent billing run ID, the API returns 404 Not Found (or an equivalent error) and no termination logic is executed.

**Preconditions:**
1. A billing run ID that does not exist in the system (e.g. a large number that is not used, or an ID from another environment).
2. The terminate endpoint is available.

**Steps:**
1. Call PATCH /billing-run/terminate with the non-existent billing run ID (e.g. PATCH /billing-run/99999999/terminate).
2. Observe the response: status code and body.
3. Confirm that no billing run is created or updated and that no stored procedure is run for this ID.

**Expected result:** The API returns 404 Not Found (or 400 with a clear "billing run not found" message). No billing run is updated. No stored procedure terminate_billing_run is called for this ID.

**References:** PDT-2023; validation – invalid or missing ID.

---

## TC-6 (Negative): PATCH /billing-run/terminate returns error when status is COMPLETED

**Objective:** Verify that termination is not allowed for a billing run that is already COMPLETED; the API must reject the request with a clear error so that only runs in the defined allowed status set can be terminated.

**Preconditions:**
1. A billing run exists and its status is COMPLETED.
2. The terminate endpoint is available.

**Steps:**
1. Call PATCH /billing-run/terminate with the ID of the completed billing run.
2. Observe the response: status code and message.
3. Verify that the run status remains COMPLETED and that no termination logic (e.g. terminate_billing_run) is executed.

**Expected result:** The API returns an error (e.g. HTTP 400) with a message indicating that termination is not allowed for this status (e.g. "Termination available only for followed statuses: [...]"). The run remains COMPLETED. No locks are released by termination for this run.

**References:** PDT-2023; availableStatusesForTermination does not include COMPLETED.

---

## TC-7 (Positive): BillingRunService.cancel() performs full termination and sets final status CANCELLED

**Objective:** Verify that when the API invokes BillingRunService.cancel(billingRunId, mustCheckPermission), the service checks that the run is in an allowed status, sets status to IN_PROGRESS_TERMINATION (if used) and then to CANCELLED, calls the stored procedure terminate_billing_run, and persists the run so that the run is fully terminated.

**Preconditions:**
1. A billing run exists in an allowed status (e.g. GENERATED).
2. Access to trigger cancel (e.g. via API that calls cancel(), or unit/integration test that calls the service).

**Steps:**
1. Invoke BillingRunService.cancel(billingRunId, true) for the billing run (e.g. via the terminate API or test).
2. Load the billing run from the repository and verify status is CANCELLED.
3. Optionally verify that the stored procedure billing_run.terminate_billing_run was invoked (e.g. by checking that locks for this run are cleaned up or by observing DB/logger).
4. Verify that a second call to cancel() for the same run throws or returns an error (run no longer in allowed status).

**Expected result:** cancel() transitions the run to CANCELLED (possibly via IN_PROGRESS_TERMINATION). The stored procedure terminate_billing_run is called. The run is saved. A second call to cancel() for the same run is rejected because the status is no longer in availableStatusesForTermination.

**References:** PDT-2023; BillingRunService.cancel(); entry_points.

---

## TC-8 (Regression): Lock cleanup after termination – no orphaned locks for the billing run

**Objective:** Verify that after a successful termination, any locks associated with the billing run are released (by the stored procedure terminate_billing_run or by explicit unlock logic) so that there are no orphaned locks and "what could break – Lock cleanup in terminate_billing_run" is addressed.

**Preconditions:**
1. A billing run exists in an allowed status and has acquired locks (e.g. during draft or generation).
2. The system has a way to query locks by billing run ID (e.g. lock table with billing_id or equivalent).
3. Termination has been executed successfully for this run.

**Steps:**
1. Before termination, optionally record the number of locks (or lock records) associated with the billing run ID.
2. Call PATCH /billing-run/terminate for the billing run and ensure it succeeds.
3. After termination completes (status = CANCELLED), query the lock table (or equivalent) for this billing run ID.
4. Verify that no locks remain for this billing run (count = 0 or no rows), or that the stored procedure / implementation explicitly removes them.

**Expected result:** After termination, the billing run has no remaining locks associated with it. Lock cleanup is performed (by terminate_billing_run or by explicit unlock). No orphaned locks remain for the cancelled run.

**References:** PDT-2023; what_could_break – "Lock cleanup in terminate_billing_run"; BillingRunLockAnalysis_TestEnvironment.md; BugValidation_BillingRunLockUnlock.md.

---

## References

- **Jira:** PDT-2023 – Billing run termination (disable button, new status IN_PROGRESS_TERMINATION).
- **Entry points:** PATCH /billing-run/terminate, BillingRunService.cancel().
- **Integration:** cancel() → terminate_billing_run → status CANCELLED; lock cleanup must occur.
- **BillingStatus:** availableStatusesForTermination; IN_PROGRESS_TERMINATION; CANCELLED.
