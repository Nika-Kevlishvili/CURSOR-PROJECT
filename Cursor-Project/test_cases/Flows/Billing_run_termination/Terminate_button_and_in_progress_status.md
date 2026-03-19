# Billing Run Termination – Terminate Button and "In Progress Termination" Status (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** The change introduces a new status "in progress termination" and disables the Terminate button when appropriate. This document tests the UI Terminate button visibility/state and the display and behaviour of the new status.

**Scope:** The UI must disable (or hide) the Terminate button when the billing run is not in a status that allows termination, and when the run is already "in progress termination". The new status "in progress termination" must be displayed correctly in the UI and reflected in the API. After termination completes, the run should be in CANCELLED status.

---

## Test data (preconditions)

- **Environment:** Test or Dev with UI and API available.
- **Billing run:** At least one billing run exists. For button tests, runs in various statuses are needed: INITIAL, DRAFT, GENERATED, PAUSED, CANCELLED, and "in progress termination" (if achievable in test data or by triggering termination and checking before completion).
- **User:** Logged-in user has permission to view and terminate billing runs where allowed.

---

## TC-1 (Positive): Terminate button is visible and enabled when run is in an allowed status

**Objective:** Verify that when the billing run is in one of the allowed statuses (INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED), the Terminate button is visible and enabled so the user can request termination.

**Preconditions:**
1. User is logged in and has permission to terminate billing runs.
2. A billing run exists in one of the allowed statuses (e.g. DRAFT or GENERATED).
3. The user opens the billing run detail or list view where the Terminate button is shown.

**Steps:**
1. Open the billing run list or detail page.
2. Select or open a billing run that is in DRAFT (or another allowed status).
3. Locate the Terminate button on the page.
4. Verify that the button is visible and is not disabled (e.g. can be clicked).

**Expected result:** The Terminate button is visible and enabled. The user can click it to initiate termination. The button state reflects that termination is allowed for this run.

**References:** PDT-2023; UI Terminate button; availableStatusesForTermination.

---

## TC-2 (Positive): After user clicks Terminate, run shows "in progress termination" and button is disabled

**Objective:** Verify that once the user clicks Terminate, the billing run transitions to (or displays) the new status "in progress termination", and the Terminate button becomes disabled (or hidden) so that the user cannot trigger a second termination.

**Preconditions:**
1. User is logged in with permission to terminate.
2. A billing run exists in an allowed status (e.g. GENERATED or DRAFT).
3. The Terminate button is visible and enabled.

**Steps:**
1. Open the billing run and click the Terminate button.
2. Observe the UI: the status shown for the billing run and the state of the Terminate button.
3. Optionally refresh or re-open the run and verify the status is "in progress termination" (or equivalent label) and the button remains disabled until termination completes.
4. Wait for termination to complete (or check via API) and verify the run eventually shows CANCELLED.

**Expected result:** After clicking Terminate, the run shows the new status "in progress termination" (or the exact label used in the UI). The Terminate button is disabled (or hidden) during this time. When termination completes, the run shows CANCELLED and the Terminate button is no longer available or remains disabled for CANCELLED runs.

**References:** PDT-2023; new status "in progress termination"; disabling terminate button.

---

## TC-3 (Positive): API returns or reflects "in progress termination" status when termination is in progress

**Objective:** Verify that the backend and API expose the new status "in progress termination" (or equivalent enum/code) so that the UI and other consumers can display and react to it correctly.

**Preconditions:**
1. A billing run exists in an allowed status.
2. A terminate request has been sent (PATCH /billing-run/terminate) and the run is in the process of being terminated but has not yet reached CANCELLED.

**Steps:**
1. Call PATCH /billing-run/terminate for a billing run in DRAFT (or another allowed status).
2. Immediately (or shortly after) call GET billing run (or the endpoint that returns the run status) for the same run ID.
3. Check the response: the status field should indicate "in progress termination" or the equivalent value used by the system.
4. After termination completes, call GET again and verify the status is CANCELLED.

**Expected result:** The GET response shows the run in "in progress termination" (or equivalent) while termination is in progress. After completion, the status is CANCELLED. The API does not return an allowed-for-termination status (e.g. DRAFT) once termination has started.

**References:** PDT-2023; new status; PATCH /billing-run/terminate; BillingRunService.cancel().

---

## TC-4 (Negative): Terminate button is disabled or hidden when run is CANCELLED

**Objective:** Verify that for a billing run that is already CANCELLED, the Terminate button is not available (disabled or hidden). The user must not be able to trigger termination again.

**Preconditions:**
1. User is logged in with permission to terminate.
2. A billing run exists and its status is CANCELLED.
3. The user opens the billing run detail or list view.

**Steps:**
1. Open the billing run list or detail for a run that is CANCELLED.
2. Look for the Terminate button.
3. Verify that the button is either not visible or is disabled and cannot be clicked.

**Expected result:** The Terminate button is disabled or hidden for CANCELLED runs. The user cannot initiate another termination. This aligns with the backend rule that CANCELLED is not in the allowed statuses for termination.

**References:** PDT-2023; UI Terminate button; availableStatusesForTermination.

---

## TC-5 (Negative): Terminate button is disabled or hidden when run is "in progress termination"

**Objective:** Verify that while the billing run is in "in progress termination" status, the Terminate button is disabled (or hidden) so that the user cannot send a duplicate terminate request from the UI.

**Preconditions:**
1. A billing run is currently in "in progress termination" status (e.g. after the user has just clicked Terminate and the run has not yet transitioned to CANCELLED).
2. The user is on the billing run detail or list view.

**Steps:**
1. Ensure the run is in "in progress termination" (e.g. by having just triggered termination and refreshing or staying on the page).
2. Locate the Terminate button.
3. Verify that the button is disabled or hidden and cannot be clicked.

**Expected result:** The Terminate button is disabled or hidden during "in progress termination". The user cannot click it again. This prevents duplicate termination and matches the backend behaviour that should reject a second terminate for a run already in this status.

**References:** PDT-2023; disabling terminate button; new status "in progress termination".

---

## TC-6 (Negative): Terminate button is disabled or hidden for IN_PROGRESS_ACCOUNTING

**Objective:** Verify that when the billing run is in IN_PROGRESS_ACCOUNTING, the Terminate button is not available (disabled or hidden), consistent with the rule that this status is not allowed for termination.

**Preconditions:**
1. A billing run exists in IN_PROGRESS_ACCOUNTING status.
2. User has permission to terminate (where allowed).
3. User opens the billing run view.

**Steps:**
1. Open the billing run that is in IN_PROGRESS_ACCOUNTING.
2. Look for the Terminate button.
3. Verify the button is disabled or hidden.

**Expected result:** The Terminate button is disabled or hidden for runs in IN_PROGRESS_ACCOUNTING. The UI does not offer termination for this status, consistent with the API rejecting terminate for this status.

**References:** PDT-2023; UI Terminate button; availableStatusesForTermination.

---

## References

- **Jira:** PDT-2023 – Change in billing run termination; disabling terminate button and adding new status "in progress termination".
- **Entry points:** UI Terminate button; PATCH /billing-run/terminate.
- **What could break:** Terminate button/API behaviour.
- **Related:** BillingRunService.cancel(); billing run status enum.
