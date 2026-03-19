# Billing Run Termination – Lock Cleanup and Resume After Cancel (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** Regression tests for lock cleanup when a billing run is terminated and for behaviour related to resuming or reusing a CANCELLED run (e.g. CANCELLED can become IN_PROGRESS_ACCOUNTING in some flows). This document ensures that terminating a run does not leave locks orphaned and that post-termination state is consistent.

**Scope:** When a billing run is terminated, any locks held by or associated with that run must be released (lock cleanup). If the system supports resuming or re-running a CANCELLED run (e.g. transitioning CANCELLED to IN_PROGRESS_ACCOUNTing or starting a new run), that flow must still work correctly after a run has been cancelled. This document covers lock cleanup on terminate and resume-after-cancel behaviour.

---

## Test data (preconditions)

- **Environment:** Test or Dev.
- **Billing run:** A billing run exists in a status that allows termination (e.g. IN_PROGRESS_GENERATION, GENERATED, or DRAFT) and may hold a lock (e.g. for generation or accounting).
- **Lock:** The system uses a Lock entity (or equivalent) that can be associated with a billing run; locks are expected to be cleaned up when the run is terminated.
- **Resume flow (if applicable):** The product may allow a CANCELLED run to be resumed or may create a new run; technical details mention "CANCELLED can become IN_PROGRESS_ACCOUNTING".

---

## TC-1 (Positive): Lock is released after billing run is terminated

**Objective:** Verify that when a billing run is successfully terminated (e.g. reaches CANCELLED status), any lock that was held by or associated with that billing run is released (deleted or marked as released). No orphaned lock should remain that would block other operations.

**Preconditions:**
1. A billing run exists in a status that holds a lock (e.g. IN_PROGRESS_GENERATION or a status that acquires a lock during processing).
2. The system has recorded a lock for this billing run (e.g. in a Lock table or equivalent).
3. The user has permission to terminate the run.

**Steps:**
1. Identify the billing run and confirm that a lock exists for it (e.g. via API or database query, if available and permitted).
2. Call PATCH /billing-run/terminate for this billing run (or trigger termination via UI).
3. Wait for termination to complete (billing run status becomes CANCELLED).
4. Check that the lock that was associated with this billing run is no longer held (e.g. lock record removed or status updated to released). Use the same method as in step 1.
5. Optionally verify that the same resource (e.g. same run type or scope) can be locked again by another operation if the product allows it.

**Expected result:** After termination completes, the lock that was associated with the billing run is released (cleaned up). No lock remains that would block other billing runs or operations. Lock cleanup is performed as part of terminate_billing_run or equivalent logic.

**References:** PDT-2023; what could break: lock cleanup on terminate; Lock; billing_run.terminate_billing_run(?).

---

## TC-2 (Positive): No lock remains when run is in "in progress termination"

**Objective:** Verify that when the billing run is in "in progress termination" status, the system either still holds a temporary lock to prevent concurrent modification or has already released the operational lock; and that once the run reaches CANCELLED, no persistent lock remains for normal operations.

**Preconditions:**
1. A billing run in an allowed status (e.g. GENERATED) has been sent a terminate request.
2. The run is currently in "in progress termination" status.

**Steps:**
1. Trigger termination for a billing run that holds a lock.
2. While the run is in "in progress termination", check lock state (if visible via API or DB).
3. After the run reaches CANCELLED, verify again that no lock remains for this run (or that the lock is released).
4. Confirm that no error occurs during termination due to lock handling (e.g. no "lock not found" or "failed to release lock" in logs if applicable).

**Expected result:** Lock handling during "in progress termination" is consistent: either the lock is released during the transition or immediately when CANCELLED is set. After CANCELLED, no operational lock remains. No regression in lock cleanup is observed.

**References:** PDT-2023; lock cleanup on terminate; new status "in progress termination".

---

## TC-3 (Positive): Resume or re-run after cancel – CANCELLED run does not block new or resumed run (if supported)

**Objective:** If the system supports resuming a CANCELLED run (e.g. CANCELLED can become IN_PROGRESS_ACCOUNTING) or starting a new run for the same scope, verify that after a run is terminated and reaches CANCELLED, the resume or new-run flow works correctly. Locks and state must not prevent the next run.

**Preconditions:**
1. The product supports either (a) resuming a CANCELLED run to a state like IN_PROGRESS_ACCOUNTING, or (b) creating a new billing run for the same scope after a previous run was CANCELLED.
2. A billing run was terminated and is now in CANCELLED status.
3. Locks for the cancelled run have been cleaned up (see TC-1, TC-2).

**Steps:**
1. Terminate a billing run and wait until its status is CANCELLED.
2. Verify lock cleanup (no orphaned lock for that run).
3. If the product supports "resume after cancel": trigger the resume action (e.g. button or API) for the CANCELLED run and check that it can transition to IN_PROGRESS_ACCOUNTING or the next allowed state without errors.
4. If the product supports "new run after cancel": create or start a new billing run for the same scope (e.g. same period or same customer set) and verify that it starts successfully (e.g. reaches INITIAL or IN_PROGRESS_DRAFT) and is not blocked by the previous CANCELLED run or by locks.
5. Observe that no error related to "already cancelled", "lock held", or "invalid state" occurs.

**Expected result:** After a run is CANCELLED and locks are cleaned up, the system allows either resuming that run (if supported) or starting a new run for the same scope. No regression: "resume after cancel" and "new run after cancel" work as designed. CANCELLED runs do not leave state that blocks subsequent runs.

**References:** PDT-2023; what could break: resume after cancel (CANCELLED can become IN_PROGRESS_ACCOUNTING); BillingRunService; lock cleanup.

---

## TC-4 (Negative): Terminate does not leave run in a state that holds a lock indefinitely

**Objective:** Verify that termination never leaves the billing run in a state where it still holds an operational lock indefinitely (e.g. stuck in "in progress termination" with a lock). This is a regression check for lock cleanup.

**Preconditions:**
1. A billing run in a lock-holding status (e.g. IN_PROGRESS_GENERATION) is terminated.
2. The system is observed until termination completes or a timeout is reached.

**Steps:**
1. Start termination for a billing run that holds a lock.
2. Wait for the run to reach CANCELLED (or "in progress termination" for a reasonable time, then CANCELLED).
3. If the run gets stuck in "in progress termination", document the behaviour and check lock state: the run must not hold the lock indefinitely (e.g. lock should be released or the run should eventually reach CANCELLED with lock released).
4. Verify that no other run or process is blocked by a lock still held by the terminated run.

**Expected result:** Termination either completes (run reaches CANCELLED) and releases the lock, or if there is a failure, the system does not leave the run holding the lock indefinitely. No regression where "terminate" leaves locks orphaned or held forever.

**References:** PDT-2023; what could break: lock cleanup on terminate.

---

## References

- **Jira:** PDT-2023 – Billing run termination; lock cleanup and resume after cancel.
- **What could break:** Lock cleanup on terminate; resume after cancel (CANCELLED can become IN_PROGRESS_ACCOUNTING).
- **Related:** Lock; BillingRunRepository; billing_run.terminate_billing_run(?); BillingRunService.cancel().
