# Correction Data by Scales – Header Period Validation (PDT-2708)

**Jira:** PDT-2708 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** The UI does not restrict creation of Correction data by scales when the header period (Date from / Date to) differs from the original data. The frontend should validate or prevent mismatched header periods for correction records.

**Scope:** Billing Data by Scales — Correction flow in the UI. When a user checks the "Correction" checkbox and fills in the form, the system should ensure that the correction's Date from and Date to match the original billing data by scales record's header period. Currently, the UI allows the user to change these dates freely and submits the form without client-side validation of period equality with the original. The backend also does not reject this, so the bug manifests end-to-end.

---

## Test data (preconditions)

Shared setup for all frontend test cases below.

- **Environment:** Test
1. Log into the Phoenix EPRES portal (`https://apps.energo-pro.bg/phoenix-epres`) with a user who has `BILLING_BY_SCALES_CREATE` and `BILLING_BY_SCALES_EDIT` permissions.
2. Ensure a POD exists (identifier: e.g. `POD_TEST_2708`, type: ELECTRICITY, status: ACTIVE) with at least one active meter installed (meter number: e.g. `MTR2708001`, with a linked active Scales nomenclature — scaleCode: `SC01`, scaleType: `A+`).
   - To create the POD: via backend `POST /pod` or through the POD management UI page.
   - To install the meter: via backend meter endpoint or POD details UI.
3. Create an **original** billing data by scales record for the POD via `POST /billing-by-scales` (or via the "Create Data by Scales" UI page) with:
   - POD identifier: `POD_TEST_2708`
   - Date from: `23.12.2025`
   - Date to: `31.01.2026`
   - Correction checkbox: unchecked (`false`)
   - Invoice number: `INV2708001`
   - Invoice date: current date
   - Billing power in kW: `100`
   - At least one detail row with valid data (period, meter, scale, readings, volumes, values).
   - Save the record. Note the record ID (visible in the URL after redirecting to preview: `?id=<originalId>`).

---

## Frontend Test Cases

### TC-FE-1 (Negative): UI allows entering mismatched Date from for correction — should warn or block

**Description:** Verify the UI behavior when a user creates a correction and manually changes the Date from field to a value different from the original record's Date from. The expected fix would either lock the header dates on corrections or show a validation error.

**Preconditions:**
1. Complete steps 1–3 from Test data above. Original record visible at preview `?id=<originalId>` with Date from `23.12.2025`, Date to `31.01.2026`.
2. User is logged in with `BILLING_BY_SCALES_CREATE` permission.

**Test steps:**
1. Navigate to "Data by Scales" > "Create" page (`/billing-data-by-scales/create`).
2. Enter the POD identifier (`POD_TEST_2708`) and verify the identifier is accepted.
3. Set Date from to `23.01.2026` (different from original's `23.12.2025`).
4. Set Date to to `31.01.2026` (same as original).
5. Check the "Correction" checkbox.
6. Fill in Invoice Correction field with `INV2708001`.
7. Fill in all other required fields (invoice number, invoice date, billing power, at least one detail row).
8. Click the "Create" button.

**Expected test case results:** The system should either: (a) display a validation error near the Date from / Date to fields stating that the correction period must match the original data's period, and NOT submit the form; OR (b) display a warning dialog informing the user that the header period does not match. The record should NOT be saved with a mismatched header period.

**Actual result (bug):** The form submits successfully. The user is redirected to the preview page showing a correction record with Date from `23.01.2026` and Date to `31.01.2026`, which does not match the original's `23.12.2025` to `31.01.2026`.

**References:** PDT-2708; UI component: `CreateBillingDataByScalesComponent`.

---

### TC-FE-2 (Negative): UI allows entering mismatched Date to for correction — should warn or block

**Description:** Verify the UI behavior when a user creates a correction and changes the Date to field to a different value than the original.

**Preconditions:**
1. Complete steps 1–3 from Test data above. Original: Date from `23.12.2025`, Date to `31.01.2026`.
2. User is logged in with `BILLING_BY_SCALES_CREATE` permission.

**Test steps:**
1. Navigate to "Data by Scales" > "Create" page.
2. Enter the POD identifier (`POD_TEST_2708`).
3. Set Date from to `23.12.2025` (same as original).
4. Set Date to to `28.02.2026` (different from original's `31.01.2026`).
5. Check the "Correction" checkbox.
6. Fill in Invoice Correction with `INV2708001`.
7. Fill all other required fields.
8. Click "Create."

**Expected test case results:** Validation error or warning preventing submission with mismatched Date to. The record is NOT saved.

**Actual result (bug):** Form submits successfully with a mismatched Date to.

**References:** PDT-2708.

---

### TC-FE-3 (Positive): Successfully create correction with matching header period via UI

**Description:** Verify that the UI allows creating a correction when the header period (Date from and Date to) exactly matches the original record.

**Preconditions:**
1. Complete steps 1–3 from Test data above. Original: Date from `23.12.2025`, Date to `31.01.2026`.
2. User is logged in with `BILLING_BY_SCALES_CREATE` permission.

**Test steps:**
1. Navigate to "Data by Scales" > "Create" page.
2. Enter the POD identifier (`POD_TEST_2708`).
3. Set Date from to `23.12.2025` (same as original).
4. Set Date to to `31.01.2026` (same as original).
5. Check the "Correction" checkbox.
6. Fill Invoice Correction with `INV2708001`.
7. Fill invoice number (e.g. `INV2708004`), invoice date, billing power.
8. Add at least one detail row with corrected values (valid meter, scale, readings, volumes, total value).
9. Click "Create."

**Expected test case results:** The form submits successfully. The user is redirected to the preview page (`/billing-data-by-scales/preview?id=<newCorrectionId>`). The preview shows: Correction = checked, Date from = `23.12.2025`, Date to = `31.01.2026`, Invoice Correction = `INV2708001`, status = ACTIVE.

**References:** PDT-2708.

---

### TC-FE-4 (Negative): Correction checkbox enables Invoice Correction field — validation on blank

**Description:** Verify that when the user checks "Correction," the Invoice Correction field becomes required and the form is not submitted if it is left blank.

**Preconditions:**
1. User logged in with `BILLING_BY_SCALES_CREATE` permission.
2. POD and original data exist as per Test data steps 1–3.

**Test steps:**
1. Navigate to "Data by Scales" > "Create" page.
2. Enter the POD identifier, set matching Date from / Date to, fill all required fields except Invoice Correction.
3. Check the "Correction" checkbox.
4. Leave the Invoice Correction field empty.
5. Click "Create."

**Expected test case results:** The form shows a validation error on the Invoice Correction field (e.g. "Invoice Correction is required when Correction is checked"). The form is NOT submitted.

**References:** PDT-2708; `toggleCorrection()` in `CreateBillingDataByScalesComponent`.

---

### TC-FE-5 (Positive): Correction checkbox enables Override toggle

**Description:** Verify that checking the "Correction" checkbox enables the "Override" toggle, and that unchecking "Correction" disables "Override" and resets it to false.

**Preconditions:**
1. User logged in, on the "Data by Scales" > "Create" page.

**Test steps:**
1. Observe that the "Override" toggle is disabled by default.
2. Check the "Correction" checkbox.
3. Observe that the "Override" toggle becomes enabled.
4. Check the "Override" toggle.
5. Uncheck the "Correction" checkbox.
6. Observe the "Override" toggle.

**Expected test case results:**
- Step 1: Override is disabled (greyed out).
- Step 3: Override becomes enabled (clickable).
- Step 5–6: Override is automatically unchecked and disabled again.

**References:** PDT-2708; `handleCorrectionChanges()`, `handleOverrideChanges()`.

---

### TC-FE-6 (Positive): Preview page displays correction record with correct header period

**Description:** Verify that the preview page for a correction record displays the correct header period (Date from, Date to) and the Correction flag.

**Preconditions:**
1. A valid correction record exists (created with matching header period via TC-FE-3 or TC-BE-4). Note its `id`.
2. User logged in with `BILLING_BY_SCALES_VIEW_BASIC` permission.

**Test steps:**
1. Navigate to `billing-data-by-scales/preview?id=<correctionId>`.
2. Inspect the displayed Date from, Date to, Correction checkbox, Invoice Correction, and detail table.

**Expected test case results:** Date from = `23.12.2025`, Date to = `31.01.2026`, Correction = checked, Invoice Correction = `INV2708001`. All form fields are read-only in preview mode. Detail rows display corrected meter readings and values.

**References:** PDT-2708; `checkForPreviewMode()` in component.

---

### TC-FE-7 (Negative): Error message is displayed when backend rejects mismatched correction

**Description:** Verify that if the backend is fixed and rejects a correction with a mismatched header period, the UI properly displays the error to the user.

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. Backend fix for PDT-2708 is deployed (header period validation active).
3. User logged in with `BILLING_BY_SCALES_CREATE` permission.

**Test steps:**
1. Navigate to "Data by Scales" > "Create."
2. Enter POD identifier, set a different Date from than the original (e.g. `23.01.2026` instead of `23.12.2025`), keep Date to matching.
3. Check "Correction," fill Invoice Correction, fill all required fields.
4. Click "Create."

**Expected test case results:** The backend returns an error. The UI displays the error message from the backend (e.g. "Correction header period must match original data's period") either as a field-level error near Date from / Date to or as a dialog / system message component. The form remains editable so the user can correct the dates.

**References:** PDT-2708; `handleBillingByScalesError()`.

---

### TC-FE-8 (Positive): Number of days recalculates when Date from or Date to changes

**Description:** Verify that the "Number of days" field recalculates correctly when the user changes Date from or Date to.

**Preconditions:**
1. User logged in, on the "Data by Scales" > "Create" page.

**Test steps:**
1. Set Date from to `23.12.2025`.
2. Set Date to to `31.01.2026`.
3. Observe the "Number of days" field.
4. Change Date from to `01.01.2026`.
5. Observe the "Number of days" field update.

**Expected test case results:**
- Step 3: Number of days = 40 (from Dec 23 to Jan 31 inclusive).
- Step 5: Number of days = 31 (from Jan 1 to Jan 31 inclusive).
- The field is read-only and auto-calculated.

**References:** PDT-2708; `calculateNumberOfDays()`.

---

### TC-FE-9 (Negative): Correction fields allow negative values when correction is checked

**Description:** Verify that certain detail row fields (difference, deducted, totalValue, volumes, totalVolumes) accept negative values when the "Correction" checkbox is checked, but reject negative values when it is unchecked.

**Preconditions:**
1. User logged in, on the "Data by Scales" > "Create" page.

**Test steps:**
1. Leave "Correction" unchecked.
2. Enter `-100` in the "Difference" field of a detail row.
3. Observe that the field shows a validation error (negative values not allowed).
4. Check the "Correction" checkbox.
5. Enter `-100` in the "Difference" field of a detail row.
6. Observe that the validation error is gone — negative values are now accepted.

**Expected test case results:**
- Step 3: Validation error displayed (pattern rejects negatives).
- Step 6: No validation error — the field accepts `-100` because correction mode enables negative values.

**References:** PDT-2708; `toggleCorrection()` changes validators for `fieldsAllowingNegatives`.

---

### TC-FE-10 (Negative): Date from beyond one-year range from Date to shows validation error

**Description:** Verify that the UI validates the one-year maximum range between Date from and Date to.

**Preconditions:**
1. User logged in, on the "Data by Scales" > "Create" page.

**Test steps:**
1. Set Date from to `01.01.2025`.
2. Set Date to to `01.06.2026` (more than one year).
3. Observe the Date from / Date to fields.
4. Attempt to submit the form.

**Expected test case results:** A validation error appears on the Date from or Date to field indicating the period must not exceed one year. The form is NOT submitted.

**References:** PDT-2708; `ValidateMinYear`, `ValidateMaxYear` validators.

---

## References

- **Jira:** PDT-2708 – The system doesn't restrict creation of the Correction data by scales with a difference in the header period with the original data.
- **UI component:** `CreateBillingDataByScalesComponent` (`billing-data-by-scales/create-billing-data-by-scales`).
- **UI service:** `BillingDataByScalesService` — `createItem()` calls `POST /billing-by-scales`, `editItem()` calls `PUT /billing-by-scales`.
- **Routes:** `/billing-data-by-scales/create`, `/billing-data-by-scales/edit?id=X`, `/billing-data-by-scales/preview?id=X`.
- **Form validations:** `startEndDateValidator`, `endStartDateValidator`, `ValidateMinYear`, `ValidateMaxYear`, required fields, pattern validators.
- **Correction toggle logic:** `toggleCorrection()`, `handleCorrectionChanges()`, `handleOverrideChanges()`.
