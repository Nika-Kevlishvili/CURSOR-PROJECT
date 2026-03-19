# Billing Run Termination – Billing Run List and Filters for CANCELLED and New Status (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** Regression tests for the billing run list and filters: runs in CANCELLED status and runs in the new "in progress termination" status must appear correctly in lists and be filterable so that users can find and manage terminated runs.

**Scope:** The billing run list (UI and/or API) must include runs in CANCELLED and, if applicable, "in progress termination" status. Filters (e.g. by status) must allow users to filter by CANCELLED and by "in progress termination" so that the new status does not break existing list or filter behaviour.

---

## Test data (preconditions)

- **Environment:** Test or Dev.
- **Billing runs:** At least one billing run in CANCELLED status and, if achievable, at least one in "in progress termination" status (e.g. by triggering termination and querying the list before it completes).
- **List/API:** The billing run list is available via UI (e.g. table or grid) and/or via an API (e.g. GET list of billing runs with optional status filter).

---

## TC-1 (Positive): Billing run list includes runs in CANCELLED status

**Objective:** Verify that the billing run list (UI or API) includes runs that are in CANCELLED status. Terminated runs must not disappear from the list unless the product explicitly hides them by design; if they are intended to be visible, they must appear with status CANCELLED.

**Preconditions:**
1. At least one billing run exists and is in CANCELLED status.
2. The user has permission to view the billing run list.
3. The list is configured to show runs in CANCELLED (e.g. no filter excluding CANCELLED, or a filter that includes CANCELLED).

**Steps:**
1. Open the billing run list (UI or call GET list API).
2. If filters exist, ensure CANCELLED is not excluded (e.g. select "All" or include "CANCELLED" in status filter).
3. Locate the billing run that was terminated (CANCELLED).
4. Verify that it appears in the list and that its status is displayed as CANCELLED (or the exact label used in the UI).

**Expected result:** The billing run in CANCELLED status appears in the list. The status column (or equivalent) shows CANCELLED. No regression: list does not hide CANCELLED runs unless that is an explicit product decision.

**References:** PDT-2023; what could break: billing run list/filters for CANCELLED.

---

## TC-2 (Positive): Billing run list includes runs in "in progress termination" status (if list shows in-progress runs)

**Objective:** Verify that when the list is configured to show runs in progress (or all statuses), runs in "in progress termination" status appear in the list with that status clearly indicated. The new status must be supported in the list data and display.

**Preconditions:**
1. A billing run has been sent a terminate request and is currently in "in progress termination" status (e.g. trigger termination and open the list before it completes).
2. The list shows runs in non-final statuses (or "all" statuses).
3. The user has permission to view the list.

**Steps:**
1. Trigger termination for a billing run and immediately open the billing run list (or call GET list API).
2. Before the run reaches CANCELLED, locate it in the list.
3. Verify that the run is visible and that its status is displayed as "in progress termination" (or the exact label/code used).
4. Optionally refresh the list and confirm the run later shows as CANCELLED when termination completes.

**Expected result:** Runs in "in progress termination" appear in the list when the list includes that status. The status is displayed correctly. When termination completes, the same run appears as CANCELLED after refresh. No regression: the new status is integrated into the list and does not cause missing or wrong rows.

**References:** PDT-2023; new status "in progress termination"; billing run list/filters.

---

## TC-3 (Positive): Filter by status CANCELLED returns only CANCELLED runs

**Objective:** Verify that when the user (or API) filters the billing run list by status CANCELLED, only runs in CANCELLED status are returned. Runs in other statuses (e.g. DRAFT, "in progress termination") must not appear in the filtered result.

**Preconditions:**
1. At least one billing run is in CANCELLED status and at least one is in another status (e.g. DRAFT or GENERATED).
2. The list supports filtering by status (e.g. dropdown or API query parameter).

**Steps:**
1. Open the billing run list and apply a filter for status = CANCELLED (or call GET list with status=CANCELLED if the API supports it).
2. Verify that all rows in the result have status CANCELLED.
3. Verify that runs in DRAFT, GENERATED, or "in progress termination" do not appear in this filtered result (unless the product allows multiple statuses in one filter).
4. Remove the filter or select "All" and confirm that the CANCELLED run(s) appear when CANCELLED is included.

**Expected result:** The filter by CANCELLED returns only CANCELLED runs. No runs in other statuses are included. The filter works correctly for the CANCELLED status. No regression after introducing "in progress termination".

**References:** PDT-2023; what could break: billing run list/filters for CANCELLED.

---

## TC-4 (Positive): Filter by "in progress termination" returns only runs in that status (if filter is supported)

**Objective:** If the product supports filtering the billing run list by the new status "in progress termination", verify that selecting this filter returns only runs that are currently in "in progress termination". Other statuses must not appear in the result.

**Preconditions:**
1. The list supports a status filter that includes "in progress termination" (or equivalent).
2. At least one billing run is in "in progress termination" (e.g. terminate was just triggered and the run has not yet reached CANCELLED).
3. Other runs exist in different statuses (e.g. CANCELLED, DRAFT).

**Steps:**
1. Trigger termination for one billing run so it is in "in progress termination".
2. Open the list and apply the filter for "in progress termination" (if available).
3. Verify that only runs in "in progress termination" appear in the result.
4. Verify that CANCELLED and other statuses do not appear in this filtered result.
5. Optionally clear the filter and confirm the run appears with the correct status in the full list.

**Expected result:** If the filter for "in progress termination" exists, it returns only runs in that status. The new status is correctly supported in the filter. If the product does not expose this filter, the run still appears in the list when "All" or a suitable group (e.g. "In progress") is selected, with the correct status displayed.

**References:** PDT-2023; new status "in progress termination"; billing run list/filters.

---

## TC-5 (Negative): List does not show runs in an undefined or wrong status after termination

**Objective:** Verify that after a billing run is terminated, the list never shows it in an undefined or incorrect status (e.g. still showing DRAFT or GENERATED when it is already CANCELLED, or showing a raw enum value instead of a user-friendly label). Data consistency and display must be correct.

**Preconditions:**
1. A billing run has been terminated and has reached CANCELLED status.
2. The list is refreshed or re-opened.

**Steps:**
1. Terminate a billing run and wait until it reaches CANCELLED.
2. Open or refresh the billing run list.
3. Find the run in the list and verify that its status is displayed as CANCELLED (or the correct label).
4. Verify that the run does not appear with an old status (e.g. DRAFT) or with a null/undefined/raw value that indicates a missing or incorrect mapping for the new flow.

**Expected result:** The list shows the terminated run with status CANCELLED. No stale or wrong status is displayed. No regression in list data or status display after the termination change.

**References:** PDT-2023; billing run list/filters for CANCELLED; new status "in progress termination".

---

## References

- **Jira:** PDT-2023 – Billing run termination; list and filters.
- **What could break:** Billing run list/filters for CANCELLED.
- **Related:** BillingRunRepository; list API; status enum and UI labels.
