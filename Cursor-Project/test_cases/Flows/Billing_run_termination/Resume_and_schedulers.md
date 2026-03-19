# Billing Run Termination – Resume and Schedulers (PDT-2023)

**Jira:** PDT-2023 (Phoenix)  
**Type:** Task  
**Summary:** After introducing the status IN_PROGRESS_TERMINATION, the resume() method and all schedulers that process billing runs must reject or exclude runs in IN_PROGRESS_TERMINATION and CANCELLED. This document covers resume() behaviour and scheduler exclusion to avoid processing runs that are being terminated or already cancelled.

**Scope:** resume() must reject billing runs in status IN_PROGRESS_TERMINATION and CANCELLED with a clear error. Schedulers that pick up billing runs for processing (e.g. draft, generation, accounting) must not select or process runs in IN_PROGRESS_TERMINATION or CANCELLED. This prevents runs from being resumed or advanced while termination is in progress or after they have been cancelled.

---

## Test data (preconditions)

- **Environment:** Test (or as specified in the ticket).
- **Billing run (IN_PROGRESS_TERMINATION):** A billing run in status IN_PROGRESS_TERMINATION (e.g. terminate was called and the run has not yet transitioned to CANCELLED).
- **Billing run (CANCELLED):** A billing run that has been terminated (status CANCELLED).
- **Billing run (resumable):** A billing run in a status that allows resume (e.g. PAUSED with processStage that allows resume).
- **API/service access:** Ability to call resume (e.g. POST /billing-run/{id}/resume or BillingRunService.resume()) and to trigger or observe schedulers.

---

## TC-1 (Negative): resume() rejects billing run in status IN_PROGRESS_TERMINATION

**Objective:** Verify that when the user or a scheduler invokes resume() for a billing run that is in status IN_PROGRESS_TERMINATION, the system rejects the request with a clear error message so that the run is not resumed while termination is in progress.

**Preconditions:**
1. A billing run exists and its status is IN_PROGRESS_TERMINATION.
2. The resume endpoint or BillingRunService.resume() is available (e.g. POST /billing-run/{id}/resume or equivalent).
3. The caller has permission to resume billing runs.

**Steps:**
1. Call the resume endpoint (or BillingRunService.resume(billingRunId, true)) with the ID of the billing run in IN_PROGRESS_TERMINATION.
2. Observe the response: status code and error message.
3. Verify that the billing run status remains IN_PROGRESS_TERMINATION (or has become CANCELLED by the termination flow) and that no resume logic (e.g. processStage change, status change to IN_PROGRESS_*) was applied.
4. Optionally verify in the database that the run was not modified by resume (e.g. processStage unchanged, status still IN_PROGRESS_TERMINATION or CANCELLED).

**Expected result:** The API returns an error (e.g. HTTP 400) with a message such as "Cannot resume billing run with IN_PROGRESS_TERMINATION status" or "Termination in progress". The run is not resumed. No processStage or status change is applied by resume. This addresses "Resume after cancel (resume() should reject IN_PROGRESS_TERMINATION/CANCELLED)".

**References:** PDT-2023; what_could_break – "resume() allowing that status"; integration_points – "schedulers and resume() must not process IN_PROGRESS_TERMINATION".

---

## TC-2 (Negative): resume() rejects billing run in status CANCELLED

**Objective:** Verify that when resume() is invoked for a billing run that is already CANCELLED, the system rejects the request with a clear error so that cancelled runs are never resumed.

**Preconditions:**
1. A billing run exists and its status is CANCELLED (already terminated).
2. The resume endpoint or BillingRunService.resume() is available.
3. The caller has permission to resume billing runs.

**Steps:**
1. Call the resume endpoint (or BillingRunService.resume(billingRunId, true)) with the ID of the cancelled billing run.
2. Observe the response: status code and error message.
3. Verify that the billing run status remains CANCELLED and that no resume logic was applied (e.g. processStage and status unchanged).
4. Optionally check that no scheduler or background job picks up this run after the failed resume attempt.

**Expected result:** The API returns an error (e.g. HTTP 400) with a message such as "Cannot resume billing run with CANCELLED status" or "Billing run must be cancelled first or recreated". The run remains CANCELLED. No resume logic runs. This ensures "Resume after cancel (resume() should reject IN_PROGRESS_TERMINATION/CANCELLED)" is satisfied for CANCELLED.

**References:** PDT-2023; what_could_break – "Resume after cancel"; BillingRunService.resume() must check CANCELLED (and IN_PROGRESS_TERMINATION).

---

## TC-3 (Positive): resume() allows billing run in a resumable status (e.g. PAUSED)

**Objective:** Verify that resume() still works for billing runs that are in a status that allows resume (e.g. PAUSED with a processStage that can be resumed), so that the new checks for IN_PROGRESS_TERMINATION and CANCELLED do not block legitimate resume operations.

**Preconditions:**
1. A billing run exists and is in a resumable state (e.g. status PAUSED and processStage such that resume is allowed by business rules).
2. The resume endpoint or BillingRunService.resume() is available.
3. The caller has permission to resume billing runs.

**Steps:**
1. Call the resume endpoint (or BillingRunService.resume(billingRunId, true)) with the ID of the resumable billing run.
2. Observe the response: success (e.g. HTTP 200 or 204).
3. Verify that the billing run has been updated (e.g. status changed to an IN_PROGRESS_* value or processStage advanced as per business logic).
4. Confirm that the run is now in progress and not stuck in PAUSED.

**Expected result:** The resume request succeeds. The billing run transitions to the expected "in progress" state according to business rules. No regression: resume still works for runs that are not IN_PROGRESS_TERMINATION or CANCELLED.

**References:** PDT-2023; regression – resume() must not be broken for valid resumable runs.

---

## TC-4 (Regression): Schedulers do not pick up billing runs in IN_PROGRESS_TERMINATION

**Objective:** Verify that all schedulers that select billing runs for processing (e.g. draft preparation, generation, accounting) exclude runs in status IN_PROGRESS_TERMINATION so that no scheduler starts or continues work on a run that is being terminated.

**Preconditions:**
1. At least one billing run exists in status IN_PROGRESS_TERMINATION (e.g. created by calling terminate and ensuring the status is set before completion to CANCELLED, or set via test data).
2. The schedulers that process billing runs are known (e.g. BillingRunStandardPreparationStateHandler, BillingRunStartGenerationScheduler, BillingRunStartAccountingScheduler or equivalent).
3. Access to trigger the scheduler or to inspect the query/criteria used to select runs.

**Steps:**
1. Identify the scheduler(s) that select billing runs (e.g. by status or processStage).
2. Ensure a billing run is in IN_PROGRESS_TERMINATION.
3. Trigger the scheduler (or run the job that uses the same selection logic) and verify that the run in IN_PROGRESS_TERMINATION is not selected for processing.
4. Optionally verify the repository or query used by the scheduler: the WHERE clause or criteria must exclude status IN_PROGRESS_TERMINATION (and CANCELLED).
5. After the run transitions to CANCELLED, trigger the scheduler again and confirm the run is still not selected.

**Expected result:** No scheduler picks up billing runs in IN_PROGRESS_TERMINATION. The run is not processed (e.g. no draft step, no generation step, no accounting step) while it is in that status. This addresses "schedulers not excluding it" from what_could_break.

**References:** PDT-2023; what_could_break – "schedulers not excluding it"; integration_points – "schedulers and resume() must not process IN_PROGRESS_TERMINATION".

---

## TC-5 (Regression): Schedulers do not pick up billing runs in CANCELLED

**Objective:** Verify that all schedulers that select billing runs for processing exclude runs in status CANCELLED so that cancelled runs are never processed again.

**Preconditions:**
1. At least one billing run exists in status CANCELLED.
2. The schedulers that process billing runs are known.
3. Access to trigger the scheduler or to inspect the selection logic.

**Steps:**
1. Identify the scheduler(s) that select billing runs.
2. Trigger the scheduler (or run the job) and verify that the run in CANCELLED is not selected.
3. Optionally verify the repository or query: the selection criteria must exclude status CANCELLED (e.g. status NOT IN (CANCELLED, IN_PROGRESS_TERMINATION, DELETED) or equivalent).
4. Confirm that no automatic transition (e.g. CANCELLED → COMPLETED as in historical reports) can occur because of scheduler processing.

**Expected result:** No scheduler picks up billing runs in CANCELLED. Cancelled runs remain in CANCELLED and are not advanced or resumed by any background job. This prevents the scenario where a CANCELLED run could be resumed and moved to COMPLETED.

**References:** PDT-2023; what_could_break – "schedulers not excluding it"; BillingRunLockAnalysis / AutoCompleted_BillingRuns_Analysis (CANCELLED → COMPLETED scenario).

---

## References

- **Jira:** PDT-2023 – Billing run termination; resume and schedulers must respect IN_PROGRESS_TERMINATION and CANCELLED.
- **What could break:** resume() allowing that status; schedulers not excluding it.
- **Integration:** Schedulers and resume() must not process IN_PROGRESS_TERMINATION (or CANCELLED).
