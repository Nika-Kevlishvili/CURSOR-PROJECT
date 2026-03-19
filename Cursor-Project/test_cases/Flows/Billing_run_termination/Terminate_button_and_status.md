# Billing Run Termination – Terminate Button and New Status (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** Terminate button must be disabled when the billing run is in "in progress termination" or when termination is not allowed; a new status "in progress termination" (IN_PROGRESS_TERMINATION) is introduced. This document covers UI behaviour for the terminate button and display of the new status.

**Scope:** Billing run termination flow. The terminate button in the UI must be disabled when the run status is IN_PROGRESS_TERMINATION (termination already in progress) or when the run is not in one of the statuses that allow termination (INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED). The button must be enabled only when the run is in one of those allowed statuses. The UI must correctly display the new status "in progress termination" and must not allow a second terminate action while in that state.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Billing run (terminatable):** At least one billing run exists in a status that allows termination (e.g. INITIAL, DRAFT, IN_PROGRESS_GENERATION, GENERATED, or PAUSED).
- **Billing run (in progress termination):** A billing run that has been sent a terminate request and is in status IN_PROGRESS_TERMINATION (e.g. created by calling PATCH /billing-run/terminate and before the run has transitioned to CANCELLED).
- **Billing run (cancelled):** A billing run that has been terminated and is in status CANCELLED.
- **User:** User has permission to view billing runs and to terminate them when allowed.

---

## TC-1 (Positive): Terminate button enabled when billing run is in a status that allows termination

**Objective:** Verify that the terminate button is enabled in the UI when the billing run status is one of INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, or PAUSED, so that the user can start termination when the run is in an allowed state.

**Preconditions:**
1. A billing run exists and its status is one of: INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, or PAUSED.
2. The user has permission to terminate billing runs.
3. The UI displays the billing run detail or list with the terminate action available for runs that allow termination.

**Steps:**
1. Open the billing run list or detail view in the UI.
2. Locate a billing run that is in one of the allowed statuses (e.g. GENERATED or PAUSED).
3. Check whether the "Terminate" (or equivalent) button or action is visible and enabled (clickable).
4. Optionally open the run detail and confirm the terminate button is enabled there as well.

**Expected result:** The terminate button is enabled. The user can click it to start the termination flow. The system allows the user to initiate termination when the run is in an allowed status.

**References:** PDT-2023; BillingStatus.availableStatusesForTermination (INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED).

---

## TC-2 (Negative): Terminate button disabled when billing run status is "in progress termination"

**Objective:** Verify that the terminate button is disabled when the billing run is already in status IN_PROGRESS_TERMINATION, so that the user cannot trigger a second termination and the UI reflects that termination is already in progress.

**Preconditions:**
1. A billing run exists and its status is IN_PROGRESS_TERMINATION (e.g. user has just clicked Terminate and the run has transitioned to this status, or the run was set to this status for test purposes).
2. The UI displays the billing run (list or detail).

**Steps:**
1. Open the billing run list or detail view.
2. Locate the billing run that is in status IN_PROGRESS_TERMINATION (or trigger terminate once and observe the status change to "in progress termination").
3. Check whether the "Terminate" button or action is disabled (greyed out or not clickable).
4. If the button is still visible, attempt to click it and confirm that no second terminate request is sent or that the system rejects it.

**Expected result:** The terminate button is disabled when the run is in IN_PROGRESS_TERMINATION. The user cannot start a second termination. The UI clearly indicates that termination is already in progress (e.g. by disabling the button and optionally showing the status "In progress termination").

**References:** PDT-2023; what_could_break – "UI button enabled during IN_PROGRESS_TERMINATION".

---

## TC-3 (Negative): Terminate button disabled when billing run status is CANCELLED

**Objective:** Verify that the terminate button is disabled when the billing run has already been terminated and is in status CANCELLED, so that the user cannot attempt to terminate an already cancelled run.

**Preconditions:**
1. A billing run exists and its status is CANCELLED (already terminated).
2. The UI displays the billing run (list or detail).

**Steps:**
1. Open the billing run list or detail view.
2. Locate a billing run that is in status CANCELLED.
3. Check whether the "Terminate" button or action is disabled or hidden.
4. If the button is visible, attempt to click it and confirm that the system does not allow termination of an already cancelled run.

**Expected result:** The terminate button is disabled (or hidden) when the run is CANCELLED. The user cannot terminate an already cancelled run. The UI reflects the final state of the run.

**References:** PDT-2023; BillingStatus CANCELLED.

---

## TC-4 (Negative): Terminate button disabled when billing run status is COMPLETED or DELETED

**Objective:** Verify that the terminate button is disabled when the billing run is in a status that does not allow termination (e.g. COMPLETED or DELETED), so that only runs in the allowed status set can be terminated.

**Preconditions:**
1. A billing run exists and its status is COMPLETED (or DELETED if applicable in the product).
2. The UI displays the billing run (list or detail).

**Steps:**
1. Open the billing run list or detail view.
2. Locate a billing run that is in status COMPLETED (or DELETED).
3. Check whether the "Terminate" button or action is disabled or hidden.
4. Confirm that the API would also reject termination for this status (optional: call PATCH /billing-run/terminate and expect an error).

**Expected result:** The terminate button is disabled (or hidden) when the run is COMPLETED or DELETED. Termination is only allowed for the defined set of statuses (INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED). The UI and API behaviour are consistent.

**References:** PDT-2023; availableStatusesForTermination does not include COMPLETED or DELETED.

---

## TC-5 (Positive): UI displays the new status "in progress termination" correctly

**Objective:** Verify that the UI shows a clear, human-readable label for the new status IN_PROGRESS_TERMINATION (e.g. "In progress termination" or "Terminating") so that the user understands that the run is being terminated and that no further action is needed until it becomes CANCELLED.

**Preconditions:**
1. A billing run has been sent a terminate request and is in status IN_PROGRESS_TERMINATION (or can be set to this status for test).
2. The user has access to the billing run list or detail view.

**Steps:**
1. Trigger termination for a billing run (or use a run already in IN_PROGRESS_TERMINATION).
2. In the billing run list, locate the run and read the status column or label.
3. Open the billing run detail and read the status displayed there.
4. Confirm that the label is understandable (e.g. "In progress termination", "Terminating") and distinct from "Cancelled" and other statuses.

**Expected result:** The UI displays a clear label for IN_PROGRESS_TERMINATION (e.g. "In progress termination"). The user can distinguish between "in progress termination" and "Cancelled". The new status is visible in both list and detail views.

**References:** PDT-2023; integration_points – UI must reflect new status.

---

## TC-6 (Regression): Billing run list and filters include "in progress termination" status

**Objective:** Ensure that the billing run list and any status filter or dropdown include the new status IN_PROGRESS_TERMINATION, so that users can see and filter runs that are currently being terminated.

**Preconditions:**
1. At least one billing run exists in status IN_PROGRESS_TERMINATION (or the product supports this status in the list).
2. The billing run list has a status filter or status column.

**Steps:**
1. Open the billing run list.
2. If there is a status filter (dropdown or multi-select), check that "In progress termination" (or the equivalent for IN_PROGRESS_TERMINATION) is present as an option.
3. Select the "In progress termination" filter and confirm that only runs in that status are shown (if any exist).
4. In the list table, confirm that the status column can display "In progress termination" for runs in that state.

**Expected result:** The list and filters support the new status IN_PROGRESS_TERMINATION. Users can filter by this status and see it displayed in the list. No regression: existing statuses (e.g. GENERATED, CANCELLED) still display and filter correctly.

**References:** PDT-2023; what_could_break – "Billing run list and filters (new status IN_PROGRESS_TERMINATION in UI/API)".

---

## References

- **Jira:** PDT-2023 – Change in billing run termination (disable terminate button, new status "in progress termination").
- **Entry points:** UI terminate button, PATCH /billing-run/terminate, BillingRunService.cancel().
- **BillingStatus:** availableStatusesForTermination = INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED; new status IN_PROGRESS_TERMINATION; CANCELLED after termination.
