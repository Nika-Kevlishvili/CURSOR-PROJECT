# Service Contract — First Version Start Date Re-alignment (PDT-2846)

**Jira:** PDT-2846 (Phoenix Delivery)  
**Type:** Task (Customer Feedback)  
**Summary:** When a Service Contract version 1 is saved as Draft, `startDate = creationDate` and is non-editable. During finalization (status transition to SIGNED / ENTERED_INTO_FORCE / etc.), if the user provides a `signingDate` earlier than version 1's `startDate`, the system must auto-realign version 1 `startDate` to the `signingDate`, then run standard validations. Non-first versions remain unchanged.

**Scope:** Service Contract update flow (`PUT /service-contract/{id}`) — version 1 start-date realignment logic in `ServiceContractBasicParametersService.edit()`. Covers the new auto-realignment, guard checks, downstream version impact, date validation in `ServiceContractDateService`, and `isStartDateValid` ordering rules.

**Linked:** PDT-2599 (relates to — Service Contract versioning / version status)

---

## Test data (preconditions)

Shared setup for all test cases below (environment + entity creation chain).

- **Environment:** Dev

1. Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE, customerIdentifier: auto-generated).
2. Create a service (EPService) that the service contract will reference. The service must have status: ACTIVE, with at least one active service version (serviceVersionId will be used in the contract request).
3. Create a service contract via `POST /service-contract` with:
   - `basicParameters.customerId`: customer ID from step 1
   - `basicParameters.customerVersionId`: customer version from step 1
   - `basicParameters.serviceId`: service ID from step 2
   - `basicParameters.serviceVersionId`: active service version from step 2
   - `basicParameters.contractStatus`: `DRAFT`
   - `basicParameters.detailsSubStatus`: `DRAFT`
   - `basicParameters.signInDate`: `null` (Draft requires no signing date)
   - `basicParameters.startDate`: system auto-sets to `today` (creation date)
   - `basicParameters.contractVersionStatus`: `DRAFT`
   - All required service parameters and additional parameters per the service's third-tab-fields.
4. Verify the created service contract: `GET /service-contract/{id}?versionId=1`
   - Confirm `versionId = 1`
   - Confirm `startDate = today` (the creation date)
   - Confirm `contractVersionStatus = DRAFT`
   - Confirm `signingDate = null`

---

## Backend Test Cases

### TC-BE-1 (Positive): Version 1 — signingDate in the past auto-realigns startDate

**Description:** Verify that when version 1 is updated with a `signingDate` earlier than its current `startDate`, the system automatically re-aligns `startDate` to the provided `signingDate`, and the update succeeds.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: none (shared setup unchanged).

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.contractStatus`: `ENTERED_INTO_FORCE` (or `SIGNED` with subStatus `SIGNED_BY_BOTH_SIDES`)
   - `basicParameters.signInDate`: a date **before** the contract's current `startDate` (e.g. if startDate = 2026-05-19, use signInDate = 2026-05-10)
   - `basicParameters.startDate`: keep original value (same as current startDate)
   - `basicParameters.contractVersionStatus`: `SIGNED`
   - `basicParameters.entryIntoForceDate`: today or in the past
   - All other required fields populated correctly.
2. Verify the response status.
3. Call `GET /service-contract/{id}?versionId=1` to read the updated contract.

**Expected test case results:** The update succeeds (HTTP 200). The returned `startDate` equals the provided `signingDate` (e.g. 2026-05-10), NOT the original creation-date. The `signingDate` is stored correctly. No error messages about "Start date must not be changed for this version."

---

### TC-BE-2 (Positive): Version 1 — signingDate equals today (same as startDate) — no realignment needed

**Description:** Verify that when version 1 is updated with `signingDate = today` (which equals the original `startDate`), no realignment occurs and `startDate` stays unchanged.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: none (shared setup unchanged; startDate was set to today at creation).

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.contractStatus`: `SIGNED` (subStatus: `SIGNED_BY_BOTH_SIDES`)
   - `basicParameters.signInDate`: today
   - `basicParameters.startDate`: same as current startDate (today)
   - `basicParameters.contractVersionStatus`: `SIGNED`
2. Verify the response status.
3. Call `GET /service-contract/{id}?versionId=1`.

**Expected test case results:** The update succeeds (HTTP 200). `startDate` remains today (unchanged). `signingDate` = today. No error messages.

---

### TC-BE-3 (Positive): Version 1 — signingDate equals startDate exactly — no realignment

**Description:** Verify that when `signingDate` is exactly equal to `startDate`, the re-alignment logic does NOT trigger (since `signingDate` is not strictly before `startDate`).

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: none (startDate = today at creation).

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.signInDate`: same date as current `startDate`
   - `basicParameters.startDate`: same as current `startDate`
   - `basicParameters.contractStatus`: `ENTERED_INTO_FORCE`
   - `basicParameters.contractVersionStatus`: `SIGNED`
   - `basicParameters.entryIntoForceDate`: today or past
2. Read the contract via `GET /service-contract/{id}?versionId=1`.

**Expected test case results:** Update succeeds (HTTP 200). `startDate` remains unchanged (equals signingDate). No error about startDate change.

---

### TC-BE-4 (Positive): Version 1 — signingDate yesterday, startDate today — realignment to yesterday

**Description:** Verify a concrete 1-day-earlier signing date causes proper realignment.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: contract was created today, so startDate = today.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.signInDate`: yesterday
   - `basicParameters.startDate`: today (unchanged from creation)
   - `basicParameters.contractStatus`: `ENTERED_INTO_FORCE`
   - `basicParameters.contractVersionStatus`: `SIGNED`
   - `basicParameters.entryIntoForceDate`: yesterday or today
2. Read contract via `GET /service-contract/{id}?versionId=1`.

**Expected test case results:** Update succeeds (HTTP 200). `startDate` = yesterday (realigned to signingDate). `signingDate` = yesterday.

---

### TC-BE-5 (Positive): Version 1 — signingDate far in the past — realignment works

**Description:** Verify realignment with a `signingDate` significantly earlier than `startDate` (e.g. 30 days before creation date).

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: none (startDate = today).

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.signInDate`: 30 days ago
   - `basicParameters.startDate`: today (unchanged)
   - `basicParameters.contractStatus`: `ENTERED_INTO_FORCE`
   - `basicParameters.contractVersionStatus`: `SIGNED`
   - `basicParameters.entryIntoForceDate`: 30 days ago or today
2. Read contract via GET.

**Expected test case results:** Update succeeds (HTTP 200). `startDate` = 30 days ago (realigned). `signingDate` = 30 days ago.

---

### TC-BE-6 (Negative): Version 1 — null signingDate, attempt to change startDate — rejected

**Description:** Verify that for version 1, when `signingDate` is null and the request sends a different `startDate`, the system rejects with "Start date must not be changed for this version."

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: `basicParameters.signInDate` = null (kept as Draft default); `basicParameters.startDate` = yesterday (deliberately different from existing startDate = today). This tests the guard at line 1313: null signingDate + changed startDate → error.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.signInDate`: null
   - `basicParameters.startDate`: yesterday (different from current startDate = today)
   - `basicParameters.contractStatus`: `DRAFT`
   - `basicParameters.contractVersionStatus`: `DRAFT`
2. Inspect the response.

**Expected test case results:** The request fails (HTTP 400 or validation error). Error message contains "Start date must not be changed for this version". The contract's `startDate` remains today (unchanged).

---

### TC-BE-7 (Negative): Version 1 — signingDate = today, attempt to change startDate — rejected

**Description:** Verify that for version 1, when `signingDate = today` (equal to creation date) but `startDate` is sent as a different date, the system rejects because `signingDate.equals(LocalDate.now())` and the startDate differs from the existing one.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: `basicParameters.signInDate` = today; `basicParameters.startDate` = yesterday (deliberately different from existing startDate = today). This triggers the guard at line 1313: `signInDate.equals(LocalDate.now())` + `startDate != existing` → error. The auto-realignment does NOT fire because today is not before today.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.signInDate`: today
   - `basicParameters.startDate`: yesterday (deliberately different from current startDate = today)
   - `basicParameters.contractStatus`: `SIGNED`
   - `basicParameters.contractVersionStatus`: `SIGNED`
2. Inspect the response.

**Expected test case results:** The request fails (HTTP 400 or validation error). Error message includes "Start date must not be changed for this version". `startDate` stays today. The auto-realignment does NOT trigger because `signingDate` (today) is not before `startDate` (today).

---

### TC-BE-8 (Negative): Version 2+ — signingDate before version 1 startDate — no auto-realign, rejected

**Description:** Verify that the auto-realignment logic is version-1-only. For version 2, even if `signingDate < version 1 startDate`, no auto-realignment of version 2 startDate occurs, and the standard "Start date must be after the start date of the first version" validation applies.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: First finalize version 1 with a past signingDate (so version 1's startDate gets realigned). Then create version 2 of the contract (savingAsNewVersion = true) with a `startDate` after version 1's startDate and `contractVersionStatus = DRAFT`.

**Test steps:**
1. Attempt `PUT /service-contract/{id}?versionId=2` with:
   - `basicParameters.signInDate`: a date before version 1's (realigned) startDate
   - `basicParameters.startDate`: a date before version 1's startDate
   - `basicParameters.contractVersionStatus`: `SIGNED`
2. Inspect the response.

**Expected test case results:** The request fails (HTTP 400 or validation error). Error message includes "Start date must be after the start date of the first version" or "Start date must not be earlier than the previous version's start date". Version 2's `startDate` is NOT auto-realigned.

---

### TC-BE-9 (Negative): Version 1 — signingDate in the future — date validation rejects

**Description:** Verify that providing a `signingDate` in the future is rejected by the date validation layer (`ServiceContractDateService`), regardless of the startDate realignment logic.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: none.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.signInDate`: tomorrow (future date)
   - `basicParameters.startDate`: today
   - `basicParameters.contractStatus`: `SIGNED` (subStatus: `SIGNED_BY_BOTH_SIDES`)
   - `basicParameters.contractVersionStatus`: `SIGNED`
2. Inspect the response.

**Expected test case results:** The request fails (HTTP 400 or validation error). Error message includes `"Signing date should be today or in future"` — this is the exact text returned by the API (note: the message text is semantically misleading; the validation rejects future dates, meaning signingDate must be today or in the past, but the error string says "or in future"). The auto-realignment does NOT trigger because `signingDate` (future) is NOT before `startDate` (today). No changes are persisted.

---

### TC-BE-10 (Positive): Version 1 — realigned startDate does not conflict with other versions

**Description:** Verify that after version 1 startDate is auto-realigned, the new value passes uniqueness validation (no other version has the same startDate).

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: The contract has only version 1 (no version 2 created yet). The signingDate used for realignment does not coincide with any other version's startDate.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.signInDate`: 5 days ago (unique, no other version has this startDate)
   - `basicParameters.startDate`: today (unchanged)
   - `basicParameters.contractStatus`: `ENTERED_INTO_FORCE`
   - `basicParameters.contractVersionStatus`: `SIGNED`
   - `basicParameters.entryIntoForceDate`: today or past
2. Verify success.
3. Read contract via GET.

**Expected test case results:** Update succeeds (HTTP 200). `startDate` = 5 days ago. No uniqueness conflict.

---

### TC-BE-11 (Negative): Version 1 — realigned startDate conflicts with existing version 2 startDate

**Description:** Verify that if auto-realignment would set version 1's `startDate` to a date already used by version 2, the `checkAndGetStartDate` validation catches the conflict and rejects.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: First update version 1 to `SIGNED` status with `signingDate = today` (no realignment needed). Then create version 2 (savingAsNewVersion = true) with `startDate` = 5 days ago (if version 2's startDate was set during creation to a specific past date). Alternatively: create version 2 with a `startDate` that equals the intended signingDate for the version 1 re-edit.

**Test steps:**
1. Attempt to re-update version 1 via `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.signInDate`: equal to version 2's `startDate` (so auto-realignment would create a duplicate)
   - `basicParameters.startDate`: today (unchanged)
2. Inspect the response.

**Expected test case results:** The request fails (HTTP 400 or validation error). Error message includes "Contract version already has provided start date" or "Contract version with this date: ... already exists". The conflicting duplicate is rejected.

---

### TC-BE-12 (Positive): Version 1 realignment followed by version 2 creation — version 2 startDate after new version 1 startDate

**Description:** Verify that after version 1's startDate is realigned to a past date, a new version 2 can be created with a startDate after the new (realigned) version 1 startDate.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: Update version 1 with signingDate = 10 days ago (startDate realigns to 10 days ago). Version status = SIGNED.

**Test steps:**
1. Create version 2 via `PUT /service-contract/{id}?versionId=1` with `savingAsNewVersion = true`:
   - `basicParameters.startDate`: 5 days ago (after version 1's new startDate of 10 days ago)
   - `basicParameters.contractVersionStatus`: `DRAFT`
2. Read version 2 via `GET /service-contract/{id}?versionId=2`.

**Expected test case results:** Version 2 created successfully (HTTP 200). Version 2's `startDate` = 5 days ago, which is after version 1's realigned `startDate` (10 days ago). The ordering validation passes.

---

### TC-BE-13 (Negative): Version 1 DRAFT status — signingDate must be null

**Description:** Verify that when version 1 is in DRAFT status, providing a `signingDate` (even if it is before `startDate`) is rejected by `ServiceContractDateService.validateDates()`, which requires signingDate to be null for DRAFT.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: `basicParameters.signInDate` = yesterday (non-null, invalid for DRAFT); `basicParameters.contractStatus` = DRAFT. The date validation layer rejects any non-null signingDate for DRAFT before the realignment logic is reached.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.contractStatus`: `DRAFT`
   - `basicParameters.signInDate`: yesterday (non-null for DRAFT)
   - `basicParameters.startDate`: today
   - `basicParameters.contractVersionStatus`: `DRAFT`
2. Inspect the response.

**Expected test case results:** The request fails (HTTP 400 or validation error). Error message includes "Signing date should be empty". DRAFT status requires null signingDate. No startDate realignment occurs.

---

### TC-BE-14 (Positive): Version 1 — transition from DRAFT to SIGNED with past signingDate — realignment on the same update

**Description:** Verify the most common real-world scenario: a single update transitions version 1 from DRAFT to SIGNED with a past signing date, and the startDate auto-realigns in the same request.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: Version 1 must first be moved from DRAFT → READY (since DRAFT → SIGNED is not a valid version status transition). So pre-update version 1 to `contractVersionStatus = READY` via a PUT with contractStatus still = READY. Then use the READY state as the starting point.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.contractStatus`: `SIGNED` (subStatus: `SIGNED_BY_BOTH_SIDES`)
   - `basicParameters.contractVersionStatus`: `SIGNED`
   - `basicParameters.signInDate`: 7 days ago
   - `basicParameters.startDate`: today (same as creation-date startDate)
   - `basicParameters.entryIntoForceDate`: in the future (since SIGNED expects entry-in-force in the future, per code)
2. Read the contract via GET.

**Expected test case results:** Update succeeds (HTTP 200). `startDate` is auto-realigned to 7 days ago (the signingDate). `contractVersionStatus` = SIGNED. `signingDate` = 7 days ago.

---

### TC-BE-15 (Positive): Version 1 — transition to ENTERED_INTO_FORCE with past signingDate

**Description:** Verify that transitioning directly to ENTERED_INTO_FORCE (which requires both signingDate and entryIntoForceDate) with a past signingDate triggers the auto-realignment of startDate.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: Update version 1 to `contractVersionStatus = READY`, then to `contractVersionStatus = SIGNED`.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.contractStatus`: `ENTERED_INTO_FORCE`
   - `basicParameters.signInDate`: 14 days ago
   - `basicParameters.entryIntoForceDate`: today or past (required for ENTERED_INTO_FORCE)
   - `basicParameters.startDate`: today (original creation date)
   - `basicParameters.contractVersionStatus`: `SIGNED`
2. Read contract via GET.

**Expected test case results:** Update succeeds (HTTP 200). `startDate` = 14 days ago (realigned to signingDate). `entryIntoForceDate` = the provided date. All date validations pass.

---

### TC-BE-16 (Negative): Version 1 — READY status, signingDate must be null — no realignment possible

**Description:** Verify that when version 1 is in READY status, providing a `signingDate` is rejected (DateService: "Signing date should be empty" for READY), so the auto-realignment path is never reached.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: Update version 1 to `contractVersionStatus = READY`, `contractStatus = READY`.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.contractStatus`: `READY`
   - `basicParameters.signInDate`: 5 days ago (non-null for READY)
   - `basicParameters.startDate`: today
   - `basicParameters.contractVersionStatus`: `READY`
2. Inspect the response.

**Expected test case results:** The request fails (HTTP 400 or validation error). Error message includes "Signing date should be empty". READY status requires null signingDate. No startDate realignment occurs.

---

### TC-BE-17 (Positive): Non-first version update — signingDate in the past does NOT trigger realignment

**Description:** Verify that the auto-realignment logic is strictly for version 1. When updating version 2+ with a past signingDate, no automatic startDate change occurs.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: Finalize version 1 successfully (SIGNED with signingDate = today). Create version 2 via `savingAsNewVersion = true` with `startDate` = tomorrow (future date, after version 1's startDate). Version 2 `contractVersionStatus = DRAFT`.

**Test steps:**
1. Update version 2 to SIGNED: `PUT /service-contract/{id}?versionId=2` with:
   - `basicParameters.signInDate`: 3 days ago (before version 2's startDate)
   - `basicParameters.startDate`: tomorrow (keep original, unchanged)
   - `basicParameters.contractVersionStatus`: `SIGNED`
2. Read the contract version 2 via GET.

**Expected test case results:** Update succeeds (HTTP 200). Version 2's `startDate` remains tomorrow (NO auto-realignment). `signingDate` = 3 days ago. The auto-realignment logic (code check `versionId == 1`) does not apply to version 2.

---

### TC-BE-18 (Negative): Version 1 — signingDate before startDate but entryIntoForceDate in the future for ENTERED_INTO_FORCE — rejected

**Description:** Verify that even though startDate auto-realigns, other date validations still apply. For `ENTERED_INTO_FORCE`, `entryIntoForceDate` must be today or in the past. Providing a future entryIntoForceDate should still be rejected.

**Preconditions:**
1. Apply Test data steps 1–4.
2. Delta: Version 1 in READY → SIGNED state.

**Test steps:**
1. Call `PUT /service-contract/{id}?versionId=1` with:
   - `basicParameters.contractStatus`: `ENTERED_INTO_FORCE`
   - `basicParameters.signInDate`: 10 days ago
   - `basicParameters.entryIntoForceDate`: tomorrow (future — invalid for ENTERED_INTO_FORCE)
   - `basicParameters.startDate`: today
   - `basicParameters.contractVersionStatus`: `SIGNED`
2. Inspect the response.

**Expected test case results:** The request fails (HTTP 400 or validation error). Error message includes "Entry in force should be today or past". The startDate auto-realignment may have been applied to the request object, but the overall update is still rejected due to the entryIntoForceDate validation. No changes are persisted.

---

### TC-BE-19 (Positive): Version 1 creation — signingDate provided at creation, startDate uses signingDate if before today

**Description:** Verify that at contract creation time (`POST /service-contract`), if a `signingDate` is provided and is before today, the initial `startDate` is set to `signingDate` (not today). This is the create-time behavior (separate from the update-time realignment).

**Preconditions:**
1. Apply Test data steps 1–2 only (customer + service).
2. Delta: Do NOT create the contract yet; create it in the test step with a past signingDate.

**Test steps:**
1. Call `POST /service-contract` with:
   - All required parameters from steps 1-2
   - `basicParameters.contractStatus`: `ENTERED_INTO_FORCE`
   - `basicParameters.signInDate`: 7 days ago
   - `basicParameters.entryIntoForceDate`: today or past
   - `basicParameters.contractVersionStatus`: `SIGNED`
2. Read the created contract via `GET /service-contract/{id}?versionId=1`.

**Expected test case results:** Contract created (HTTP 201). `startDate` = 7 days ago (signingDate, since it is before today). `signingDate` = 7 days ago. This confirms the create-path behavior: `if (signingDate != null && signingDate.isBefore(LocalDate.now())) { startDate = signingDate } else { startDate = today }`.

---

### TC-BE-20 (Positive): Version 1 creation — no signingDate (DRAFT) — startDate = today

**Description:** Verify that creating a service contract in DRAFT status (no signingDate) sets `startDate = today`.

**Preconditions:**
1. Apply Test data steps 1–2 only.
2. Delta: Create the contract in DRAFT status (the typical creation path).

**Test steps:**
1. Call `POST /service-contract` with:
   - `basicParameters.contractStatus`: `DRAFT`
   - `basicParameters.signInDate`: null
   - `basicParameters.contractVersionStatus`: `DRAFT`
2. Read the created contract via GET.

**Expected test case results:** Contract created (HTTP 201). `startDate` = today. `signingDate` = null. Confirms the default creation behavior.

---

## References

- **Jira:** PDT-2846 – Additional First Version Start Date logic.
- **Related:** PDT-2599 – Service Contract versioning / version status.
- **Code:**
  - `ServiceContractBasicParametersService.edit()` — lines 1313–1321 (guard check + auto-realignment)
  - `ServiceContractBasicParametersService.isStartDateValid()` — ordering validation
  - `ServiceContractBasicParametersService.checkAndGetStartDate()` — uniqueness + ordering
  - `ServiceContractDateService.validateDates()` — status-based date validations
  - `ServiceContractService.create()` — lines 146–150 (create-time startDate logic)
  - `ServiceContractController` — `PUT /service-contract/{id}`, `POST /service-contract`
- **Diagrams:** No applicable diagram found in `Cursor-Project/config/Diagrams/`.
