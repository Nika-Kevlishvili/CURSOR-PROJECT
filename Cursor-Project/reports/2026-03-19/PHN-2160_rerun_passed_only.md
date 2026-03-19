PHN-2160 - Playwright test results (rerun, passed-only details)

Jira: PHN-2160  
Title: Put: Update existing POD  
Date: 2026-03-19  
Spec: `Cursor-Project/EnergoTS/tests/cursor/PHN-2160-put-update-existing-pod.spec.ts`

Stats: 4 passed, 0 failed, 59 skipped (63 total).

-------------------------

Passed tests (details)

Test 1: [PHN-2160 TC-COMP-14] Update non-existent POD ID — Passed

- What is verified: Updating a non-existent POD ID returns a valid client error (no upsert/creation).
- Steps:
  - PUT update request with a non-existent numeric POD id (`999999999`) and a name-only payload.
  - Assert status is `404` or `400`.
- Result: Passed

Test 2: [PHN-2160 TC-API-4] Update non-existent POD identifier — Passed

- What is verified: Updating a non-existent POD id returns a valid client error (no upsert/creation).
- Steps:
  - PUT update request with a non-existent numeric POD id (`999999998`) and a name-only payload.
  - Assert status is `404` or `400`.
- Result: Passed

Test 3: [PHN-2160 TC-API-5] Invalid identifier format path validation — Passed

- What is verified: Invalid POD id format is rejected.
- Steps:
  - PUT update request with an invalid id value (`%%%invalid%%%`) and a name-only payload.
  - Assert status is `400` or `404`.
- Result: Passed

Test 4: [PHN-2160 TC-REG-4] /exists returns false for non-existent identifier — Passed

- What is verified: `/exists` behavior for non-existent identifier is handled consistently:
  - If endpoint returns `200`, it must return `false` (JSON boolean or `false` text).
  - If endpoint returns `400/404`, it must indicate invalid/non-existent identifier in the response.
- Steps:
  - GET `/exists` for `POD_DOES_NOT_EXIST_PHN_2160`.
  - Apply the assertions above based on status code.
- Result: Passed

-------------------------

All other tests

- Only stats shown (no per-test details): 0 failed, 59 skipped.

How to run

- From `Cursor-Project/EnergoTS/` (cursor branch only):
  - `npx playwright test tests/cursor/PHN-2160-put-update-existing-pod.spec.ts --reporter=line`

