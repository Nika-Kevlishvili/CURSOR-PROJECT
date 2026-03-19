# POD update existing – Concurrency and locking (PHN-2160)

**Jira:** PHN-2160 (Phoenix)  
**Type:** Task  
**Summary:** Validate stability and correctness of updating an existing POD under concurrency: simultaneous edits, retries, lock ordering, and integration risks where POD updates intersect with contract–POD flows and identifier-based checks.

**Scope:** This document covers concurrent update scenarios that can cause lost updates, deadlocks, inconsistent reads, and UI/API confusion. The expected behaviour is that the system either (a) prevents concurrent writes via locking/versioning and returns clear conflict errors, or (b) defines deterministic last-write-wins rules and still preserves data integrity. Negative scenarios confirm that under contention the system does not deadlock, does not corrupt the POD, and does not break dependent lookups like `/pod/{identifier}/exists` or contract–POD association workflows.

---

## Test data (preconditions)

- **Environment:** As per ticket.
- **Existing POD:** `POD_A` exists and is retrievable by identifier.
- **Two editors:** Two separate authenticated editor sessions (API clients or two UI users).
- **Dependent consumer checks:** Ability to call `/pod/{identifier}/exists` (or equivalent) and to retrieve contract–POD associations if used in the environment.

---

## TC-1 (Positive): Concurrent updates to different fields – deterministic result and no data loss

**Objective:** Verify that when two editors update different fields of the same POD concurrently, the system behaves predictably (either merges safely per contract, or rejects one update to prevent lost updates).

**Preconditions:**
1. `POD_A` exists.
2. Editor A and Editor B are authorized.
3. The POD has at least two independent mutable fields (Field X and Field Y).

**Steps:**
1. Editor A prepares an update that changes Field X.
2. Editor B prepares an update that changes Field Y.
3. Submit both updates as close in time as possible.
4. Fetch final POD state.

**Expected result:** The final POD state is consistent with the system’s concurrency contract:
- If versioning/ETag is enforced: one update succeeds and the other fails with a conflict; the failed editor must refetch and reapply.
- If safe merging is supported: both changes are present without overwriting unrelated fields.
In all cases, no partial/corrupted state.

---

## TC-2 (Negative): Lost update protection – same field updated concurrently

**Objective:** Ensure the system prevents silent overwrites when two updates target the same field.

**Preconditions:**
1. `POD_A` exists.
2. Editors A and B are authorized.

**Steps:**
1. Editor A updates Field X to value `X1` (do not assume immediate visibility if async; proceed as soon as request returns).
2. Editor B updates Field X to value `X2` concurrently or immediately after, without refetching.
3. Fetch final POD state.

**Expected result:** Either:
- One update is rejected with a conflict and the final state reflects the successful update only, or
- If last-write-wins is intentionally implemented, the final state reflects the later accepted update *and* the system records a clear audit trail of both attempts (if auditing exists).
No silent, undetectable overwrite if conflict protection is expected.

---

## TC-3 (Negative): High-frequency retry storm – repeated PUTs do not deadlock or degrade correctness

**Objective:** Validate resilience under retry storms (common in distributed systems) and ensure the update endpoint remains safe and consistent.

**Preconditions:**
1. `POD_A` exists.
2. Editor client can issue repeated requests rapidly.

**Steps:**
1. Choose a valid payload and send PUT updates for `POD_A` in a tight loop (e.g. 20–50 requests) with identical body (idempotent test).
2. Repeat with alternating payload values (to simulate rapid successive edits).
3. Monitor responses for timeouts, 5xx errors, or lock-related failures.
4. Fetch final POD state.

**Expected result:** The system remains stable (no deadlocks/timeouts beyond expected rate limits). Responses are consistent. Final POD state is valid and matches the last accepted update by design.

---

## TC-4 (Negative): Lock ordering regression – POD update while contract–POD flow runs

**Objective:** Detect lock ordering/deadlock risks when a POD update occurs during a contract–POD operation (attach/detach/update association) that may lock related entities.

**Preconditions:**
1. A contract–POD association exists for `POD_A` (or can be created).
2. Two editor sessions exist.

**Steps:**
1. In Session 1, start (or trigger) a contract–POD operation involving `POD_A` (e.g. attach/detach, contract update that touches POD references).
2. In Session 2, attempt to update `POD_A` at the same time.
3. Observe both operations’ responses and timing.

**Expected result:** The system does not deadlock. If locking blocks one operation, it should fail gracefully with a clear error or wait within acceptable timeout. No partial state: contract–POD associations remain consistent, and POD update is either applied fully or rejected.

---

## TC-5 (Positive): Consistent reads during update – `/pod/{identifier}/exists` remains correct

**Objective:** Ensure that during an update, identifier-based existence checks do not produce inconsistent “false negatives” that can break dependent flows.

**Preconditions:**
1. `POD_A` exists.
2. `/pod/{identifier}/exists` (or equivalent) is available.

**Steps:**
1. Begin a POD update request (if the system supports long-running updates; otherwise simulate with concurrent requests).
2. Repeatedly call `/pod/POD_A/exists` during the update window.
3. After update completes, call `/exists` again.

**Expected result:** `/exists` remains true for `POD_A` throughout (unless identifier itself is being changed, in which case behaviour must match the API contract and transition must be safe and well-defined). No transient incorrect “does not exist” responses.

---

## References

- **Jira:** PHN-2160 – Put: Update existing POD.
- **Cross-dependency risks addressed:** locking/concurrency, contract–POD flow interaction, and `/pod/{identifier}/exists` stability under contention.

