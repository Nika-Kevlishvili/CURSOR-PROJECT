# POD update existing - Comprehensive exhaustive suite (PHN-2160)

**Jira:** PHN-2160 (Phoenix)  
**Type:** Story  
**Summary:** This document provides exhaustive test coverage for "Put: Update existing POD" where update is triggered by Sales Portal, input is `podParameters.Name` only, and update must happen in the already existing POD version (must not create a new POD version).

**Scope:** The suite validates end-to-end behaviour for API and UI around updating existing POD name, including strict field-scope enforcement, route/mapping compatibility risk, versioning invariants, lock and permission checks, and cross-dependency regressions (`/pod/{id}` vs `/api/pod/pod/{id}`, `/exists`, list usage, and bound-object restrictions). It also covers edge and boundary cases, concurrency, and negative misuse scenarios where clients attempt to update fields other than name. The expected result is deterministic, secure, and backward-compatible behaviour with no unintended POD version creation.

---

## Test data (preconditions)

- **Environment:** Test (preferred) or Dev/Dev2 matching Jira setup.
- **Sales Portal user (editor):** Has permission to update existing POD.
- **Sales Portal user (viewer):** Can view POD but cannot update.
- **Existing POD with versions:** At least one POD with a known current version and audit/history visibility.
- **Bound POD sample:** A POD currently used by dependent flow/object (contract or equivalent bound object).
- **API access:** Ability to call route as documented `/api/pod/pod/{id}` and verify actual controller mapping behaviour.
- **Baseline payload:** Valid payload with `podParameters.Name` only.

---

## TC-1 (Positive): Update existing POD name from Sales Portal happy path

**Objective:** Verify that a valid Sales Portal update changes only POD name in the existing POD version.

**Preconditions:**
1. POD `POD_A` exists and is editable.
2. Editor user is logged in.
3. Current POD version ID is recorded.

**Steps:**
1. Open `POD_A` in Sales Portal edit flow.
2. Change only `podParameters.Name` to a new valid value.
3. Save update.
4. Reload POD details and version history.

**Expected result:** Update succeeds, name is changed, and version ID remains the same (existing version updated, no new version created).

**References:** PHN-2160; Confluence 740229121.

---

## TC-2 (Positive): API PUT update via documented route `/api/pod/pod/{id}`

**Objective:** Confirm documented endpoint accepts valid name-only update payload.

**Preconditions:**
1. POD `POD_A` exists.
2. Editor token is valid.

**Steps:**
1. Send PUT request to `/api/pod/pod/{id}` for `POD_A`.
2. Payload contains only `podParameters.Name` with valid non-empty value.
3. Read POD details after response.

**Expected result:** Request succeeds and name is updated in place without creating a new POD version.

---

## TC-3 (Negative): Route mismatch detection between `/api/pod/pod/{id}` and `/pod/{id}`

**Objective:** Validate mapping risk and ensure clients receive consistent behaviour/documentation.

**Preconditions:**
1. POD `POD_A` exists.
2. Editor token is valid.

**Steps:**
1. Send identical valid name-only update to `/api/pod/pod/{id}`.
2. Send identical valid name-only update to `/pod/{id}`.
3. Compare status code, response model, and persistence.

**Expected result:** Either both routes are intentionally supported with equivalent behaviour, or unsupported route returns clear expected error. No silent partial update.

---

## TC-4 (Negative): Reject request when `podParameters.Name` is missing

**Objective:** Ensure required input is enforced.

**Preconditions:**
1. POD `POD_A` exists.

**Steps:**
1. Send PUT request with missing `podParameters.Name`.
2. Retrieve POD after request.

**Expected result:** Validation error returned, POD unchanged, no new version created.

---

## TC-5 (Negative): Reject request when `podParameters.Name` is null

**Objective:** Validate null handling for required name.

**Preconditions:**
1. POD `POD_A` exists.

**Steps:**
1. Send PUT with `podParameters.Name = null`.
2. Verify response and persisted state.

**Expected result:** Request is rejected with clear validation message; POD remains unchanged.

---

## TC-6 (Negative): Reject blank-only name

**Objective:** Ensure whitespace-only values are invalid.

**Preconditions:**
1. POD `POD_A` exists.

**Steps:**
1. Send PUT with name containing only spaces/tabs.
2. Fetch POD details.

**Expected result:** Validation error; unchanged POD.

---

## TC-7 (Positive): Boundary - minimum allowed name length

**Objective:** Verify lower boundary acceptance for valid minimal name.

**Preconditions:**
1. POD `POD_A` exists.
2. Minimum length rule is known from backend validation.

**Steps:**
1. Send PUT with name at exact minimum allowed length.
2. Fetch POD details and version history.

**Expected result:** Update succeeds; name persists; no new version record.

---

## TC-8 (Positive): Boundary - maximum allowed name length

**Objective:** Verify upper valid boundary.

**Preconditions:**
1. POD `POD_A` exists.

**Steps:**
1. Send PUT with name at exact maximum allowed length.
2. Verify persistence and version ID stability.

**Expected result:** Update succeeds with exact boundary value, no new version.

---

## TC-9 (Negative): Boundary overflow - name length max+1

**Objective:** Ensure overflow is blocked.

**Preconditions:**
1. POD `POD_A` exists.

**Steps:**
1. Send PUT with name exceeding max length by 1.
2. Retrieve POD details.

**Expected result:** Validation error, no data change.

---

## TC-10 (Positive): Preserve allowed special characters in name

**Objective:** Verify supported characters are accepted and stored correctly.

**Preconditions:**
1. POD `POD_A` exists.

**Steps:**
1. Send valid name containing permitted special characters and multilingual-safe ASCII subset expected by system.
2. Reload POD details from API and UI.

**Expected result:** Name is stored and displayed exactly as expected by normalization rules.

---

## TC-11 (Negative): Attempt to update extra fields beyond name - explicit rejection

**Objective:** Validate business comment "only POD name can be updated."

**Preconditions:**
1. POD `POD_A` exists with other mutable-looking fields in request model.

**Steps:**
1. Send PUT with `podParameters.Name` plus additional fields (invoicing-related and non-name fields).
2. Fetch POD and related invoicing data.

**Expected result:** Request is rejected with field-level error, or extra fields are ignored per contract; in both cases only name may change and invoicing data must remain unchanged.

---

## TC-12 (Negative): Attempt to modify invoicing data through update endpoint

**Objective:** Confirm invoicing data cannot be updated in this story scope.

**Preconditions:**
1. POD `POD_A` has linked invoicing data.

**Steps:**
1. Submit payload trying to alter invoicing attributes together with name.
2. Validate API response.
3. Verify invoicing data before/after.

**Expected result:** Invoicing data update is blocked/ignored according to contract; no unauthorized invoicing modification.

---

## TC-13 (Positive): Idempotency - repeat same valid name update

**Objective:** Ensure retries do not create versions or side effects.

**Preconditions:**
1. POD `POD_A` exists.

**Steps:**
1. Send the same valid PUT request twice (or multiple times).
2. Compare state and audit/version history.

**Expected result:** Final state remains consistent; no duplicate version creation; deterministic response behaviour.

---

## TC-14 (Negative): Update non-existent POD ID

**Objective:** Verify no implicit create/upsert on unknown ID.

**Preconditions:**
1. POD ID used in request does not exist.

**Steps:**
1. Send valid name-only PUT for non-existent ID.
2. Query POD list/exists endpoint for that ID.

**Expected result:** Not-found error; no new POD/version created.

---

## TC-15 (Negative): Permission denied for viewer role

**Objective:** Verify role-based access control for update.

**Preconditions:**
1. Viewer user (no update permission) is authenticated.
2. POD `POD_A` exists.

**Steps:**
1. Attempt update from viewer user through UI/API.
2. Check response and persisted data.

**Expected result:** Access denied; no update applied.

---

## TC-16 (Negative): Lock validation blocks update when lock not allowed

**Objective:** Validate lock/annotation protections from controller/service layer.

**Preconditions:**
1. POD `POD_A` is in lock state that should block update.

**Steps:**
1. Attempt valid name-only update while lock is active.
2. Observe response and persistence.

**Expected result:** Clear lock-related rejection and no update.

---

## TC-17 (Positive): Allowed lock state permits update

**Objective:** Confirm valid lock state allows normal update flow.

**Preconditions:**
1. POD `POD_A` is in update-allowed lock state.

**Steps:**
1. Send valid name-only update.
2. Verify update and unchanged version ID.

**Expected result:** Update succeeds with expected lock-aware behaviour.

---

## TC-18 (Negative): Bound-object restriction blocks prohibited update

**Objective:** Validate restrictions when POD is bound to dependent objects.

**Preconditions:**
1. `POD_A` is bound to dependent object with strict update rules.

**Steps:**
1. Attempt name update violating bound-object rule.
2. Check response and related object integrity.

**Expected result:** Update is rejected when rule disallows it; no broken references.

---

## TC-19 (Positive): Bound-object allowed scenario updates safely

**Objective:** Ensure allowed bound-object scenario still supports name update.

**Preconditions:**
1. `POD_A` is bound but rule allows name-only update.

**Steps:**
1. Perform name-only update.
2. Verify dependent references still resolve correctly.

**Expected result:** Update succeeds with no dependency regressions.

---

## TC-20 (Negative): Concurrent edits from two users on same POD

**Objective:** Detect lost-update or conflict-handling issues.

**Preconditions:**
1. Two editor sessions are active.
2. POD `POD_A` exists.

**Steps:**
1. User A prepares name change `Name_A`.
2. User B prepares name change `Name_B`.
3. Submit updates nearly simultaneously.
4. Check final state, response codes, and audit.

**Expected result:** Conflict handling is deterministic (reject stale/second or controlled last-write-wins per contract); no corruption or version duplication.

---

## TC-21 (Negative): Concurrent edit with active lock acquisition race

**Objective:** Validate no deadlock and clear failure mode under lock contention.

**Preconditions:**
1. Two clients can issue updates concurrently.

**Steps:**
1. Start update A and immediately start update B for same POD.
2. Track response timing and status.

**Expected result:** No deadlock; one flow may be rejected/retried cleanly; persisted state remains valid.

---

## TC-22 (Positive): `/exists` and retrieval consistency after name update

**Objective:** Ensure related read flows remain correct after update.

**Preconditions:**
1. POD `POD_A` exists and can be checked via exists/read endpoints.

**Steps:**
1. Perform successful name update.
2. Call relevant exists/retrieval endpoints for same POD ID.

**Expected result:** Existence and retrieval stay consistent and do not regress because of update logic.

---

## TC-23 (Negative): No sorting/filtering/pagination assumptions in update flow

**Objective:** Validate client and backend do not rely on unsupported sort/filter/page params for this operation.

**Preconditions:**
1. POD `POD_A` exists.

**Steps:**
1. Attempt to send sort/filter/pagination params along with update request.
2. Observe API behaviour and error handling.

**Expected result:** Unsupported params are ignored or rejected clearly; update semantics remain unaffected.

---

## TC-24 (Positive): Audit/history confirms in-place update, not new POD version

**Objective:** Provide final proof of story-critical invariant.

**Preconditions:**
1. Audit/version history is accessible.
2. POD `POD_A` exists.

**Steps:**
1. Record current version metadata.
2. Perform successful name-only update.
3. Inspect version history/audit records.

**Expected result:** Audit shows update event on existing version; no creation of a new POD version entity.

---

## References

- **Jira:** PHN-2160 - Put: Update existing POD.
- **Confluence:** pageId `740229121`.
- **Business constraints:** update triggered by Sales Portal; input is `podParameters.Name` only; no invoicing data changes; no new POD version creation.
- **Cross-dependency risks covered:** route mismatch, lock validation, permission annotations, `updateExistingVersion` branch logic, request model over-posting risk, bound-object restrictions, and concurrent edits.
