# Billing Run Termination – List and Filters (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** The new status IN_PROGRESS_TERMINATION must be supported in the billing run list and in any status filters or API responses so that users can see and filter runs that are being terminated. This document covers list display, filtering by the new status, and API/enum compatibility.

**Scope:** Billing run list (UI and API) and filters. The list must display runs in status IN_PROGRESS_TERMINATION with a clear label. Filtering by status must include IN_PROGRESS_TERMINATION. Any API that returns billing run status (e.g. GET list, GET by ID) must include the new enum value so that the UI and consumers do not break (e.g. "unknown status" or missing filter option). Addresses "Billing run list and filters (new status IN_PROGRESS_TERMINATION in UI/API)" and "API/tests assuming fixed enum".

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Billing run (IN_PROGRESS_TERMINATION):** At least one billing run in status IN_PROGRESS_TERMINATION (e.g. by calling PATCH /billing-run/terminate and ensuring the run is in this state when the list is queried, or by test data).
- **Billing run (other statuses):** Billing runs in other statuses (e.g. GENERATED, CANCELLED, COMPLETED) to verify list and filters show all statuses correctly.
- **UI and API access:** Access to the billing run list (UI) and to the list/filter API (e.g. GET /billing-run or equivalent with status filter).

---

## TC-1 (Positive): Billing run list displays runs in status IN_PROGRESS_TERMINATION with correct label

**Objective:** Verify that when the billing run list (UI or API) is loaded, any run that is in status IN_PROGRESS_TERMINATION is displayed with a correct, human-readable label (e.g. "In progress termination" or "Terminating") so that the user can see which runs are currently being terminated.

**Preconditions:**
1. At least one billing run exists in status IN_PROGRESS_TERMINATION (e.g. terminate was just called and the run has not yet transitioned to CANCELLED).
2. The billing run list is available (UI table or API that returns a list of billing runs with status).
3. The list shows a status column or status field for each run.

**Steps:**
1. Open the billing run list (UI or call GET list API).
2. Locate the run that is in IN_PROGRESS_TERMINATION (e.g. by ID or by triggering terminate and refreshing the list before it becomes CANCELLED).
3. Read the status value displayed for that run in the list (UI label or API response field).
4. Verify that the label is correct and distinct from "Cancelled", "Generated", "Completed", etc. (e.g. "In progress termination" or "Terminating").
5. If the list is from an API, verify that the status field in the response contains the enum value IN_PROGRESS_TERMINATION (or the string used by the API) so that the UI can map it to the correct label.

**Expected result:** The list displays runs in IN_PROGRESS_TERMINATION with a clear, correct label. The API (if used) returns the new status value so that no "unknown" or missing status appears. This addresses "Billing run list and filters (new status IN_PROGRESS_TERMINATION in UI/API)".

**References:** PDT-2023; what_could_break – "Billing run list and filters"; integration_points – new status in UI/API.

---

## TC-2 (Positive): Filter by status IN_PROGRESS_TERMINATION returns only runs in that status

**Objective:** Verify that when the user (or API client) applies a filter for status IN_PROGRESS_TERMINATION, the list returns only billing runs that are in that status, so that users can quickly find runs that are currently being terminated.

**Preconditions:**
1. At least one billing run exists in status IN_PROGRESS_TERMINATION.
2. At least one billing run exists in another status (e.g. GENERATED or CANCELLED).
3. The list supports filtering by status (UI dropdown/filter or API query parameter such as status=IN_PROGRESS_TERMINATION).

**Steps:**
1. Open the billing run list and apply the status filter for "In progress termination" (or IN_PROGRESS_TERMINATION in the API).
2. Verify that the list shows only runs that are in IN_PROGRESS_TERMINATION (e.g. check each row's status or the API response items).
3. Verify that runs in other statuses (e.g. GENERATED, CANCELLED) are not included in the filtered result.
4. Remove the filter or select "All" and confirm that runs in IN_PROGRESS_TERMINATION appear again in the full list with the correct status label.

**Expected result:** The filter for IN_PROGRESS_TERMINATION returns only runs in that status. The filter is consistent with the data (no runs in other statuses appear when this filter is applied). This completes coverage for "Billing run list and filters (new status IN_PROGRESS_TERMINATION in UI/API)".

**References:** PDT-2023; what_could_break – "Billing run list and filters".

---

## TC-3 (Regression): API and enum include IN_PROGRESS_TERMINATION – no fixed enum assumption

**Objective:** Verify that any API that returns billing run status (e.g. GET billing run by ID, GET billing run list, or a status enum endpoint) includes the value IN_PROGRESS_TERMINATION so that existing clients or tests that assume a fixed set of statuses do not break (e.g. no "unknown" status, no validation error when the backend returns the new value). Addresses "API/tests assuming fixed enum".

**Preconditions:**
1. The product exposes billing run status via API (REST or other).
2. There may be existing tests or clients that iterate over statuses or validate allowed values.
3. At least one billing run can be in IN_PROGRESS_TERMINATION for the test.

**Steps:**
1. Call GET billing run by ID (or equivalent) for a run that is in IN_PROGRESS_TERMINATION and verify the response body contains the status field with value IN_PROGRESS_TERMINATION (or the exact string/enum used by the API).
2. If the API has a "get statuses" or "get enum" endpoint (e.g. for dropdowns), call it and verify that IN_PROGRESS_TERMINATION is present in the list of billing run statuses.
3. Run any existing automated tests that use billing run status (e.g. list, filter, or status validation) and ensure they either include the new status in expected values or do not assume a fixed enum that excludes IN_PROGRESS_TERMINATION (update tests if they previously assumed a fixed set).
4. Verify that the UI (or client) does not show "Unknown status" or throw when receiving IN_PROGRESS_TERMINATION from the API.

**Expected result:** The API returns IN_PROGRESS_TERMINATION as a valid status value. No client or test fails because of a fixed enum assumption (e.g. switch/case or allowed-values list that omits the new status). If tests exist that validate status enum, they are updated to include IN_PROGRESS_TERMINATION. This addresses "API/tests assuming fixed enum" from what_could_break.

**References:** PDT-2023; what_could_break – "API/tests assuming fixed enum"; BillingStatus enum.

---

## TC-4 (Positive): List and filters still work for all other statuses (no regression)

**Objective:** Ensure that after adding IN_PROGRESS_TERMINATION, the billing run list and filters still correctly display and filter by all existing statuses (e.g. INITIAL, DRAFT, IN_PROGRESS_DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED, COMPLETED, CANCELLED, DELETED) so that there is no regression on existing behaviour.

**Preconditions:**
1. Billing runs exist in several statuses (e.g. GENERATED, PAUSED, CANCELLED, COMPLETED).
2. The list and status filter are available.

**Steps:**
1. Open the billing run list without filter and verify that runs in GENERATED, PAUSED, CANCELLED, COMPLETED (and others if applicable) are displayed with correct labels.
2. Apply the filter for each of these statuses (e.g. CANCELLED, GENERATED) and verify that only runs in that status are returned.
3. Verify that the order or pagination of the list is not broken and that the new status does not cause any layout or display error (e.g. column width, missing translation).
4. If the API returns a list of available statuses for the filter, verify that both the new status (IN_PROGRESS_TERMINATION) and all existing statuses are present.

**Expected result:** All existing statuses continue to display and filter correctly. No regression: list and filters work for INITIAL, DRAFT, IN_PROGRESS_DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED, COMPLETED, CANCELLED, and IN_PROGRESS_TERMINATION. The new status is additive and does not break existing behaviour.

**References:** PDT-2023; regression – list and filters.

---

## References

- **Jira:** PDT-2023 – Billing run termination; list and filters for new status.
- **What could break:** Billing run list and filters (new status IN_PROGRESS_TERMINATION in UI/API); API/tests assuming fixed enum.
- **BillingStatus:** Include IN_PROGRESS_TERMINATION in enum and in all list/filter responses.
