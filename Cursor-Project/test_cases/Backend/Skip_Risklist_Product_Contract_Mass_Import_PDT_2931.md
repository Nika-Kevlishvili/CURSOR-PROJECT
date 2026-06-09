# Skip RiskList permission — product contract mass import (PDT-2931)

**Jira:** PDT-2931 (Phoenix Delivery)  
**Type:** Customer Feedback  
**Summary:** When the test user has **Skip RiskList** (`bg.energo.phoenix.security.verb.skip_risklist`) on **Product Contracts**, product contract mass import must skip the external Risk List check for **create**, **edit same version**, and **edit new version** rows — all rows complete successfully and contracts are persisted.

**Scope:** Backend API flow on **Dev** after PDT-2931 merge (`phoenix-core-lib` `aca0283f6`, `phoenix-core` `046149ba71`). Mass import uploads via `POST /mass-import/product-contracts/files/upload` (HTTP **202 ACCEPTED**), async processing via `PRODUCT_CONTRACT_MASS_IMPORT` process type. Excel routing: column 0 = contract number, column 1 = version, column 2 = `C` (new version) or `E` (same version); empty 0/1/2 = **create** row. Risk rejection message when check runs: `RISK_LIST_DECISION-Can't conclude a contract because of risk assessment restriction;`. Use **CONSUMER** POD type (not all-GENERATOR) so the risk path is exercised.

**Risk trigger (correct methodology):** Risk List evaluates whether **consumption is excessively high** — not whether the customer UIC is on a block list. Preconditions use a CONSUMER POD with **`estimatedMonthlyAvgConsumption` at Phoenix max (99_999_999)** and any fresh customer. Yearly consumption sent to Risk List = `monthly * 12 / 1000` (≈ 1_199_999.988 MWh). **Without** `skip_risklist` → Save / mass-import row must fail risk check; **with** `skip_risklist` → must succeed.

---

## ⚠️ Current status — known backend defect (PDT-2931 fix incomplete on Dev)

These test cases assert the **expected** behavior per the PDT-2931 acceptance criteria (skip RiskList → mass import succeeds). On the **current Dev build**, the positive mass-import cases **TC-BE-1 … TC-BE-4 are expected to FAIL** because the merged fix does not propagate `skip_risklist` into the mass-import process. They will turn green only after the backend is corrected.

**Root cause (code evidence):**
- PDT-2931 (`aca0283f6`) added a `Set<String> permissions` parameter to `ProductContractAdditionalParametersService.create/update` and threads the mass-import process permissions through `ProductContractService.create/edit` → `ProductContractMassImportProcessService`. The RiskList check now runs when `permissions != null && !permissions.contains(SKIP_RISKLIST)`.
- The mass-import process permission set is built in `MassImportBaseService.uploadMassImportFile`: it takes `getPermissionsFromContext(PRODUCT_CONTRACTS_MI)` and only additionally adds `PRODUCT_CONTRACT_EDIT_LOCKED` from the `PRODUCT_CONTRACTS` context.
- `PRODUCT_CONTRACTS_MI` context = `{ MI_EDIT, MI_CREATE, PRODUCT_CONTRACT_MI_EDIT_LOCKED }` (`PermissionContextEnum`). **`skip_risklist` lives in `PRODUCT_CONTRACTS`** and is **never** copied into the process permissions.
- Result: for mass import, `permissions` is non-null but lacks `skip_risklist` → the RiskList API is always called (the `Failed to get response from Risk List API` / `RISK_LIST_DECISION` errors observed). The direct `POST /product-contract` path passes `permissions = null` → falls back to the live `PRODUCT_CONTRACTS` context check → skip works (TC-BE-5 passes).

**Suggested backend fix (for the dev team):** in `MassImportBaseService.uploadMassImportFile`, also copy `SKIP_RISKLIST` from the `PRODUCT_CONTRACTS` context into the process permissions when present (mirroring the existing `PRODUCT_CONTRACT_EDIT_LOCKED` handling), so mass import matches the interface path.

**Expected results matrix on current Dev:**

| TC | Asserts | Current Dev result |
|----|---------|--------------------|
| TC-BE-1 create + skip | success | ❌ FAIL (RiskList still called) — until backend fix |
| TC-BE-2 edit same + skip | success | ❌ FAIL — until backend fix |
| TC-BE-3 edit new version + skip | success | ❌ FAIL — until backend fix |
| TC-BE-4 combined + skip | all rows success | ❌ FAIL — until backend fix |
| TC-BE-5 direct POST + skip | success | ✅ PASS (skip works on interface path) |
| TC-BE-6 create without skip | risk rejection | ✅ PASS (control) |
| TC-BE-7 upload without mi_create | 403 | ✅ PASS (auth guard) |

---

## Test data (preconditions)

Reference appendix only — each TC below repeats its full standalone chain (Rule **TC-STANDALONE-PRE.0**).

- **Environment:** Dev (`http://10.236.20.11:8091` per Swagger dev spec).
- **Permission verb IDs:**
  - Skip RiskList: `bg.energo.phoenix.security.verb.skip_risklist` (context **PRODUCT_CONTRACTS**)
  - Mass import create: `bg.energo.phoenix.security.verb.mi_create` (context **PRODUCT_CONTRACTS_MI**)
  - Mass import edit: `bg.energo.phoenix.security.verb.mi_edit` (context **PRODUCT_CONTRACTS_MI**)
  - Process view: `bg.energo.phoenix.security.verb.process_view` (context **PROCESS**)

---

## Backend Test Cases

### TC-BE-1 (Positive): Mass import CREATE row succeeds when user has Skip RiskList on high-consumption POD

**Description:** Verify that a product contract **create** mass import row completes without Risk List rejection when the uploading user has **skip_risklist** and the POD has max consumption (risk would block Save if check were enforced).

**Preconditions:**
1. Authenticate on Dev API (Bearer JWT) as **User A** with **PRODUCT_CONTRACTS_MI** (`mi_create`) and **PRODUCT_CONTRACTS** (`skip_risklist`), plus **PROCESS** (`process_view`).
2. Create a fresh **ACTIVE** customer via `POST /customer` (any legal/private identifier).
3. Create a CONSUMER POD via `POST /pod` with **`estimatedMonthlyAvgConsumption` = 99_999_999** (Phoenix max) — this drives Risk List via high consumption (`monthly * 12 / 1000` ≈ 1_199_999.988). Confirm risk path: as **User B** (MI perms **without** `skip_risklist`), a create-row smoke import or `POST /product-contract` must fail with `RISK_LIST_DECISION` / `risk assessment restriction`.
4. Create a product via `POST /product` (ACTIVE, valid term and delivery type for MI template); capture `productDetailId` / version identifiers required by template.
5. Create terms via `POST /terms` linked to product from step 4.
6. Create a price component via `POST /price-component` linked to product from step 4.
7. Download the official template via `GET /mass-import/product-contracts/template/download`; keep header row unchanged.
8. Fill **one create row** (contract number, version, and C/E columns **empty**) with valid MI fields for customer from step 2 and POD from step 3, product from step 4, employee, interest rate, banking, and estimated consumption per template validation rules.
9. Ensure no REAL invoice lock applies (new create — no prior contract for this row).

**Test steps:**
1. `POST /mass-import/product-contracts/files/upload` as **User A** with multipart field `file` = Excel from precondition 8; assert HTTP **202 ACCEPTED**.
2. `GET /process` filtered by type **PRODUCT_CONTRACT_MASS_IMPORT** (or list and pick latest for User A); capture `processId`.
3. Poll `GET /process/{processId}` until `status` = **COMPLETED** (timeout per Dev SLA).
4. `GET /process/{processId}/report/download`; open report and locate the create row `record_identifier` / row index.
5. `GET /product-contract` list or detail API (per Swagger) using contract number / detail id from report `record_identifier_version`.

**Expected test case results:** Process `status` = **COMPLETED**. Report row shows **success** = true; `error_message` empty or absent. No text containing `RISK_LIST_DECISION` or `risk assessment restriction`. A new product contract exists in DB/API for the customer from step 2; contract detail id matches `record_identifier_version`. No partial/failed row for the create operation.

**References:** PDT-2931; `ProductContractMassImportProcessService.processRow` (create branch); `ProductContractAdditionalParametersService.create` (skip_risklist branch).

---

### TC-BE-2 (Positive): Mass import EDIT same version (`E`) succeeds with Skip RiskList

**Description:** Verify **edit same version** mass import (`column 2 = E`) updates the contract without Risk List failure when skip permission is granted.

**Preconditions:**
1. Authenticate on Dev API as **User A** with **PRODUCT_CONTRACTS_MI** (`mi_edit`), **PRODUCT_CONTRACTS** (`skip_risklist`), and **PROCESS** (`process_view`).
2. Create fresh ACTIVE customer via `POST /customer`; capture `customerId`, `customerVersionId`, `customerNumber`.
3. Create CONSUMER POD via `POST /pod` with **`estimatedMonthlyAvgConsumption` = 99_999_999**; capture `podDetailId`.
4. Create product, terms, and price component (steps 4–6 pattern from TC-BE-1); capture product identifiers for template.
5. Create a baseline product contract via `POST /product-contract` linking customer step 2, POD step 3, product step 4 (status suitable for edit — e.g. SIGNED per business rules); capture `contractNumber`, `contractDetailId`, and **version number** `V`.
6. Download template via `GET /mass-import/product-contracts/template/download`.
7. Fill **one edit row**: column 0 = `contractNumber` from step 5, column 1 = `V`, column 2 = **`E`**; update allowed fields per template (e.g. estimated consumption, banking) without violating validation.
8. Confirm contract from step 5 has **no REAL invoice lock** blocking edit, or User A also has `product_contract_mi_edit_locked` if lock exists.

**Test steps:**
1. `POST /mass-import/product-contracts/files/upload` as **User A** with edit-row Excel; assert **202 ACCEPTED**.
2. `GET /process` filtered by **PRODUCT_CONTRACT_MASS_IMPORT**; capture `processId`.
3. Poll `GET /process/{processId}` until **COMPLETED**.
4. `GET /process/{processId}/report/download`; verify edit row success.
5. `GET /product-contract/{id}` (or list by number) for contract from step 5; confirm updated fields match Excel changes and version count unchanged (same version edit).

**Expected test case results:** Row **success** = true; no `RISK_LIST_DECISION` error. Contract number unchanged; same version `V` updated in place. Process **COMPLETED** with zero failed rows.

**References:** `ProductContractMassImportProcessService` edit branch (`createEdit == E`); `ProductContractAdditionalParametersService.update`.

---

### TC-BE-3 (Positive): Mass import EDIT new version (`C`) succeeds with Skip RiskList

**Description:** Verify **edit — create new version** mass import (`column 2 = C`) creates a new contract version without Risk List failure when skip permission is granted.

**Preconditions:**
1. Authenticate as **User A** (`mi_edit`, `skip_risklist`, `process_view`).
2. Create fresh ACTIVE customer via `POST /customer`; capture identifiers.
3. Create CONSUMER POD via `POST /pod` with **`estimatedMonthlyAvgConsumption` = 99_999_999**; capture `podDetailId`.
4. Create product, terms, price component; capture template-required IDs.
5. Create signed/active product contract via `POST /product-contract` for customer step 2; capture `contractNumber`, current `versionId`, version number `V`.
6. Download template via `GET /mass-import/product-contracts/template/download`.
7. Fill **one row**: column 0 = `contractNumber`, column 1 = `V` (or `0` for latest per template rules), column 2 = **`C`**, column 3 = **start date** (`yyyy-MM-dd` future valid date required for new version).
8. No invoice lock blocking new version unless User A has `product_contract_mi_edit_locked`.

**Test steps:**
1. `POST /mass-import/product-contracts/files/upload` as **User A** with new-version Excel; assert **202 ACCEPTED**.
2. `GET /process` filtered by **PRODUCT_CONTRACT_MASS_IMPORT**; capture `processId`.
3. Poll `GET /process/{processId}` until `status` = **COMPLETED**.
4. `GET /process/{processId}/report/download`; assert row **success** = true.
5. `GET /product-contract` by number; verify **new version** exists (version number > `V` or new `contractDetailId` per API), `startDate` matches column 3.

**Expected test case results:** Row **success** = true; no risk rejection message. New contract version persisted. Process **COMPLETED**; no failed rows.

**References:** `ProductContractMassImportProcessService` (`setSavingAsNewVersion(true)`); PDT-2931 Jira description (Contract edit — create new version).

---

### TC-BE-4 (Positive): Combined Excel — create + edit same version + edit new version — all rows succeed

**Description:** End-to-end batch: one file containing three row types (create, `E`, `C`) with max-consumption POD; with **skip_risklist**, **all rows** must pass and process completes with no failures.

**Preconditions:**
1. Authenticate on Dev API as **User A** with **PRODUCT_CONTRACTS_MI** (`mi_create`, `mi_edit`), **PRODUCT_CONTRACTS** (`skip_risklist`), and **PROCESS** (`process_view`).
2. Create fresh ACTIVE customer via `POST /customer`; capture `customerNumber`, `customerId`, `customerVersionId`.
3. Create CONSUMER POD via `POST /pod` with **`estimatedMonthlyAvgConsumption` = 99_999_999**; capture `podDetailId`. Validate risk path with **User B** (no `skip_risklist`): one-row create MI must fail with `RISK_LIST_DECISION`.
4. Create a product via `POST /product` (ACTIVE, valid term); capture `productDetailId` / version id for template.
5. Create terms via `POST /terms` linked to product from step 4.
6. Create a price component via `POST /price-component` linked to product from step 4 (rate, currency, type per Swagger).
7. Create **baseline contract** via `POST /product-contract` linking customer step 2, POD step 3, product step 4 (SIGNED or ACTIVE per Dev rules); capture `contractNumber`, version number `V`, `contractDetailId`.
8. Download template via `GET /mass-import/product-contracts/template/download`; build **three-row Excel**:
   - **Row 1 (create):** columns 0/1/2 **empty** — new contract for customer step 2, CONSUMER POD step 3, product step 4.
   - **Row 2 (edit same version):** col 0 = `contractNumber`, col 1 = `V`, col 2 = **`E`** — update allowed field (e.g. estimated consumption).
   - **Row 3 (edit new version):** col 0 = `contractNumber`, col 1 = `V`, col 2 = **`C`**, col 3 = **start date** `yyyy-MM-dd` (future valid date).
9. Ensure baseline contract from step 7 has no REAL invoice lock, or User A has `product_contract_mi_edit_locked`.

**Test steps:**
1. `POST /mass-import/product-contracts/files/upload` as **User A** with three-row file; assert **202 ACCEPTED**.
2. `GET /process` filtered by **PRODUCT_CONTRACT_MASS_IMPORT**; capture `processId`.
3. Poll `GET /process/{processId}` until `status` = **COMPLETED**.
4. `GET /process/{processId}/report/download`; count rows with **success** = true vs false.
5. `GET /product-contract` by numbers from report: verify new contract from row 1, updated fields on version `V` (row 2), and new version after row 3.

**Expected test case results:** Process **COMPLETED**. **All three rows** `success` = true. **Zero** rows with `RISK_LIST_DECISION` or `risk assessment restriction`. Report shows three successful `record_identifier_version` values. No rollback of successful rows due to another row.

**References:** PDT-2931 full acceptance (interface parity for create / edit same / edit new version).

---

### TC-BE-5 (Positive): Direct POST /product-contract succeeds with Skip RiskList (interface parity baseline)

**Description:** Confirm **User A** with `skip_risklist` can create a contract with max-consumption POD via `POST /product-contract` without risk rejection — establishes parity expectation that mass import must match the interface path.

**Preconditions:**
1. Authenticate on Dev API as **User A** with **PRODUCT_CONTRACTS** (`product_contract_create`, `skip_risklist`).
2. Create fresh ACTIVE customer via `POST /customer`; capture `customerId`, `customerVersionId`, `customerNumber`.
3. Create CONSUMER POD via `POST /pod` with **`estimatedMonthlyAvgConsumption` = 99_999_999**; capture `podDetailId`.
4. Create a product via `POST /product` (ACTIVE); capture `productDetailId` and product version id.
5. Create terms via `POST /terms` linked to product from step 4.
6. Create a price component via `POST /price-component` linked to product from step 4.
7. Build `ProductContractCreateRequest` JSON per Swagger with at minimum: `basicParameters.customerId` = step 2, `basicParameters.customerVersionId` = step 2, `productContractPointOfDeliveries[].pointOfDeliveryDetailId` = step 3, product/version ids from step 4, `additionalParameters.estimatedTotalConsumptionUnderContractKwh` > 0, valid `employeeId`, `interestRateId`, and banking details per template validation.

**Test steps:**
1. `POST /product-contract` as **User A** with JSON body from precondition 7.
2. Assert HTTP **200** or **201**; parse response body for `id` (contract id) and contract number field per Swagger schema.
3. `GET /product-contract/{id}` from step 2; assert `customerId` matches step 2 and contract status is created per payload.

**Expected test case results:** HTTP **200/201**; response body contains contract `id`. No error text containing `RISK_LIST_DECISION` or `risk assessment restriction`. `GET /product-contract/{id}` returns the contract linked to customer from step 2. Demonstrates skip works on direct API path (`permissions = null` → live PermissionService context).

**References:** `ProductContractController`; Jira description "same as in the interface".

---

### TC-BE-6 (Negative): Mass import CREATE fails with risk error when Skip RiskList is absent

**Description:** Control case — **User B** without `skip_risklist` must get a failed create mass import row with max-consumption POD (proves risk path is testable and skip permission is the differentiator).

**Preconditions:**
1. Authenticate on Dev API as **User B** with **PRODUCT_CONTRACTS_MI** (`mi_create`) and **PROCESS** (`process_view`) — explicitly **without** `bg.energo.phoenix.security.verb.skip_risklist` on **PRODUCT_CONTRACTS**.
2. Create fresh ACTIVE customer via `POST /customer`; capture `customerNumber`, `customerId`, `customerVersionId`.
3. Create CONSUMER POD via `POST /pod` with **`estimatedMonthlyAvgConsumption` = 99_999_999**; capture `podDetailId`.
4. Create a product via `POST /product` (ACTIVE); capture product/version ids for template.
5. Create terms via `POST /terms` linked to product from step 4.
6. Create a price component via `POST /price-component` linked to product from step 4.
7. Download template via `GET /mass-import/product-contracts/template/download`; fill **one create row** (columns 0/1/2 empty) with customer step 2, POD step 3, and product step 4 mandatory fields.
8. Record current contract count for customer step 2 via `GET /product-contract` list (or equivalent filter) before upload.

**Test steps:**
1. `POST /mass-import/product-contracts/files/upload` as **User B** with Excel from precondition 7; assert **202 ACCEPTED** (upload accepted; row failure expected during processing).
2. `GET /process` for **PRODUCT_CONTRACT_MASS_IMPORT**; capture `processId`.
3. Poll `GET /process/{processId}` until `status` = **COMPLETED**.
4. `GET /process/{processId}/report/download`; read failed row `error_message` and `success` flag.
5. `GET /product-contract` list for customer step 2; compare contract count to precondition 8.

**Expected test case results:** Process `status` = **COMPLETED** with row **success** = false. `error_message` contains `RISK_LIST_DECISION` and `Can't conclude a contract because of risk assessment restriction`. Contract count for customer step 2 is **unchanged** vs precondition 8 (no new contract from this import). Upload HTTP remains **202** (async processing model).

**References:** `ProductContractAdditionalParametersService.validateCustomerInRiskListAPI`; negative control for PDT-2931.

---

### TC-BE-7 (Negative): Mass import upload rejected when user lacks mass import create permission

**Description:** Authorization guard — user with `skip_risklist` but **without** `mi_create` cannot upload product contract mass import files.

**Preconditions:**
1. Authenticate as **User C** with **PRODUCT_CONTRACTS** (`skip_risklist` only) — **no** `mi_create` / `mi_edit` on **PRODUCT_CONTRACTS_MI**.
2. Download template; prepare minimal valid Excel file (one create row) — file content valid so failure is permission-only.

**Test steps:**
1. `POST /mass-import/product-contracts/files/upload` as **User C** with multipart `file`.
2. Assert response status and body.

**Expected test case results:** HTTP **403 Forbidden** (or environment-documented authorization failure). **No** new `PRODUCT_CONTRACT_MASS_IMPORT` process created for User C (verify via `GET /process` if ambiguous). No contract rows processed.

**References:** `MassImportController.uploadFile`; `PermissionValidator` on mass import endpoints.

---

## References

- **Jira:** [PDT-2931](https://oppa-support.atlassian.net/browse/PDT-2931) — Skip Risk list permission must be applied to the mass imports.
- **Code:** `ProductContractMassImportProcessService.java`, `ProductContractAdditionalParametersService.java`, `MassImportBaseService.java`, `MassImportController.java`.
- **Swagger (Dev):** `Cursor-Project/config/swagger/dev/swagger-spec.json` — `/mass-import/{domainType}/files/upload`, `/process/{id}`, `/process/{id}/report/download`.
- **Permission verb:** `bg.energo.phoenix.security.verb.skip_risklist` (`PermissionEnum.SKIP_RISKLIST`).
- **Dev commits:** `phoenix-core-lib` `aca0283f6`, `phoenix-core` `046149ba71`.
