# Billing Run Termination – Concurrent Operations and Lock Ordering (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** This document covers regression scenarios for concurrent start and terminate, ordering of terminate_billing_run and unlock, and double-terminate behaviour. It ensures that concurrent operations do not leave the system in an inconsistent state and that lock cleanup is reliable.

**Scope:** What could break: "Concurrent start and terminate"; "terminate_billing_run/unlock ordering"; "Lock cleanup in terminate_billing_run". The tests verify that when terminate is called, the run transitions correctly and locks are cleaned up in the right order; that concurrent start and terminate do not leave inconsistent status or locks; and that a second terminate request is rejected.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Billing run:** At least one billing run in an allowed status (e.g. GENERATED or PAUSED) that can be terminated.
- **Lock visibility:** If possible, a way to query locks by billing run ID (e.g. lock table with billing_id) to verify cleanup.
- **API access:** PATCH /billing-run/terminate and, if applicable, an endpoint or action that "starts" or resumes the run (for concurrent tests).

---

## TC-1 (Regression): Concurrent start and terminate – consistent final state

**Objective:** Verify that when a "start" (or resume) and a "terminate" are triggered at nearly the same time for the same billing run, the system reaches a consistent final state: either the run is terminated (CANCELLED) or it is in progress, but not both. No inconsistent status (e.g. IN_PROGRESS_* with termination flags) or orphaned locks should remain.

**Preconditions:**
1. A billing run exists in a status that allows both resume and terminate (e.g. PAUSED).
2. The tester can trigger resume (e.g. POST /billing-run/{id}/resume) and terminate (PATCH /billing-run/terminate) in quick succession or in parallel (e.g. two concurrent API calls or two threads in an integration test).
3. The system uses locking or transactional behaviour to serialize or resolve conflicts.

**Steps:**
1. From the same run in PAUSED (or equivalent), trigger both actions:
   - Thread A: call PATCH /billing-run/terminate for the run.
   - Thread B: call resume (e.g. POST /billing-run/{id}/resume) for the same run.
   Both calls can be sent as close together as possible (e.g. same second or within a short window).
2. Wait for both requests to complete and note the response of each (success or error).
3. Load the billing run and verify its final status: it must be either CANCELLED (terminate won) or an IN_PROGRESS_* status (resume won), but not an impossible combination (e.g. CANCELLED and IN_PROGRESS_GENERATION).
4. If one request fails, verify the error is clear (e.g. "termination in progress" or "run not in resumable state"). Verify that the run has no orphaned locks and that processStage (if applicable) is consistent with status.

**Expected result:** One of the two operations "wins". The final state is consistent: either the run is CANCELLED (and not in progress) or it is in an IN_PROGRESS_* status (and not cancelled). The other request fails with a clear error. No inconsistent state (e.g. status IN_PROGRESS_TERMINATION and processStage indicating active generation) and no duplicate lock cleanup or creation that leaves orphaned locks. This addresses "Concurrent start and terminate" from what_could_break.

**References:** PDT-2023; what_could_break – "Concurrent start and terminate".

---

## TC-2 (Regression): terminate_billing_run and unlock ordering – locks released after termination

**Objective:** Verify that the order of operations during termination is correct: the stored procedure terminate_billing_run (or equivalent) is invoked and performs lock cleanup (or that explicit unlock happens after or as part of termination) so that after the run is CANCELLED, no locks remain for this billing run. This addresses "terminate_billing_run/unlock ordering" and "Lock cleanup in terminate_billing_run".

**Preconditions:**
1. A billing run exists in an allowed status and has acquired locks (e.g. system locks linked to this billing run ID).
2. The system has a way to list or count locks by billing run ID (e.g. lock table with billing_id or equivalent).
3. Termination is implemented so that terminate_billing_run is called and/or explicit unlock is performed.

**Steps:**
1. Optionally note the number of locks for the billing run before termination (if the run holds locks at this stage).
2. Call PATCH /billing-run/terminate for the billing run and ensure the request succeeds and the run status becomes CANCELLED.
3. After the response and any asynchronous completion, query the lock table (or equivalent) for this billing run ID.
4. Verify that there are no locks left for this billing run (count = 0 or no rows). If the implementation documents that unlock happens inside terminate_billing_run, verify that the procedure is called (e.g. by logs or DB) and that after it runs, locks are gone.
5. If the implementation sets status to IN_PROGRESS_TERMINATION before calling the procedure, verify that lock cleanup happens before or when transitioning to CANCELLED so that the run never remains CANCELLED with leftover locks.

**Expected result:** After termination, the billing run has status CANCELLED and zero locks associated with it. The ordering of "terminate_billing_run" and status update (and any unlock) is correct so that locks are always released. No orphaned locks remain. This satisfies "terminate_billing_run/unlock ordering" and "Lock cleanup in terminate_billing_run".

**References:** PDT-2023; what_could_break – "Lock cleanup in terminate_billing_run", "terminate_billing_run/unlock ordering"; BillingRunLockAnalysis_TestEnvironment.md.

---

## TC-3 (Negative): Second terminate request while first is in progress or after CANCELLED is rejected

**Objective:** Verify that when the user or system sends a second terminate request (e.g. double-click or retry) either while the run is already IN_PROGRESS_TERMINATION or after it is CANCELLED, the API rejects the second request with a clear error and does not run termination logic again (no duplicate stored procedure call, no duplicate status update).

**Preconditions:**
1. A billing run in an allowed status (e.g. GENERATED), or a run already in IN_PROGRESS_TERMINATION or CANCELLED.
2. The terminate endpoint is available.
3. The caller has permission to terminate.

**Steps:**
1. First terminate: call PATCH /billing-run/terminate for the billing run. If the implementation returns quickly with status IN_PROGRESS_TERMINATION, proceed to step 2 before it becomes CANCELLED; otherwise use a run already in IN_PROGRESS_TERMINATION or CANCELLED.
2. Second terminate: call PATCH /billing-run/terminate again for the same billing run ID (simulating double-click or retry).
3. Observe the response of the second call: it must be an error (e.g. HTTP 400 or 409) with a message indicating that termination is not allowed (e.g. "Termination in progress" or "Billing run already cancelled").
4. Verify that the stored procedure terminate_billing_run was not called a second time for this run (e.g. by logs or by checking that lock count or DB state did not change in an unexpected way). Verify that the run status is still IN_PROGRESS_TERMINATION or CANCELLED.

**Expected result:** The second terminate request is rejected. The API returns an error and does not execute termination logic again. No duplicate stored procedure call, no duplicate status update, and no inconsistent lock state. This prevents "Terminate button/API expectations" and double-processing from breaking the flow.

**References:** PDT-2023; what_could_break – "Terminate button/API expectations"; TC-3 and TC-4 in API_terminate_flow.md.

---

## References

- **Jira:** PDT-2023 – Billing run termination; concurrent operations and lock ordering.
- **What could break:** Lock cleanup in terminate_billing_run; terminate_billing_run/unlock ordering; Concurrent start and terminate; Terminate button/API expectations.
- **Related:** BillingRunService.cancel(); stored procedure billing_run.terminate_billing_run; lock entity and billingId.
