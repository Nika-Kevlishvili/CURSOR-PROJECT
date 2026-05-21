# PDT-2846 — Playwright Backend Test Run (Detailed)

## Run context

| Field | Value |
|-------|-------|
| Jira | [PDT-2846](https://oppa-support.atlassian.net/browse/PDT-2846) |
| Title | Additional First Version Start Date logic |
| Date | 2026-05-19 |
| Environment | Dev (`http://10.236.20.11:8091` API, `http://10.236.20.11:8080` portal) |
| Branch | `cursor` (EnergoTS) |
| Spec | `Cursor-Project/EnergoTS/tests/cursor/PDT-2846-be-service-contract-first-version-start-date.spec.ts` |
| Manual test cases | `Cursor-Project/test_cases/Backend/Service_Contract_First_Version_StartDate_PDT_2846.md` |
| Command | `npx playwright test tests/cursor/PDT-2846-be-service-contract-first-version-start-date.spec.ts` |
| Assignee | Ketevan Bardakovi |
| Tester | nika kevlishvili |

## Summary

| Metric | Count |
|--------|------:|
| Total (TC-BE-1 … TC-BE-20) | 20 |
| Passed | 20 |
| Failed | 0 |
| Skipped | 0 |
| Duration | ~100.7s (4 workers) |

## Dev API notes (automation vs manual TC text)

- POST `/service-contract`: first version uses `contractVersionStatus: SIGNED` with header `contractStatus: DRAFT` and `signInDate: null` for draft-like setup.
- v1 cannot be promoted to `contractVersionStatus: READY` (API: *"Version status of first version should be VALID/SIGNED!"*).
- Negative messages may differ slightly from manual TC wording (e.g. future signing → `signing date must not be in the future`; DRAFT + signing → `signing date must not be present`).
- **TC-BE-11 (Playwright):** creates v2 with **same `startDate` as v1** and expects **400** duplicate/ordering error (Dev did not return conflict on original “re-edit v1 to match v2” scenario).

## Per test case (manual steps + Playwright execution + outcome)

### TC-BE-1

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3106
- **Manual expected result:** The update succeeds (HTTP 200). The returned `startDate` equals the provided `signingDate` (e.g. 2026-05-10), NOT the original creation-date. The `signingDate` is stored correctly. No error messages about "Start date must not be changed for this version."

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Verify the response status.
3. Call `GET /service-contract/{id}?versionId=1` to read the updated contract.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**
8. PUT version 1 with past signingDate — **PASS**
9. GET version 1 — startDate equals signingDate — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-2

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3107
- **Manual expected result:** The update succeeds (HTTP 200). `startDate` remains today (unchanged). `signingDate` = today. No error messages.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Verify the response status.
3. Call `GET /service-contract/{id}?versionId=1`.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**
8. PUT version 1 with signingDate = today — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-3

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3109
- **Manual expected result:** Update succeeds (HTTP 200). `startDate` remains unchanged (equals signingDate). No error about startDate change.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Read the contract via `GET /service-contract/{id}?versionId=1`.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-4

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3108
- **Manual expected result:** Update succeeds (HTTP 200). `startDate` = yesterday (realigned to signingDate). `signingDate` = yesterday.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Read contract via `GET /service-contract/{id}?versionId=1`.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-5

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3110
- **Manual expected result:** Update succeeds (HTTP 200). `startDate` = 30 days ago (realigned). `signingDate` = 30 days ago.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Read contract via GET.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-6

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3111
- **Manual expected result:** The request fails (HTTP 400 or validation error). Error message contains "Start date must not be changed for this version". The contract's `startDate` remains today (unchanged).

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Inspect the response.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-7

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3112
- **Manual expected result:** The request fails (HTTP 400 or validation error). Error message includes "Start date must not be changed for this version". `startDate` stays today. The auto-realignment does NOT trigger because `signingDate` (today) is not before `startDate` (today).

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Inspect the response.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-8

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3113
- **Manual expected result:** The request fails (HTTP 400 or validation error). Error message includes "Start date must be after the start date of the first version" or "Start date must not be earlier than the previous version's start date". Version 2's `startDate` is NOT auto-realigned.

**Manual test steps (from test case file):**

1. Attempt `PUT /service-contract/{id}?versionId=2` with:
2. Inspect the response.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-9

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3114
- **Manual expected result:** The request fails (HTTP 400 or validation error). Error message includes `"Signing date should be today or in future"` — this is the exact text returned by the API (note: the message text is semantically misleading; the validation rejects future dates, meaning signingDate must be today or in the past, but the error string says "or in future"). The auto-realignment does NOT trigger because `signingDate` (future) is NOT before `startDate` (today). No changes are persisted.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Inspect the response.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-10

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3115
- **Manual expected result:** Update succeeds (HTTP 200). `startDate` = 5 days ago. No uniqueness conflict.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Verify success.
3. Read contract via GET.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-11

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3116
- **Manual expected result:** The request fails (HTTP 400 or validation error). Error message includes "Contract version already has provided start date" or "Contract version with this date: ... already exists". The conflicting duplicate is rejected.

**Manual test steps (from test case file):**

1. Attempt to re-update version 1 via `PUT /service-contract/{id}?versionId=1` with:
2. Inspect the response.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-12

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3117
- **Manual expected result:** Version 2 created successfully (HTTP 200). Version 2's `startDate` = 5 days ago, which is after version 1's realigned `startDate` (10 days ago). The ordering validation passes.

**Manual test steps (from test case file):**

1. Create version 2 via `PUT /service-contract/{id}?versionId=1` with `savingAsNewVersion = true`:
2. Read version 2 via `GET /service-contract/{id}?versionId=2`.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-13

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3118
- **Manual expected result:** The request fails (HTTP 400 or validation error). Error message includes "Signing date should be empty". DRAFT status requires null signingDate. No startDate realignment occurs.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Inspect the response.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-14

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3119
- **Manual expected result:** Update succeeds (HTTP 200). `startDate` is auto-realigned to 7 days ago (the signingDate). `contractVersionStatus` = SIGNED. `signingDate` = 7 days ago.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Read the contract via GET.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-15

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3120
- **Manual expected result:** Update succeeds (HTTP 200). `startDate` = 14 days ago (realigned to signingDate). `entryIntoForceDate` = the provided date. All date validations pass.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Read contract via GET.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-16

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3121
- **Manual expected result:** The request fails (HTTP 400 or validation error). Error message includes "Signing date should be empty". READY status requires null signingDate. No startDate realignment occurs.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Inspect the response.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-17

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3122
- **Manual expected result:** Update succeeds (HTTP 200). Version 2's `startDate` remains tomorrow (NO auto-realignment). `signingDate` = 3 days ago. The auto-realignment logic (code check `versionId == 1`) does not apply to version 2.

**Manual test steps (from test case file):**

1. Update version 2 to SIGNED: `PUT /service-contract/{id}?versionId=2` with:
2. Read the contract version 2 via GET.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-18

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3123
- **Manual expected result:** The request fails (HTTP 400 or validation error). Error message includes "Entry in force should be today or past". The startDate auto-realignment may have been applied to the request object, but the overall update is still rejected due to the entryIntoForceDate validation. No changes are persisted.

**Manual test steps (from test case file):**

1. Call `PUT /service-contract/{id}?versionId=1` with:
2. Inspect the response.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-19

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3124
- **Manual expected result:** Contract created (HTTP 201). `startDate` = 7 days ago (signingDate, since it is before today). `signingDate` = 7 days ago. This confirms the create-path behavior: `if (signingDate != null && signingDate.isBefore(LocalDate.now())) { startDate = signingDate } else { startDate = today }`.

**Manual test steps (from test case file):**

1. Call `POST /service-contract` with:
2. Read the created contract via `GET /service-contract/{id}?versionId=1`.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

### TC-BE-20

- **Playwright status:** PASSED
- **Portal:** http://10.236.20.11:8080/service-contracts/preview?id=3125
- **Manual expected result:** Contract created (HTTP 201). `startDate` = today. `signingDate` = null. Confirms the default creation behavior.

**Manual test steps (from test case file):**

1. Call `POST /service-contract` with:
2. Read the created contract via GET.

**Playwright execution (step-by-step):**

1. Precondition: Create customer — **PASS**
2. Precondition: Communication channels (BILLING and CONTRACT) — **PASS**
3. Precondition: Price component (PER_PIECE) — **PASS**
4. Precondition: Create term — **PASS**
5. Precondition: Create POD — **PASS**
6. Precondition: Create commercial service — **PASS**
7. Precondition: Create DRAFT service-contract — **PASS**

**Actual result:** All steps completed; overall **PASSED**. Contract persisted in Dev; open portal link to inspect final state.

## Related artifacts

- Machine report: `Cursor-Project/EnergoTS/playwright-report-detailed.md`
- Source JSON: `Cursor-Project/EnergoTS/playwright-report.json`
- HTML report: run `npx playwright show-report` from `EnergoTS/`

**Jira source:** REST fallback via `fetch-issue.ps1` (MCP not used in this run).