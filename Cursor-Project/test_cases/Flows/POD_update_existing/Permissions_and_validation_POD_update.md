# POD update existing – Permissions and validation (PHN-2160)

**Jira:** PHN-2160 (Phoenix)  
**Type:** Task  
**Summary:** Exhaustive permission and validation tests for updating existing PODs. Ensures only authorized roles can update, field-level restrictions are enforced, errors are clear, and updates do not weaken security or data integrity.

**Scope:** This document focuses on authorization (who can update) and validation (what can be updated and under which rules). The expected behaviour is that the system enforces a strict permission model for POD updates, validates all inputs (including boundary constraints and forbidden field changes), and rejects invalid requests without partial updates. Negative tests verify that unauthorized access is blocked, input validation is robust, and sensitive fields cannot be modified by clients if they are meant to be read-only.

---

## Test data (preconditions)

- **Environment:** As per ticket.
- **Existing POD:** `POD_A` exists with a complete baseline dataset.
- **Users/roles:**
  - **Admin/editor role:** Can update PODs.
  - **Viewer role:** Can read but cannot update PODs.
  - **No-access role:** Cannot read or update PODs (if applicable).
- **Audit/log visibility:** Ability to verify audit entries (UI/system logs) if the product exposes them to users/administrators.

---

## TC-1 (Positive): Authorized editor can update POD (permission allow)

**Objective:** Verify that a user with the correct role/permission can update an existing POD and that the update is persisted.

**Preconditions:**
1. `POD_A` exists.
2. Editor user is authenticated and authorized to update PODs.

**Steps:**
1. Call the POD update operation (API PUT or UI Save) with a valid payload that changes an allowed field.
2. Retrieve `POD_A` via standard read path (UI detail or GET).

**Expected result:** Update succeeds. `POD_A` reflects the change. The response/UI confirms success.

---

## TC-2 (Negative): Viewer/read-only user cannot update POD (permission deny)

**Objective:** Ensure a read-only user is blocked from updating PODs.

**Preconditions:**
1. `POD_A` exists.
2. Viewer user is authenticated but does not have update permission.

**Steps:**
1. Attempt to update `POD_A` as the viewer user (API PUT or UI).
2. Retrieve `POD_A` using an authorized user to confirm state.

**Expected result:** The system rejects the update (e.g. 403/forbidden UI state). `POD_A` remains unchanged. Error message is clear and does not leak internal permission implementation details.

---

## TC-3 (Negative): Anonymous/invalid token cannot update POD

**Objective:** Confirm that requests without authentication are rejected.

**Preconditions:**
1. `POD_A` exists.

**Steps:**
1. Call the update endpoint without auth (or with an invalid/expired token).
2. Observe response and verify no update occurred.

**Expected result:** The system rejects with 401 (or equivalent) and makes no changes.

---

## TC-4 (Negative): Field-level protection – attempt to update read-only/system-managed fields

**Objective:** Verify that fields which should not be client-modifiable (e.g. internal IDs, created timestamps, system status flags, links to other entities) cannot be updated through the update endpoint.

**Preconditions:**
1. `POD_A` exists.
2. Editor user is authorized.
3. The API/UI exposes some fields as read-only (ticket-defined).

**Steps:**
1. Prepare an update payload that includes a change to a suspected read-only field (e.g. internal numeric ID, creation date, foreign key, system status).
2. Send the update request.
3. Retrieve `POD_A` after the request.

**Expected result:** The system rejects the update with a validation error, or ignores the read-only field while applying allowed changes (only if explicitly designed). In either case, the read-only field is not changed, and behaviour is consistent and documented.

---

## TC-5 (Negative): Validation – required field missing or null

**Objective:** Ensure required fields cannot be removed or set to null if the contract forbids it.

**Preconditions:**
1. `POD_A` exists.
2. Editor user is authorized.

**Steps:**
1. Attempt to update with a payload missing a required field (or explicitly setting it to null).
2. Retrieve `POD_A` to confirm no partial updates.

**Expected result:** The system rejects the request with a clear validation error and does not persist any part of the update.

---

## TC-6 (Negative): Validation – invalid enum/state transition not allowed

**Objective:** If the POD has a status/state field, verify only allowed values/transitions are accepted.

**Preconditions:**
1. `POD_A` exists in a known state.
2. Editor user is authorized.

**Steps:**
1. Attempt to set status/state to an invalid value (not in enum).
2. Attempt an invalid transition (e.g. from “inactive” to a state that requires prerequisites) if such rules exist.
3. Retrieve `POD_A`.

**Expected result:** Invalid values/transitions are rejected with clear errors. State remains unchanged.

---

## TC-7 (Positive): Audit trail – update records who/when changed POD (if auditing is required)

**Objective:** Verify that POD updates are auditable so that administrators can trace changes (critical for compliance and debugging).

**Preconditions:**
1. `POD_A` exists.
2. Editor user is authorized.
3. Audit trail is accessible (UI audit view, admin logs, or an API).

**Steps:**
1. Perform a successful update on `POD_A`.
2. Open the audit view/logs for `POD_A`.

**Expected result:** Audit shows the update event with at least: timestamp, user identity, and changed fields (or a reference to the change). No missing/incorrect attribution.

---

## TC-8 (Negative): Error message hygiene – validation errors are user-readable and consistent

**Objective:** Ensure validation errors are consistent across API/UI and do not expose sensitive internals (stack traces, SQL errors, internal table names).

**Preconditions:**
1. `POD_A` exists.
2. Editor user is authorized.

**Steps:**
1. Trigger several validation errors (missing required field, invalid format, uniqueness conflict).
2. Inspect error messages returned by API and/or shown in UI.

**Expected result:** Errors are clear, consistent, and actionable. No sensitive internal information is exposed. The system returns stable error codes/messages suitable for API consumers.

---

## References

- **Jira:** PHN-2160 – Put: Update existing POD.
- **Cross-dependency risks addressed:** permissions (role enforcement), validation contract stability, and prevention of dangerous field mutation that could break identifier-based lookups and contract–POD flows.

