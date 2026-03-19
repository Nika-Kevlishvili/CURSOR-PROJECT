# Billing Run Termination – Allowed Statuses and Rejections (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** Billing run termination change: terminate is allowed only for runs in specific statuses (INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED). Termination adds a new status "in progress termination" and disables the terminate button. This document tests which statuses allow terminate and which must be rejected.

**Scope:** The flow covers the decision logic for when a billing run can be terminated. The backend (PATCH /billing-run/terminate and BillingRunService.cancel()) must allow termination only when the run is in one of: INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED. For any other status (e.g. CANCELLED, IN_PROGRESS_ACCOUNTING, or the new "in progress termination"), the system must reject the request with a clear error. The UI Terminate button must be shown only when termination is allowed and hidden or disabled when not.

---

## Test data (preconditions)

- **Environment:** Test or Dev (as per ticket).
- **Billing run:** At least one billing run exists. For positive cases, the run is in one of: INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED. For negative cases, the run may be in CANCELLED, IN_PROGRESS_ACCOUNTING, or "in progress termination" (or equivalent), or the run ID may be missing or invalid.
- **User/API:** The caller has permission to terminate billing runs (e.g. appropriate role or API token).
- **Backend:** The service uses the list of allowed statuses (availableStatusesForTermination) when deciding whether to accept the terminate request.

---

## TC-1 (Positive): Terminate billing run in INITIAL status

**Objective:** Verify that a user or API can successfully request termination of a billing run that is in INITIAL status. The system must accept the request and proceed with termination (e.g. transition to "in progress termination" and then to CANCELLED).

**Preconditions:**
1. A billing run exists and its current status is INITIAL.
2. The user has permission to terminate billing runs.
3. No other operation is concurrently changing this billing run's status.

**Steps:**
1. Identify the billing run ID (e.g. from the billing run list or API).
2. Call PATCH /billing-run/terminate with the billing run identifier (or use the UI Terminate button).
3. Observe the response (e.g. HTTP 200 or 204) and, if applicable, the response body.
4. Optionally verify the billing run status after the call (e.g. "in progress termination" then CANCELLED, or CANCELLED directly, depending on implementation).

**Expected result:** The request is accepted. The system returns success (e.g. 200 OK or 204 No Content). The billing run moves toward or reaches a terminal state (e.g. "in progress termination" then CANCELLED). No validation error about "status not allowed for termination" is returned.

**Actual result (if bug):** (Leave blank unless documenting a known defect.)

**References:** PDT-2023; availableStatusesForTermination; BillingRunService.cancel(); PATCH /billing-run/terminate.

---

## TC-2 (Positive): Terminate billing run in IN_PROGRESS_DRAFT status

**Objective:** Verify that termination is allowed when the billing run is in IN_PROGRESS_DRAFT status. The system must accept the terminate request and process it.

**Preconditions:**
1. A billing run exists and its current status is IN_PROGRESS_DRAFT.
2. The user has permission to terminate billing runs.

**Steps:**
1. Locate the billing run in IN_PROGRESS_DRAFT (e.g. via API or UI list).
2. Call PATCH /billing-run/terminate with that billing run's ID (or click Terminate in the UI).
3. Check the response and the resulting status of the billing run.

**Expected result:** The terminate request succeeds. The billing run is terminated (or moves to "in progress termination" then CANCELLED). The system does not reject with "status not allowed for termination".

**References:** PDT-2023; availableStatusesForTermination.

---

## TC-3 (Positive): Terminate billing run in DRAFT status

**Objective:** Verify that a billing run in DRAFT status can be terminated. The API and UI must allow termination for this status.

**Preconditions:**
1. A billing run exists in DRAFT status.
2. The user has permission to terminate billing runs.

**Steps:**
1. Select the billing run in DRAFT status.
2. Invoke termination (PATCH /billing-run/terminate or UI Terminate button).
3. Verify success response and final status.

**Expected result:** Termination is accepted and the billing run is terminated (or goes through "in progress termination" to CANCELLED).

**References:** PDT-2023; availableStatusesForTermination.

---

## TC-4 (Positive): Terminate billing run in IN_PROGRESS_GENERATION status

**Objective:** Verify that termination is allowed when the billing run is in IN_PROGRESS_GENERATION. The system must accept the request and stop or cancel the run appropriately.

**Preconditions:**
1. A billing run exists in IN_PROGRESS_GENERATION status.
2. The user has permission to terminate billing runs.

**Steps:**
1. Identify a billing run in IN_PROGRESS_GENERATION.
2. Call PATCH /billing-run/terminate (or use UI Terminate).
3. Verify the response and that the run is no longer in IN_PROGRESS_GENERATION (e.g. moves to "in progress termination" then CANCELLED).

**Expected result:** The request is accepted. The run is terminated; no error indicating that this status is not allowed for termination.

**References:** PDT-2023; availableStatusesForTermination.

---

## TC-5 (Positive): Terminate billing run in GENERATED status

**Objective:** Verify that a billing run that has completed generation (GENERATED) can still be terminated. The business may allow cancelling a run that is generated but not yet accounted.

**Preconditions:**
1. A billing run exists in GENERATED status.
2. The user has permission to terminate billing runs.

**Steps:**
1. Select the billing run in GENERATED status.
2. Request termination via API or UI.
3. Verify success and final status (e.g. CANCELLED or "in progress termination" then CANCELLED).

**Expected result:** Termination is accepted. The billing run is terminated successfully.

**References:** PDT-2023; availableStatusesForTermination.

---

## TC-6 (Positive): Terminate billing run in PAUSED status

**Objective:** Verify that a billing run in PAUSED status can be terminated. The system must allow termination for PAUSED runs.

**Preconditions:**
1. A billing run exists in PAUSED status.
2. The user has permission to terminate billing runs.

**Steps:**
1. Locate the billing run in PAUSED status.
2. Call PATCH /billing-run/terminate (or use UI Terminate).
3. Verify the response and the billing run's resulting status.

**Expected result:** The terminate request succeeds. The billing run is terminated (or transitions through "in progress termination" to CANCELLED).

**References:** PDT-2023; availableStatusesForTermination.

---

## TC-7 (Negative): Terminate rejected when billing run is already CANCELLED

**Objective:** Verify that the system rejects a terminate request when the billing run is already in CANCELLED status. No duplicate termination or state corruption should occur.

**Preconditions:**
1. A billing run exists and its status is CANCELLED.
2. The user has permission to terminate billing runs.

**Steps:**
1. Identify the billing run ID that is in CANCELLED status.
2. Call PATCH /billing-run/terminate with that ID (or attempt to use the Terminate button if it is still visible).
3. Observe the response: status code and error message.

**Expected result:** The system returns an error (e.g. 400 Bad Request or 409 Conflict). The error message clearly indicates that the billing run cannot be terminated because it is already CANCELLED (or similar). The billing run status remains CANCELLED. No duplicate termination is performed.

**References:** PDT-2023; availableStatusesForTermination; regression: terminate button/API behaviour.

---

## TC-8 (Negative): Terminate rejected when billing run is in "in progress termination"

**Objective:** Verify that if a new status "in progress termination" (or equivalent) exists, a second terminate request for the same run is rejected while the run is in that status. This prevents duplicate termination and ensures the button is effectively disabled during termination.

**Preconditions:**
1. A billing run exists and is in "in progress termination" status (e.g. after a first terminate request has been accepted but before it has transitioned to CANCELLED).
2. The user has permission to terminate billing runs.

**Steps:**
1. Obtain a billing run that is currently in "in progress termination" status.
2. Call PATCH /billing-run/terminate again with the same billing run ID (or attempt to click Terminate again if the UI still exposes it).
3. Observe the response.

**Expected result:** The system rejects the second request with an appropriate error (e.g. 400 or 409) indicating that the run is already being terminated or is not in a status that allows termination. The run continues its transition to CANCELLED without being affected by the second request.

**References:** PDT-2023; new status "in progress termination"; disable terminate button.

---

## TC-9 (Negative): Terminate rejected when billing run is IN_PROGRESS_ACCOUNTING

**Objective:** Verify that the system does not allow termination when the billing run is in IN_PROGRESS_ACCOUNTING, as this status is not in the allowed list. The API must return a clear error.

**Preconditions:**
1. A billing run exists in IN_PROGRESS_ACCOUNTING status.
2. The user has permission to terminate billing runs.

**Steps:**
1. Identify a billing run in IN_PROGRESS_ACCOUNTING status.
2. Call PATCH /billing-run/terminate with that billing run's ID.
3. Check the response status and body.

**Expected result:** The system returns an error (e.g. 400 Bad Request). The message indicates that the billing run cannot be terminated in its current status (e.g. "Termination not allowed for status IN_PROGRESS_ACCOUNTING" or similar). The billing run status remains unchanged.

**References:** PDT-2023; availableStatusesForTermination.

---

## TC-10 (Negative): Terminate rejected when billing run ID is missing or invalid

**Objective:** Verify that the API rejects the request when the billing run identifier is missing, malformed, or does not exist. No partial or erroneous termination should occur.

**Preconditions:**
1. The user has access to the terminate API (e.g. valid authentication).
2. No valid billing run ID is provided, or an ID that does not exist is used.

**Steps:**
1. Call PATCH /billing-run/terminate with a missing billing run ID (e.g. empty path parameter or missing body field, depending on API design).
2. Call PATCH /billing-run/terminate with an invalid format (e.g. non-numeric or wrong type).
3. Call PATCH /billing-run/terminate with a valid format but non-existent ID (e.g. 999999999).
4. Observe the response for each attempt.

**Expected result:** For missing or invalid ID: the system returns 400 Bad Request (or 404 for non-existent ID). The error message clearly indicates what is wrong (e.g. "Billing run ID is required", "Billing run not found"). No billing run is modified.

**References:** PDT-2023; PATCH /billing-run/terminate; validation.

---

## TC-11 (Negative): Terminate rejected when user has no permission

**Objective:** Verify that the system rejects a terminate request when the caller does not have permission to terminate billing runs. Authorization must be enforced.

**Preconditions:**
1. A billing run exists in a status that allows termination (e.g. DRAFT or GENERATED).
2. The caller uses credentials or a role that does not have permission to terminate billing runs.

**Steps:**
1. Call PATCH /billing-run/terminate with the billing run ID using the unauthorized caller's token or session.
2. If testing UI, log in as a user without terminate permission and attempt to click Terminate.
3. Observe the response (e.g. 403 Forbidden) and that the billing run status is unchanged.

**Expected result:** The system returns 403 Forbidden (or equivalent). The error indicates that the user is not allowed to perform this action. The billing run is not modified.

**References:** PDT-2023; permissions; authorization.

---

## References

- **Jira:** PDT-2023 – Change in billing run termination; disabling terminate button and adding new status "in progress termination".
- **Entry points:** PATCH /billing-run/terminate; BillingRunService.cancel(); UI Terminate button.
- **Allowed statuses for termination:** INITIAL, IN_PROGRESS_DRAFT, DRAFT, IN_PROGRESS_GENERATION, GENERATED, PAUSED.
- **Related:** BillingRunRepository; billing_run.terminate_billing_run(?); Lock.
