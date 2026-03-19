# POD update existing – API PUT update contract (PHN-2160)

**Jira:** PHN-2160 (Phoenix)  
**Type:** Task  
**Summary:** Verify that the API “PUT: update existing POD” correctly updates an existing POD record without creating duplicates, enforces validation and permissions, remains idempotent, and returns clear errors. This document emphasizes boundary conditions and regressions that can affect identifier-based lookups and dependent consumers.

**Scope:** This document covers the API behaviour for updating an existing POD using a PUT operation. The expected behaviour is that the system updates exactly one existing POD (identified by the path identifier or ticket-specified key), validates the payload, and preserves uniqueness and referential integrity. It must not create a new POD record, must not break `/pod/{identifier}/exists`, and must be robust under retries and concurrent updates. Negative scenarios confirm that invalid inputs, forbidden states, or conflicts are rejected with clear, consistent errors and without partial updates.

---

## Test data (preconditions)

- **Environment:** As per ticket (prefer Test if available; otherwise Dev/Dev2).
- **Existing POD (baseline):** A POD exists with a known `identifier` and a complete set of fields required by the API.
- **Secondary POD (for uniqueness tests):** A second POD exists with a different `identifier` (used to validate uniqueness constraints if identifier can be updated).
- **API client:** A client capable of calling the PUT endpoint with appropriate auth (and with a “no-permission” user for negative permission tests).
- **Contracts linked to POD (for integrity checks):** At least one contract–POD association exists referencing the baseline POD (if the product supports it), so we can verify update does not break linked flows.

---

## TC-1 (Positive): Update existing POD – happy path updates allowed fields

**Objective:** Verify that a valid PUT request updates the existing POD’s allowed fields and returns a success response, without creating a new POD record or breaking identifier-based reads.

**Preconditions:**
1. A POD exists with identifier `POD_A` and has stable baseline values for all fields used in the update.
2. The caller is authenticated and authorized to update PODs.
3. The API endpoint for “PUT update existing POD” is reachable in the chosen environment.

**Steps:**
1. Fetch the current POD details for `POD_A` (e.g. GET POD details by identifier), and record the current values.
2. Prepare a valid PUT payload that updates a set of allowed mutable fields (e.g. name/label, address, metadata, or other ticket-defined fields).
3. Call the PUT update endpoint for `POD_A` with the payload.
4. Confirm the response indicates success (e.g. success status code and a response body containing the updated POD or an updated timestamp/version).
5. Fetch the POD details for `POD_A` again.
6. Compare the updated POD with the expected changes and ensure fields not in the payload remain unchanged unless the API specifies full-replacement semantics for PUT.

**Expected result:** The request succeeds and the existing POD is updated exactly as expected. No new POD is created. The POD can still be retrieved by `POD_A`, and any dependent references (e.g. contract–POD links) remain valid.

**References:** PHN-2160; PUT update existing POD endpoint.

---

## TC-2 (Positive): PUT idempotency – retry the same request (network retry)

**Objective:** Ensure the API is safe under retries: sending the same PUT request multiple times results in the same final state and does not create duplicates or unintended side effects.

**Preconditions:**
1. Same as TC-1.
2. The test client can repeat the same request multiple times (simulating retries).

**Steps:**
1. Execute TC-1 step 3 (PUT update) with a deterministic payload.
2. Immediately resend the exact same PUT request to `POD_A` one or more times.
3. Fetch POD details after the final retry.

**Expected result:** All requests succeed (or duplicates are handled safely if the API returns “already up to date”). The final POD state matches the requested update. No extra records are created, and audit/history (if any) is consistent with expected idempotent semantics.

---

## TC-3 (Positive): Partial vs full update semantics – verify behaviour matches API contract

**Objective:** Verify whether PUT behaves as full replacement or supports partial updates, and confirm the behaviour is consistent and documented (critical for backward compatibility).

**Preconditions:**
1. A POD exists with several non-null fields (including optional fields).
2. Caller is authorized.

**Steps:**
1. Create two payload variants:
   - Variant A: includes only a subset of mutable fields.
   - Variant B: includes all mutable fields (explicitly setting optional fields).
2. Call PUT with Variant A and observe whether omitted fields remain unchanged or are cleared/defaulted.
3. Restore baseline state if needed.
4. Call PUT with Variant B and verify all fields reflect the payload.

**Expected result:** The API behaves consistently with its contract:
- If PUT is full replacement: omitted fields are cleared/defaulted as specified, and this is stable across calls.
- If PUT is partial: omitted fields remain unchanged.
In both cases, the behaviour is deterministic and does not cause accidental data loss.

---

## TC-4 (Negative): Update non-existent POD identifier

**Objective:** Verify that updating a POD that does not exist returns a clear error and does not create a new POD implicitly (unless the ticket explicitly defines upsert behaviour, in which case the test must confirm correct upsert rules).

**Preconditions:**
1. Caller is authorized.
2. There is no POD with identifier `POD_DOES_NOT_EXIST`.

**Steps:**
1. Call PUT update for identifier `POD_DOES_NOT_EXIST` with an otherwise valid payload.
2. Observe the response.
3. Attempt to retrieve `POD_DOES_NOT_EXIST` via GET (and via `/pod/{identifier}/exists` if available).

**Expected result:** The API returns a “not found” error (e.g. 404) or the ticket-defined error response. No POD record is created as a side effect. `/exists` remains false.

---

## TC-5 (Negative): Invalid identifier format (path parameter validation)

**Objective:** Ensure the endpoint validates the identifier format early and returns a clear validation error without any writes.

**Preconditions:**
1. Caller is authorized.

**Steps:**
1. Call PUT with an invalid identifier format in the path (e.g. empty string, too long, invalid characters, whitespace).
2. Observe response and verify no changes were applied to any existing POD.

**Expected result:** The API rejects the request with a validation error (e.g. 400). No POD is updated and no audit/update side effects occur.

---

## TC-6 (Negative): Payload validation – missing required fields

**Objective:** Verify the API enforces required fields (as per contract) and returns consistent, actionable validation errors.

**Preconditions:**
1. A POD exists with identifier `POD_A`.
2. Caller is authorized.

**Steps:**
1. Build a payload that omits one required field at a time (e.g. required name/address/status fields, if any).
2. For each invalid payload, call PUT update for `POD_A`.
3. After each call, fetch the POD and confirm it did not change.

**Expected result:** Each request is rejected with a clear error that identifies the missing field(s). The POD remains unchanged.

---

## TC-7 (Negative): Payload validation – invalid field lengths / boundaries

**Objective:** Validate boundary conditions for fields such as strings, numeric ranges, enums, and nested objects, ensuring consistent errors and no partial updates.

**Preconditions:**
1. A POD exists with identifier `POD_A`.
2. Caller is authorized.

**Steps:**
1. For each field with constraints, prepare payloads covering:
   - Minimum boundary (e.g. empty string when not allowed).
   - Maximum allowed length (exact boundary).
   - Over-maximum length (boundary + 1).
   - Invalid enum values or case-sensitivity variants if applicable.
2. Call PUT with each payload and verify response and persistence.

**Expected result:** Valid boundaries succeed; invalid boundaries fail with clear validation errors. No partial updates occur; either the update is fully applied or fully rejected.

---

## TC-8 (Negative): Uniqueness regression – attempt to set identifier to an existing POD’s identifier (if identifier is mutable)

**Objective:** If the ticket allows updating the POD identifier, verify uniqueness is enforced and conflicts are rejected without corrupting `/exists` behaviour or identifier-based reads.

**Preconditions:**
1. Two PODs exist: `POD_A` and `POD_B`.
2. Caller is authorized.
3. The API contract allows updating identifier (if not allowed, treat as a separate negative test that it is rejected).

**Steps:**
1. Send PUT update for `POD_A` attempting to change its identifier to `POD_B` (or to an identifier already in use).
2. Observe response.
3. Verify `POD_A` still exists and remains retrievable under its original identifier.
4. Verify `POD_B` remains unchanged.
5. Check `/pod/{identifier}/exists` for both identifiers.

**Expected result:** The API rejects the update with a conflict/validation error (e.g. 409). No identifier collision occurs. `/exists` results remain correct and stable.

---

## TC-9 (Negative): Permission denied – caller lacks POD update permission

**Objective:** Verify that unauthorized users cannot update a POD and that the system does not leak sensitive details.

**Preconditions:**
1. A POD exists with identifier `POD_A`.
2. A user or token exists without POD update permissions (e.g. read-only role).

**Steps:**
1. Call PUT update for `POD_A` using the no-permission user.
2. Observe response.
3. Fetch the POD using an authorized user and confirm no changes were applied.

**Expected result:** The request is rejected (e.g. 403 Forbidden). The POD remains unchanged. Error response is clear but does not disclose internal authorization details.

---

## TC-10 (Negative): Concurrency conflict – update with stale version/ETag (if supported)

**Objective:** Ensure the system protects against lost updates when two clients update the same POD, using versioning/ETags or another concurrency mechanism if present.

**Preconditions:**
1. A POD exists with identifier `POD_A`.
2. The API returns a version/updatedAt/ETag (or similar) that can be used for conditional updates, if supported.
3. Caller is authorized.

**Steps:**
1. Client 1 fetches `POD_A` and records the version/ETag.
2. Client 2 updates `POD_A` successfully (PUT) changing a field.
3. Client 1 attempts to update `POD_A` using the stale version/ETag (e.g. `If-Match` header) or stale payload assumptions.

**Expected result:** The API rejects the stale update with a conflict (e.g. 409 or 412 Precondition Failed) or ticket-defined behaviour. The system preserves the newer update and returns a response that helps the client recover (e.g. instruct to refetch).

---

## TC-11 (Positive): Cross-feature stability – contract–POD references remain valid after update

**Objective:** Verify that updating POD details does not break dependent flows that reference the POD (e.g. contract POD association retrieval, billing/termination flows that rely on POD identity).

**Preconditions:**
1. A contract–POD association exists pointing to `POD_A`.
2. Caller is authorized to update POD.

**Steps:**
1. Update `POD_A` using PUT (as in TC-1).
2. Retrieve the contract–POD association and any related screens/endpoints that depend on `POD_A`.
3. Confirm the association still references the correct POD and displays/returns updated POD details where appropriate.

**Expected result:** Dependent flows continue to function. The link remains intact. Any views that present POD details reflect the updated values without errors.

---

## References

- **Jira:** PHN-2160 – Put: Update existing POD.
- **Regression targets:** POD list/filter views; `/pod/{identifier}/exists`; contract–POD flows that rely on stable identifier and consistent POD state.

