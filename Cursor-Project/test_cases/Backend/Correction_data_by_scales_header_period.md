# Correction Data by Scales – Header Period Validation (PDT-2708)

**Jira:** PDT-2708 (Phoenix Delivery)  
**Type:** Bug  
**Summary:** The system does not restrict creation of Correction data by scales when the header period (dateFrom/dateTo) differs from the original data. The backend must validate that the correction's header period matches the original data's header period.

**Scope:** Billing Data by Scales — Correction flow. When a user creates a correction record (`correction=true`) via `POST /billing-by-scales`, the system should verify that the correction's `dateFrom` and `dateTo` (the "header period") are equal to the corresponding original billing data by scales record's header period. Currently, the system skips the overlap check for corrections (line 80 of `BillingByScalesService.create()`) but does NOT enforce period equality with the original. This allows correction records with mismatched header periods to be saved.

---

## Test data (preconditions)

Shared setup for all test cases below. Each test case refines or extends these steps as needed.

- **Environment:** Test
1. Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE, customerIdentifier: auto-generated).
2. Create a POD (Point of Delivery) via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activation date: 2025-01-01, identifier: e.g. `POD_TEST_2708`).
3. Create a product via `POST /product` (term: INDEFINITE, data delivery type: BY_SCALE).
4. Create terms for the product via `POST /terms` (linked to product from step 3).
5. Create a price component via `POST /price-component` (type: ENERGY, rate: 0.15 BGN/kWh, currency: BGN, linked to product from step 3).
6. Create a product contract via `POST /product-contract` (linking customer from step 1, POD from step 2, product from step 3; status: ACTIVE, entry-into-force date: 2025-01-01, no termination date).
7. Install a meter on the POD via the meter endpoint (meter number: e.g. `MTR2708001`, grid operator matching the POD, installment date: 2024-01-01, status: ACTIVE; link meter-scale with an active Scales nomenclature entry — scaleCode: e.g. `SC01`, scaleType: e.g. `A+`).
8. Create an **original** billing data by scales record via `POST /billing-by-scales` with:
   - `identifier`: POD identifier from step 2 (`POD_TEST_2708`)
   - `dateFrom`: `2025-12-23`
   - `dateTo`: `2026-01-31`
   - `correction`: `false`
   - `override`: `false`
   - `invoiceNumber`: `INV2708001`
   - `invoiceDate`: current date-time
   - `billingPowerInKw`: `100`
   - `saveRecordForIntermediatePeriod`: `false`
   - `saveRecordForMeterReadings`: `false`
   - `billingByScalesTableCreateRequests`: at least one row with `periodFrom`: `2025-12-23`, `periodTo`: `2026-01-31`, `meterNumber`: `MTR2708001`, `scaleCode`: `SC01`, `scaleType`: `A+`, `oldMeterReading`: `1000`, `newMeterReading`: `1500`, `difference`: `500`, `multiplier`: `1`, `correction`: `0`, `deducted`: `0`, `totalVolumes`: `500`, `volumes`: `500`, `unitPrice`: `0.15`, `totalValue`: `75.00`, `index`: `0`.
   - Record the returned `id` as `originalId`.

---

## Backend Test Cases

### TC-BE-1 (Negative): Reject correction with different dateFrom than original

**Description:** Verify that the API rejects creation of a correction billing data by scales record when the correction's `dateFrom` is different from the original data's `dateFrom`, while `dateTo` matches.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original record has `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.
2. Original billing data by scales is in status ACTIVE with `id` = `originalId`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `identifier`: same POD identifier (`POD_TEST_2708`)
   - `dateFrom`: `2026-01-23` (different from original's `2025-12-23`)
   - `dateTo`: `2026-01-31` (same as original)
   - `correction`: `true`
   - `override`: `false`
   - `invoiceNumber`: `INV2708002`
   - `invoiceDate`: current date-time
   - `invoiceCorrection`: `INV2708001`
   - `billingPowerInKw`: `100`
   - `saveRecordForIntermediatePeriod`: `false`
   - `saveRecordForMeterReadings`: `false`
   - `billingByScalesTableCreateRequests`: one row with valid data for the correction period.
2. Inspect the HTTP response status and body.

**Expected test case results:** The system returns an error (HTTP 409 Conflict or HTTP 400 Bad Request) indicating that the correction's header period `dateFrom` does not match the original data's header period. The correction record is NOT created in the database.

**Actual result (bug):** The system creates the correction record successfully (HTTP 201 Created) without checking that the header period matches the original. The correction record has a `dateFrom` of `2026-01-23` while the original has `2025-12-23`.

**References:** PDT-2708; `BillingByScalesService.create()` line 80.

---

### TC-BE-2 (Negative): Reject correction with different dateTo than original

**Description:** Verify that the API rejects creation of a correction billing data by scales record when the correction's `dateTo` is different from the original data's `dateTo`, while `dateFrom` matches.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original record has `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `identifier`: same POD identifier
   - `dateFrom`: `2025-12-23` (same as original)
   - `dateTo`: `2026-02-28` (different from original's `2026-01-31`)
   - `correction`: `true`
   - `override`: `false`
   - `invoiceNumber`: `INV2708003`
   - `invoiceDate`: current date-time
   - `invoiceCorrection`: `INV2708001`
   - `billingPowerInKw`: `100`
   - remaining fields valid
2. Inspect the HTTP response status and body.

**Expected test case results:** The system returns an error (HTTP 409 or 400) stating the correction's `dateTo` does not match the original data. Correction record is NOT created.

**Actual result (bug):** System creates the correction with a mismatched `dateTo` (HTTP 201).

**References:** PDT-2708.

---

### TC-BE-3 (Negative): Reject correction when both dateFrom and dateTo differ from original

**Description:** Verify that when both the correction's `dateFrom` and `dateTo` differ from the original, the system rejects the request.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original: `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `dateFrom`: `2026-01-01` (different)
   - `dateTo`: `2026-02-28` (different)
   - `correction`: `true`
   - `invoiceCorrection`: `INV2708001`
   - All other fields valid.
2. Inspect the response.

**Expected test case results:** Error response (HTTP 409 or 400) rejecting the correction due to mismatched header period. No correction record created.

**Actual result (bug):** Correction created successfully with completely different header period.

**References:** PDT-2708.

---

### TC-BE-4 (Positive): Successfully create correction with matching header period

**Description:** Verify that the system allows creation of a correction billing data by scales record when the correction's header period (dateFrom and dateTo) exactly matches the original data's header period.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original: `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `identifier`: same POD identifier
   - `dateFrom`: `2025-12-23` (same as original)
   - `dateTo`: `2026-01-31` (same as original)
   - `correction`: `true`
   - `override`: `false`
   - `invoiceNumber`: `INV2708004`
   - `invoiceDate`: current date-time
   - `invoiceCorrection`: `INV2708001`
   - `billingPowerInKw`: `100`
   - `saveRecordForIntermediatePeriod`: `false`
   - `saveRecordForMeterReadings`: `false`
   - `billingByScalesTableCreateRequests`: one row with `periodFrom`: `2025-12-23`, `periodTo`: `2026-01-31`, valid meter and scale data, corrected meter readings and volumes.
2. Inspect the HTTP response status and body.
3. Call `GET /billing-by-scales/{returnedId}` to verify the saved record.

**Expected test case results:** The system creates the correction successfully (HTTP 201). The returned record has `correction: true`, `dateFrom: 2025-12-23`, `dateTo: 2026-01-31` matching the original. The response body contains the new record's ID.

**References:** PDT-2708.

---

### TC-BE-5 (Positive): Create original (non-correction) billing data by scales successfully

**Description:** Verify that creating an original (non-correction) billing data by scales record works without the header-period-match check being triggered.

**Preconditions:**
1. Complete steps 1–7 from Test data above. No existing billing data by scales for this POD and period.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `identifier`: POD identifier
   - `dateFrom`: `2026-02-01`
   - `dateTo`: `2026-02-28`
   - `correction`: `false`
   - `override`: `false`
   - `invoiceNumber`: `INV2708005`
   - `invoiceDate`: current date-time
   - `billingPowerInKw`: `100`
   - `saveRecordForIntermediatePeriod`: `false`
   - `saveRecordForMeterReadings`: `false`
   - `billingByScalesTableCreateRequests`: one valid row within the header period.
2. Inspect the response.

**Expected test case results:** Record created successfully (HTTP 201). The overlap check passes (no existing record for this POD and period). The record is saved as non-correction (`correction: false`).

**References:** PDT-2708.

---

### TC-BE-6 (Negative): Reject correction with dateFrom one day earlier than original

**Description:** Verify that even a one-day difference in `dateFrom` is caught by the header period validation for correction records.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original: `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `dateFrom`: `2025-12-22` (one day before original)
   - `dateTo`: `2026-01-31` (same as original)
   - `correction`: `true`
   - `invoiceCorrection`: `INV2708001`
   - All other required fields valid.
2. Inspect the response.

**Expected test case results:** Error response (HTTP 409 or 400). The system rejects the correction because `dateFrom` does not exactly match the original.

**Actual result (bug):** Correction created with the off-by-one `dateFrom`.

**References:** PDT-2708.

---

### TC-BE-7 (Negative): Reject correction with dateTo one day later than original

**Description:** Verify that even a one-day difference in `dateTo` is caught.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original: `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `dateFrom`: `2025-12-23` (same)
   - `dateTo`: `2026-02-01` (one day after original)
   - `correction`: `true`
   - `invoiceCorrection`: `INV2708001`
   - All other required fields valid.
2. Inspect the response.

**Expected test case results:** Error response (HTTP 409 or 400). Correction rejected due to mismatched `dateTo`.

**Actual result (bug):** Correction created with the off-by-one `dateTo`.

**References:** PDT-2708.

---

### TC-BE-8 (Negative): Reject correction with swapped dateFrom and dateTo

**Description:** Verify that the system rejects a correction where the correction's dateFrom equals the original's dateTo and vice versa (swapped values).

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original: `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `dateFrom`: `2026-01-31` (swapped — equals original dateTo)
   - `dateTo`: `2025-12-23` (swapped — equals original dateFrom)
   - `correction`: `true`
   - `invoiceCorrection`: `INV2708001`
   - All other fields valid.
2. Inspect the response.

**Expected test case results:** Error response. The `ValidBillingByScalesPeriod` validator should reject `dateTo` before `dateFrom` (HTTP 400). Even if that passes, the header period validation should reject the mismatch. No correction record created.

**References:** PDT-2708; `ValidBillingByScalesPeriod` annotation.

---

### TC-BE-9 (Positive): Create correction with override=true and matching header period

**Description:** Verify that creating a correction with `override=true` is allowed when the header period matches the original.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original: `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `dateFrom`: `2025-12-23` (same as original)
   - `dateTo`: `2026-01-31` (same as original)
   - `correction`: `true`
   - `override`: `true`
   - `invoiceCorrection`: `INV2708001`
   - All other required fields valid, including rows with negative values (allowed with override).
2. Inspect the response.
3. Call `GET /billing-by-scales/{returnedId}` to verify.

**Expected test case results:** Correction created successfully (HTTP 201). Record shows `correction: true`, `override: true`, matching header period. The override flag allows negative volume/value values in detail rows.

**References:** PDT-2708; `ValidInvoiceCorrection` annotation.

---

### TC-BE-10 (Negative): Reject correction with override=true and mismatched header period

**Description:** Verify that even with `override=true`, the header period must match the original.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original: `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `dateFrom`: `2026-01-01` (different)
   - `dateTo`: `2026-01-31` (same)
   - `correction`: `true`
   - `override`: `true`
   - `invoiceCorrection`: `INV2708001`
   - All other required fields valid.
2. Inspect the response.

**Expected test case results:** Error response (HTTP 409 or 400). Override does not bypass the header period match requirement.

**Actual result (bug):** Correction with override created with mismatched dateFrom.

**References:** PDT-2708.

---

### TC-BE-11 (Negative): Reject correction when no original data exists for the POD

**Description:** Verify that the system rejects a correction request when there is no original (non-correction) billing data by scales record for the specified POD.

**Preconditions:**
1. Complete steps 1–7 from Test data above (POD exists, meter installed). No billing data by scales record created for this POD.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `identifier`: POD identifier from step 2
   - `dateFrom`: `2025-12-23`
   - `dateTo`: `2026-01-31`
   - `correction`: `true`
   - `invoiceCorrection`: `INV_NO_ORIGINAL`
   - All other required fields valid.
2. Inspect the response.

**Expected test case results:** Error response indicating no original data found for correction. Correction cannot be created without a corresponding original record.

**References:** PDT-2708.

---

### TC-BE-12 (Negative): Reject correction when invoiceCorrection is blank but correction=true

**Description:** Verify that the `ValidInvoiceCorrection` annotation rejects a correction request when `invoiceCorrection` is empty/blank while `correction=true`.

**Preconditions:**
1. Complete steps 1–8 from Test data above.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `correction`: `true`
   - `invoiceCorrection`: `""` (empty string)
   - `dateFrom`: `2025-12-23`, `dateTo`: `2026-01-31` (matching original)
   - All other required fields valid.
2. Inspect the response.

**Expected test case results:** Validation error (HTTP 400): "when correction is true, InvoiceCorrection should not be empty." No record created.

**References:** PDT-2708; `ValidInvoiceCorrection` annotation.

---

### TC-BE-13 (Negative): Reject correction where override=true but correction=false

**Description:** Verify that the `ValidInvoiceCorrection` annotation rejects a request with `override=true` but `correction=false`.

**Preconditions:**
1. Complete steps 1–7 from Test data above.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `correction`: `false`
   - `override`: `true`
   - `invoiceCorrection`: `null`
   - Other fields valid.
2. Inspect the response.

**Expected test case results:** Validation error (HTTP 400): "Override shouldn't be checked if correction is not checked." No record created.

**References:** PDT-2708; `ValidInvoiceCorrection` annotation.

---

### TC-BE-14 (Negative): Reject creation with dateFrom after dateTo

**Description:** Verify that the `ValidBillingByScalesPeriod` validator rejects a request where `dateFrom` is after `dateTo`.

**Preconditions:**
1. Complete steps 1–7 from Test data above.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `dateFrom`: `2026-02-01`
   - `dateTo`: `2026-01-01`
   - `correction`: `false`
   - Other fields valid.
2. Inspect the response.

**Expected test case results:** Validation error (HTTP 400): "PeriodTo must be before (or equal) periodFrom." No record created.

**References:** PDT-2708; `ValidBillingByScalesPeriod`.

---

### TC-BE-15 (Negative): Reject creation with period exceeding one year

**Description:** Verify that the header period cannot exceed one year.

**Preconditions:**
1. Complete steps 1–7 from Test data above.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - `dateFrom`: `2025-01-01`
   - `dateTo`: `2026-06-01` (more than one year)
   - `correction`: `false`
   - Other fields valid.
2. Inspect the response.

**Expected test case results:** Validation error (HTTP 400): "Period should be limited to one-year time interval." No record created.

**References:** PDT-2708; `ValidBillingByScalesPeriod`.

---

### TC-BE-16 (Positive): View correction data by scales confirms matching header period

**Description:** Verify that `GET /billing-by-scales/{id}` returns the correction record with the correct header period, confirming it was saved correctly.

**Preconditions:**
1. Complete steps 1–8 from Test data above.
2. Create a valid correction via `POST /billing-by-scales` with matching header period (`dateFrom: 2025-12-23`, `dateTo: 2026-01-31`, `correction: true`, `invoiceCorrection: INV2708001`). Record the returned `correctionId`.

**Test steps:**
1. Send `GET /billing-by-scales/{correctionId}`.
2. Inspect the response body.

**Expected test case results:** HTTP 200. Response body contains `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`, `correction: true`, `invoiceCorrection: INV2708001`, `status: ACTIVE`. The detail rows (billingByScalesTableCreateRequests) are present with the corrected values.

**References:** PDT-2708.

---

### TC-BE-17 (Negative): Reject correction with empty/missing dateFrom

**Description:** Verify that the API rejects a correction request when `dateFrom` is null or missing.

**Preconditions:**
1. Complete steps 1–8 from Test data above.

**Test steps:**
1. Send `POST /billing-by-scales` with `dateFrom: null`, `dateTo: 2026-01-31`, `correction: true`, `invoiceCorrection: INV2708001`, other fields valid.
2. Inspect the response.

**Expected test case results:** Validation error (HTTP 400): "Date From must not be null." No record created.

**References:** PDT-2708; `@NotNull` on `dateFrom`.

---

### TC-BE-18 (Negative): Reject correction with empty/missing dateTo

**Description:** Verify that the API rejects a correction request when `dateTo` is null or missing.

**Preconditions:**
1. Complete steps 1–8 from Test data above.

**Test steps:**
1. Send `POST /billing-by-scales` with `dateFrom: 2025-12-23`, `dateTo: null`, `correction: true`, `invoiceCorrection: INV2708001`, other fields valid.
2. Inspect the response.

**Expected test case results:** Validation error (HTTP 400): "Date to must not be null." No record created.

**References:** PDT-2708; `@NotNull` on `dateTo`.

---

### TC-BE-19 (Negative): Reject non-correction record with overlapping period for same POD

**Description:** Verify that creating a non-correction record for the same POD and overlapping date range is rejected.

**Preconditions:**
1. Complete steps 1–8 from Test data above. Original: `dateFrom: 2025-12-23`, `dateTo: 2026-01-31`, `correction: false`.

**Test steps:**
1. Send `POST /billing-by-scales` with:
   - Same POD identifier
   - `dateFrom`: `2026-01-01` (overlaps with original)
   - `dateTo`: `2026-02-28`
   - `correction`: `false`
   - `override`: `false`
   - Other fields valid.
2. Inspect the response.

**Expected test case results:** Error (HTTP 409 Conflict): "Can't create billing data by scale, because in system already exist billing data by scale which include same POD and period from and period to has overlap." No record created.

**References:** PDT-2708; `checkBillingByScalesWithPodPeriodFromAndPeriodTo()`.

---

### TC-BE-20 (Positive): Create multiple corrections for the same original — all with matching header period

**Description:** Verify that more than one correction can be created for the same original data, as long as each correction's header period matches the original.

**Preconditions:**
1. Complete steps 1–8 from Test data above.
2. Create a first correction via `POST /billing-by-scales` with matching header period (dateFrom: `2025-12-23`, dateTo: `2026-01-31`, `correction: true`, `invoiceCorrection: INV2708001`). Record its ID.

**Test steps:**
1. Send another `POST /billing-by-scales` with:
   - Same POD identifier
   - `dateFrom`: `2025-12-23` (matches original)
   - `dateTo`: `2026-01-31` (matches original)
   - `correction`: `true`
   - `invoiceCorrection`: `INV2708004` (references first correction or original)
   - Other fields valid with updated values.
2. Inspect the response.

**Expected test case results:** HTTP 201 Created. The second correction is saved successfully because corrections skip the overlap check and the header period matches the original.

**References:** PDT-2708.

---

## References

- **Jira:** PDT-2708 – The system doesn't restrict creation of the Correction data by scales with a difference in the header period with the original data.
- **Backend service:** `BillingByScalesService.create()` — line 80 skips overlap check for corrections but missing header period equality check.
- **Controller:** `BillingByScalesController` at `/billing-by-scales` (POST, GET, PUT, DELETE).
- **Entity:** `BillingByScale` (schema: `pod`, table: `billing_by_scale`) — `dateFrom`, `dateTo`, `correction`, `override`.
- **Request DTO:** `BillingByScalesCreateRequest` with `@ValidBillingByScalesPeriod`, `@ValidInvoiceCorrection`, `@ValidPeriods`.
- **Custom validators:** `ValidBillingByScalesPeriod` (date range, max 1 year), `ValidInvoiceCorrection` (correction/override/invoiceCorrection consistency).
