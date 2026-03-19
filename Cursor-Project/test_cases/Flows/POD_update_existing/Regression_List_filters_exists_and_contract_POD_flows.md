# POD update existing – Regression: list/filters, `/exists`, contract–POD flows (PHN-2160)

**Jira:** PHN-2160 (Phoenix)  
**Type:** Task  
**Summary:** Regression suite validating that updating an existing POD does not break dependent behaviours: POD list and filters (UI/API), `/pod/{identifier}/exists`, and contract–POD flows that rely on stable POD identifiers and consistent POD state.

**Scope:** This document is a comprehensive regression checklist for cross-dependency risks. The expected behaviour is that after a POD update, all identifier-based operations and list/filter/search results remain correct, consistent, and performant. Contract–POD flows must continue to work (attach/detach/read associations) without stale caches or referential issues. Negative scenarios verify safe handling of edge conditions such as identifier changes (if allowed), stale caches, and incorrect filter results, and confirm the system does not return misleading responses (e.g. `/exists=false` for an existing POD).

---

## Test data (preconditions)

- **Environment:** As per ticket.
- **POD dataset for list/filter:** At least 5 PODs exist with varied field values to exercise filters:
  - `POD_A` (target for update)
  - `POD_B` (different attributes)
  - Additional PODs that share some filterable attributes with `POD_A`
- **UI access:** POD list page supports search and filters (if UI exists).
- **API access:** Any list/search endpoints used by UI or consumers are available (if applicable).
- **Exists endpoint:** `/pod/{identifier}/exists` is available (or equivalent).
- **Contract dataset:** At least one contract is associated with `POD_A` (if the product supports contract–POD relations).

---

## TC-1 (Positive): POD list and search reflect updated values immediately after update

**Objective:** Verify that after updating a POD, list/search results reflect the new data and do not show stale values.

**Preconditions:**
1. `POD_A` exists and appears in list/search.
2. Editor user can update `POD_A`.

**Steps:**
1. Record current list view (or API list response) values for `POD_A` for at least one displayed field.
2. Update `POD_A` (API PUT or UI) changing a field that appears in list/search (e.g. name/label/status).
3. Refresh the list view (using normal UI refresh) or re-run the list API query.
4. Search for `POD_A` by identifier and confirm it appears.

**Expected result:** `POD_A` appears in list/search and shows updated values. No duplicate rows for `POD_A`. Filters/search remain functional.

---

## TC-2 (Positive): Filters behave correctly after update (inclusion/exclusion boundaries)

**Objective:** Ensure that updating a filter-relevant field results in correct inclusion/exclusion in filtered views, preventing regression in filtering logic.

**Preconditions:**
1. Filterable field exists (e.g. status/region/type).
2. `POD_A` currently matches Filter Set 1 but not Filter Set 2 (or vice versa).

**Steps:**
1. Apply Filter Set 1 and confirm `POD_A` is present.
2. Update `POD_A` so that it should no longer match Filter Set 1 (and/or should match Filter Set 2).
3. Re-apply filters and verify list membership.

**Expected result:** Filter results update correctly: `POD_A` is included/excluded according to the new field values. No stale membership.

---

## TC-3 (Positive): `/pod/{identifier}/exists` remains true for existing POD before and after update

**Objective:** Verify that the existence check endpoint remains stable and correct after updating an existing POD.

**Preconditions:**
1. `POD_A` exists.
2. Exists endpoint is available.

**Steps:**
1. Call `/pod/POD_A/exists` and confirm it returns true.
2. Update `POD_A` (change a non-identifier field).
3. Call `/pod/POD_A/exists` again.

**Expected result:** `/exists` returns true before and after the update. No transient false negatives. Response format and status remain consistent.

---

## TC-4 (Negative): `/pod/{identifier}/exists` returns false for non-existent identifier (no false positives)

**Objective:** Ensure `/exists` does not regress to returning true for identifiers that do not exist.

**Preconditions:**
1. No POD exists with identifier `POD_DOES_NOT_EXIST`.

**Steps:**
1. Call `/pod/POD_DOES_NOT_EXIST/exists`.

**Expected result:** The endpoint returns false (or ticket-defined “not found” semantics). No errors that imply server instability.

---

## TC-5 (Negative): Identifier change regression (only if identifier is allowed to change)

**Objective:** If the update can change the POD identifier, verify the transition is safe and does not break consumer assumptions.

**Preconditions:**
1. `POD_A` exists.
2. The product allows identifier changes (if not allowed, treat as a negative test that such attempts are rejected).

**Steps:**
1. Update `POD_A` to change identifier from `POD_A` to `POD_A_NEW`.
2. Call `/pod/POD_A/exists` and `/pod/POD_A_NEW/exists`.
3. Search/list for both identifiers.
4. Retrieve POD details by identifier using both values.

**Expected result:** Behaviour follows the defined contract:
- Either identifier changes are rejected (preferred for stability), or
- If allowed: old identifier is no longer valid (exists=false) and new identifier is valid (exists=true) after the update, with no period where both are true for the same POD unless explicitly supported. No duplicates and no broken reads.

---

## TC-6 (Positive): Contract–POD association retrieval works after POD update

**Objective:** Ensure contract–POD flows are not broken by POD updates (critical regression risk).

**Preconditions:**
1. A contract is associated with `POD_A`.
2. User can view contract POD details.

**Steps:**
1. Update `POD_A` with a visible field change.
2. Open contract details and navigate to the POD section (or call the API that returns contract–POD association data).
3. Confirm the association is still present and references the correct POD.

**Expected result:** The association remains intact. Contract view/API returns correct POD reference and reflects updated POD details where relevant.

---

## TC-7 (Negative): Contract–POD operation still enforces validation when POD update introduces edge values

**Objective:** Ensure that after updating POD fields to boundary-but-valid values, contract–POD flows still validate and behave correctly (no hidden assumptions break).

**Preconditions:**
1. Contract is associated with `POD_A`.
2. Some POD fields have strict constraints (length, format, or specific allowed sets).

**Steps:**
1. Update `POD_A` to boundary-valid values (e.g. max length strings, rare-but-valid enum).
2. Perform a contract–POD operation that reads/uses POD attributes (e.g. list contract PODs, attach/detach another POD, run a contract update that revalidates POD info).

**Expected result:** Contract–POD operations succeed. No unexpected validation failures or crashes caused by boundary-valid POD data.

---

## TC-8 (Negative): UI cache regression – list shows stale POD after successful update (detect and prevent)

**Objective:** Detect stale-cache behaviour in UI where an updated POD does not reflect new values in list/filter/search views.

**Preconditions:**
1. UI list and detail views exist for PODs.
2. `POD_A` appears in the list.

**Steps:**
1. Update `POD_A` in detail/edit view and confirm success in detail view.
2. Navigate to the list without hard refreshing the browser.
3. Observe whether list row shows updated values.
4. Apply a filter/search and re-check the row.

**Expected result:** The list reflects updated values through normal navigation/refresh flows. If a stale state appears, it is considered a regression that must be fixed (or documented with a workaround).

---

## References

- **Jira:** PHN-2160 – Put: Update existing POD.
- **Regression targets explicitly requested:** permissions (covered in separate doc), locking (covered in separate doc), UI edit flow, list/filter regression, `/pod/{identifier}/exists` regression, contract POD flows impact.

