# POD update existing – UI edit flow (PHN-2160)

**Jira:** PHN-2160 (Phoenix)  
**Type:** Task  
**Summary:** Validate the UI flow for editing an existing POD: loading data, editing fields, save/cancel behaviour, validation feedback, list/detail refresh, and safe handling of concurrent changes and permissions.

**Scope:** This document covers the end-to-end UI experience for updating an existing POD. The expected behaviour is that the user can open a POD detail/edit screen, modify allowed fields, and save changes successfully with clear confirmation and immediate visibility in relevant UI areas (detail view, list views, filters, and search). Negative scenarios confirm that the UI blocks invalid inputs, handles authorization errors, handles conflicts from concurrent edits, and does not leave the UI in a misleading state (e.g. showing saved changes when the backend rejected them).

---

## Test data (preconditions)

- **Environment:** As per ticket (prefer Test if available; otherwise Dev/Dev2).
- **User (editor):** A user account with permission to edit PODs in UI.
- **User (read-only):** A user account that can view PODs but cannot edit (for permission tests).
- **Existing POD:** A POD exists with identifier `POD_A` and visible fields in the UI (including at least one required field and one optional field).
- **Baseline list view:** POD list page shows `POD_A` (or can be found via search/filter).

---

## TC-1 (Positive): Open POD edit screen and successfully save valid changes

**Objective:** Verify that a user with edit permission can open the edit screen for an existing POD, change allowed fields, save successfully, and see consistent updated data in the UI.

**Preconditions:**
1. `POD_A` exists and is visible in UI.
2. The editor user is logged in and has permission to edit PODs.

**Steps:**
1. Navigate to the POD list page.
2. Search for `POD_A` (e.g. by identifier) and open its detail page.
3. Click the “Edit” action (or equivalent) to enter edit mode.
4. Modify one required field and one optional field (use values that are valid and clearly distinguishable from the baseline).
5. Click “Save”.
6. Observe the success feedback (toast/banner) and verify the page reflects the updated values.
7. Navigate back to the POD list and ensure `POD_A` row reflects updated values where applicable.

**Expected result:** Save succeeds. The UI shows a clear success confirmation. Updated values are visible in detail view and list view without requiring a manual hard refresh beyond normal UI behaviour.

**References:** PHN-2160; UI POD edit flow.

---

## TC-2 (Positive): Cancel edit – discard changes and keep original values

**Objective:** Ensure that the user can cancel out of the edit flow and that unsaved changes are not persisted.

**Preconditions:**
1. Same as TC-1.

**Steps:**
1. Open `POD_A` in edit mode.
2. Change one or more fields.
3. Click “Cancel” or navigate away using the UI back action.
4. If the UI shows an “unsaved changes” confirmation, choose the option to discard changes.
5. Re-open `POD_A` detail view (and edit view if necessary) to confirm persisted values.

**Expected result:** Changes are discarded. The POD remains unchanged in the backend and UI. The UI provides a clear discard confirmation if changes were present.

---

## TC-3 (Negative): Client-side validation – required field empty blocks Save

**Objective:** Verify that the UI prevents saving when required fields are invalid and provides actionable validation messages.

**Preconditions:**
1. Editor user can edit `POD_A`.

**Steps:**
1. Open `POD_A` in edit mode.
2. Clear a required field (e.g. set to empty).
3. Click “Save”.

**Expected result:** Save is blocked. The UI highlights the invalid field and shows a clear validation message explaining what is required. No request is sent (if client-side validation exists) or the backend rejects it and UI displays the backend error without persisting changes.

---

## TC-4 (Negative): Server-side validation – invalid field length/format shows inline error and preserves user input

**Objective:** Ensure that if backend validation rejects a value (length/format), the UI shows a meaningful error and does not lose user-entered data unnecessarily.

**Preconditions:**
1. Editor user can edit `POD_A`.

**Steps:**
1. Open `POD_A` in edit mode.
2. Enter a value that violates a backend constraint (e.g. over max length, invalid characters in an identifier-like field, invalid enum).
3. Click “Save”.
4. Observe the error message.
5. Correct the value to a valid one and save again.

**Expected result:** The UI shows the backend error in a user-readable form (ideally field-level). The user’s input remains present so it can be corrected. After correction, save succeeds.

---

## TC-5 (Negative): Permission regression – read-only user cannot edit (Edit button hidden/disabled)

**Objective:** Confirm that users without edit permissions cannot access the edit flow and receive appropriate UI messaging if they try.

**Preconditions:**
1. Read-only user is logged in.
2. `POD_A` exists and is visible to the read-only user.

**Steps:**
1. Open `POD_A` detail page.
2. Verify whether the UI displays an “Edit” action.
3. If “Edit” is visible, attempt to click it.
4. If direct navigation to edit URL is possible, attempt to open it.

**Expected result:** The read-only user cannot edit. The Edit action is hidden or disabled, or the UI blocks access with a clear “not authorized” message. No changes are persisted.

---

## TC-6 (Negative): Concurrency – UI shows conflict when POD changed by another user

**Objective:** Verify that the UI properly handles concurrent edits and does not overwrite changes silently.

**Preconditions:**
1. Editor user A and editor user B both can edit PODs.
2. `POD_A` exists.

**Steps:**
1. User A opens `POD_A` in edit mode and changes Field X (do not save yet).
2. User B opens `POD_A` and saves a change to Field X (or another visible field).
3. User A attempts to save their changes.

**Expected result:** The UI shows a conflict or “data changed” error (ticket-defined behaviour). User A is guided to refresh/reload before saving. The system does not silently overwrite User B’s changes unless explicitly designed to do so, and if it does overwrite, the UI must clearly indicate last-write-wins semantics.

---

## TC-7 (Positive): Post-save navigation – updated POD remains consistent across list, filters, and detail

**Objective:** Ensure UI consistency after update across different UI entry points that users commonly rely on.

**Preconditions:**
1. Editor user can edit `POD_A`.
2. POD list supports search/filter by identifier and possibly other fields.

**Steps:**
1. Save an update on `POD_A` (as in TC-1).
2. Navigate to POD list.
3. Use filters/search that should include `POD_A` and verify it appears with the updated values.
4. Open `POD_A` again and confirm the same updated values appear.

**Expected result:** UI views are consistent. No stale cache shows old data in one view while another view shows new data.

---

## References

- **Jira:** PHN-2160 – Put: Update existing POD.
- **Regression targets:** UI list/filter behaviours; edit permissions; conflict handling; identifier-based navigation.

