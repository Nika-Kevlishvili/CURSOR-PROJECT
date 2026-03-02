# Product Contract – Create

**Scope:** Product contract creation – Basic Parameters, validations, API and UI.  
**Confluence:** Product Contract Create (2654209), Product contract edit (2228419), Contract statuses logic (80510980).

---

## Test data (preconditions)

- User has permission to create product contracts.
- Customer and other required master data exist (if applicable).
- Phoenix EPRES (or equivalent) and API available (e.g. POST `/product-contract` or create flow endpoint).

---

## TC-1: Create product contract with valid Basic Parameters (happy path)

**Objective:** Verify that a product contract can be created with all required and valid Basic Parameters.

**Preconditions:** User logged in; create form or API available.

**Steps:**
1. Open the product contract create form (UI) or prepare POST request (API).
2. Enter valid Basic Parameters:
   - Signing date (e.g. past or today).
   - Entry into force date (≥ signing date).
   - Contract term end date (e.g. > entry into force date).
   - Supply activation date (if required).
   - Other mandatory fields per business rules (customer, product, etc.).
3. Save / submit (Create).

**Expected result:** Contract is created successfully; contract number is assigned (e.g. TMP-TEMP initially or final number); status is as per business rule (e.g. SIGNED or DRAFT); contract is visible in list and can be opened; create_date and system_user_id are set.

**References:** Confluence Product Contract Create (2654209); Contract statuses logic (80510980).

---

## TC-2: Create contract – required field missing

**Objective:** Verify that creation fails or is blocked when a required Basic Parameter is missing.

**Preconditions:** Same as TC-1.

**Steps:**
1. Open the product contract create form.
2. Leave one required field empty (e.g. signing date, entry into force date, or customer).
3. Attempt to save / submit.

**Expected result:** Validation error is shown; contract is not created; user is informed which field is required (or list of errors). No new record in `product_contract.contracts`.

**References:** Confluence Product contract edit – mandatory fields and validations.

---

## TC-3: Create contract – entry into force date before signing date

**Objective:** Verify that entry into force date must be on or after signing date.

**Preconditions:** Same as TC-1.

**Steps:**
1. Open the product contract create form.
2. Set signing date = e.g. 2025-06-01.
3. Set entry into force date = e.g. 2025-05-01 (before signing date).
4. Fill other required fields and attempt to save.

**Expected result:** Validation error; contract is not created; message indicates that entry into force date must be on or after signing date.

**References:** Confluence Contract statuses logic – date validations (entry into force ≥ signing date).

---

## TC-4: Create contract – contract term end date before entry into force date

**Objective:** Verify that contract term end date must be after entry into force date (where applicable).

**Preconditions:** Same as TC-1.

**Steps:**
1. Open the product contract create form.
2. Set signing date and entry into force date (e.g. 2025-01-01).
3. Set contract term end date = e.g. 2024-12-31 (before entry into force).
4. Fill other required fields and attempt to save.

**Expected result:** Validation error; contract is not created; message indicates invalid term end date (e.g. must be after entry into force date).

**References:** Confluence Contract statuses logic – term end date validations.

---

## TC-5: Create contract via API (POST) and verify in DB and UI

**Objective:** Verify that a contract created via API is persisted correctly and visible in UI.

**Preconditions:** API access; valid request body (Basic Parameters and any required nested data).

**Steps:**
1. Call POST `/product-contract` (or equivalent create endpoint) with valid payload (signing date, entry into force date, contract term end date, customer, etc.).
2. Assert response: 201 or 200; response body contains contract id and contract number.
3. In the database, query `product_contract.contracts` for the returned id; verify create_date, system_user_id, contract_status, and basic parameters.
4. In the UI, open the contract by id or number and verify Basic Parameter tab shows the same data.

**Expected result:** Contract is created via API; DB and UI show consistent data; no data loss or wrong default status.

**References:** API spec (Swagger/OpenAPI); product_contract.contracts schema.

---

## TC-6: Create contract – termination date disabled on create

**Objective:** Verify that termination date is not editable (or is disabled) during contract creation.

**Preconditions:** Confluence states termination date is "Always disabled in creation".

**Steps:**
1. Open the product contract create form.
2. Check whether termination date field is present and editable.
3. If present, attempt to set a value and save; if disabled, confirm it cannot be changed.

**Expected result:** Termination date is either not shown or disabled on create; contract is created without user-set termination date (termination_date remains null until set by status/scheduler later).

**References:** Confluence Contract statuses logic – "Termination date: Always disabled in creation".

---

## TC-7: Create contract – duplicate or invalid contract number (if applicable)

**Objective:** If contract number is user-entered or has uniqueness rule, verify validation when duplicate or invalid format is used.

**Preconditions:** Business rule: contract number unique or format validated.

**Steps:**
1. Create a first contract with contract number e.g. "TEST-001" (if field is editable on create).
2. Attempt to create a second contract with the same contract number "TEST-001".
3. Or enter an invalid format (e.g. too long, invalid characters) if validated.

**Expected result:** Duplicate: creation fails with uniqueness error. Invalid format: validation error and contract is not created. (Skip or adapt if contract number is system-generated only.)

**References:** Product contract edit – contract number rules.

---

## Summary

| TC  | Focus |
|-----|--------|
| TC-1 | Valid create – happy path |
| TC-2 | Required field missing |
| TC-3 | Entry into force date before signing date |
| TC-4 | Contract term end date before entry into force |
| TC-5 | Create via API and verify in DB and UI |
| TC-6 | Termination date disabled on create |
| TC-7 | Duplicate or invalid contract number (if applicable) |

---

**Confluence / code references**

| Topic | Reference |
|-------|-----------|
| Product Contract Create | Confluence page 2654209 |
| Product contract edit, validations | Confluence page 2228419 |
| Contract statuses, date rules, termination date on create | Confluence page 80510980 |
| API | POST /product-contract (or equivalent); Swagger spec in config/ |
