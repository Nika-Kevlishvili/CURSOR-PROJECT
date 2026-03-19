# Billing Run Termination – Concurrent Start and Terminate (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** Regression tests for concurrent operations: starting (or resuming) a billing run while another request is terminating the same run, or multiple terminate requests. The system must behave consistently and not corrupt state or leave locks in an inconsistent state.

**Scope:** When a user (or system) starts or resumes a billing run at the same time that a terminate request is in progress (or vice versa), the system must handle the conflict safely. Similarly, concurrent terminate requests must not cause duplicate termination or inconsistent status. This document tests concurrent start/terminate and concurrent terminate behaviour.

---

## Test data (preconditions)

- **Environment:** Test or Dev.
- **Billing run:** A billing run exists in a status that allows both termination and possibly start/resume (e.g. DRAFT, GENERATED, or PAUSED).
- **Concurrent access:** Two clients (e.g. two API calls or UI and API) can act on the same billing run; or one user can trigger terminate while another operation (e.g. start generation or resume) is in progress.

---

## TC-1 (Positive): Single terminate request wins when start and terminate are concurrent

**Objective:** Verify that when one client sends a terminate request and another sends a start (or resume) request for the same billing run at nearly the same time, the system resolves the conflict in a defined way: either terminate wins and the run becomes CANCELLED (and start is rejected or no-op), or the system returns a clear error for one of the requests. No inconsistent state (e.g. run both "terminated" and "in progress") should occur.

**Preconditions:**
1. A billing run exists in a status that allows both start (or resume) and terminate (e.g. PAUSED or DRAFT).
2. Two clients or requests can be sent in quick succession (e.g. one to "start generation" or "resume", one to "terminate").

**Steps:**
1. Identify the billing run ID and ensure it is in an allowed status for both operations.
2. Send a terminate request (PATCH /billing-run/terminate) and almost simultaneously send a start or resume request (e.g. start generation or resume accounting) for the same run. Use two parallel API calls or two browser sessions if testing UI.
3. Observe the responses: one may succeed and the other fail, or both may be serialized so that one succeeds and the other gets a conflict or "invalid state" error.
4. After both requests complete, verify the final status of the billing run: it must be in exactly one well-defined state (e.g. CANCELLED or IN_PROGRESS_GENERATION, not both). Check that no lock is left in an inconsistent state.

**Expected result:** The system handles the concurrent requests without corrupting state. Either terminate wins (run ends as CANCELLED and the start/resume is rejected or has no effect) or the behaviour is clearly defined (e.g. "run is being terminated" returned for the start request). The billing run has a single, consistent final status. No duplicate termination and no orphaned lock.

**References:** PDT-2023; what could break: concurrent start/terminate.

---

## TC-2 (Negative): Second terminate request rejected when first is already in progress

**Objective:** Verify that if two terminate requests are sent for the same billing run in quick succession, the second request is rejected (e.g. 409 Conflict or 400 Bad Request) because the run is already "in progress termination" or already CANCELLED. Only one termination is applied.

**Preconditions:**
1. A billing run exists in an allowed status (e.g. GENERATED).
2. Two clients or two requests can call PATCH /billing-run/terminate with the same run ID.

**Steps:**
1. Send the first PATCH /billing-run/terminate request for the billing run.
2. Immediately (or before the first request completes) send a second PATCH /billing-run/terminate request for the same run ID.
3. Observe the response for the second request: it should be an error (e.g. 409 or 400) indicating that the run is already being terminated or cannot be terminated again.
4. Verify that the billing run is terminated only once (e.g. eventually in CANCELLED, with no duplicate "in progress termination" or duplicate cancellation records if applicable).

**Expected result:** The second terminate request is rejected. The error message or status code clearly indicates that termination is already in progress or that the run is not in a status that allows termination. The run reaches CANCELLED exactly once. No duplicate termination logic is executed.

**References:** PDT-2023; what could break: terminate button/API behaviour; new status "in progress termination".

---

## TC-3 (Negative): Start or resume rejected when run is already "in progress termination"

**Objective:** Verify that if a start or resume request (e.g. start generation, resume accounting) is sent for a billing run that is already in "in progress termination" status, the system rejects it with a clear error. The run must complete termination and become CANCELLED; it must not transition back to an in-progress state due to the concurrent start/resume.

**Preconditions:**
1. A billing run has been sent a terminate request and is currently in "in progress termination" status.
2. The product supports a "start" or "resume" operation that could theoretically be called for this run.

**Steps:**
1. Trigger termination for a billing run (e.g. DRAFT or GENERATED).
2. Before or while the run is in "in progress termination", send a start or resume request for the same run (e.g. "start generation" or "resume").
3. Observe the response: it should be an error (e.g. 400 or 409) indicating that the run cannot be started or resumed because it is being terminated (or similar).
4. Verify that the run still proceeds to CANCELLED and does not revert to IN_PROGRESS_GENERATION or another in-progress state due to the start/resume request.

**Expected result:** The start/resume request is rejected. The billing run remains in "in progress termination" and then becomes CANCELLED. No state corruption. The system treats "in progress termination" as a terminal path that cannot be overridden by start/resume.

**References:** PDT-2023; concurrent start/terminate; new status "in progress termination".

---

## References

- **Jira:** PDT-2023 – Billing run termination; concurrent operations.
- **What could break:** Concurrent start/terminate; terminate button/API behaviour.
- **Related:** BillingRunService.cancel(); PATCH /billing-run/terminate; Lock.
