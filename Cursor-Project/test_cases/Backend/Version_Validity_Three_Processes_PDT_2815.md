# Contract version validity — Mass Email, Mass SMS, Penalty (PDT-2815)

**Jira:** PDT-2815 (Phoenix Delivery)  
**Type:** Customer Feedback  
**Summary:** Changes in processes impacted by version validity  
**Status:** Testing (Hotfix 28.05)

**Scope:** Verify that Mass Email, Mass SMS, and Penalty Calculation use only contract versions in status **Valid/Signed by both sides** (code: `SIGNED` on `contract_details` / `versionStatus` / `contractVersionStatus`), per Jira description and attachment **Using validity in 3 processes.docx** (PDT-2599 follow-on). Test cases are written to **expose description-vs-code gaps**, not to assume code-only behavior.

**Linked:** PDT-2599 (relates to — Service Contract version status)

**Environment:** Dev (Phoenix aligned to `origin/dev`)

---

## Description vs code (dev) — findings for QA

| Area | Jira / attachment expectation | Current dev code (evidence) | Test focus |
|------|------------------------------|----------------------------|------------|
| Mass Email/SMS — explicit version | Reject if version not Valid/Signed | Repository queries filter `SIGNED`; service appends `doesn't exist or is not in status Valid/Signed by both sides` | TC-BE-2, TC-BE-5 |
| Mass Email/SMS — latest fallback | Latest **eligible** SIGNED only | `findLatest*DetailVersionId` uses `max(startDate)` among `SIGNED` only | TC-BE-3, TC-BE-4 |
| Mass Email/SMS — all customers | Same eligibility product + service | `CustomerRepository.fetchActiveContractsAndAssociatedCustomersForMassCommunication` filters both CTEs with `status = 'SIGNED'` | TC-BE-12 |
| Penalty — POD scope (formulas 2/4/6/8) | Only PODs in **respective** version | `findPodsBelongingToContractForAction` still allows respective **or future** versions; error text says "future version" | **Gap — TC-BE-7** |
| Penalty — version resolution (product) | Signed + date window | `ProductContractDetailsRepository.getContractDetailIdByExecutionDate` subquery filters `cd.status = 'SIGNED'` | TC-BE-8, TC-BE-9 |
| Penalty — version resolution (service) | Same Valid/Signed + date window as product | `ServiceContractDetailsRepository.getContractDetailIdByExecutionDate` filters `cd.status = 'SIGNED'` in subquery; verify rejection when only DRAFT covers date | TC-BE-15 |
| Penalty — price component tags | 0 / 1 / many messages | `PenaltyFormulaEvaluationService` messages match attachment | TC-BE-10, TC-BE-11 |
| Penalty — historical / recalculation | Past actions keep version at original `executionDate`; no jump to newer version unless date changes | `ActionValidatorService.getRespectiveContractVersion` resolves by request `executionDate` each call — must prove stability across new versions | TC-BE-16 |
| Mass comm — all-customers (service branch) | Service rows must be eligible SIGNED only | `CustomerRepository.fetchActiveContractsAndAssociatedCustomersForMassCommunication` — service CTE may not filter `contract_version_status` same as product (cross-dep) | TC-BE-12, TC-BE-17 |
| Cross-process consistency | Same contract/date → same version context | Independent code paths — must be proven by data setup | TC-BE-13 |
| Header vs version “Signed by both sides” | BA wording may imply header sub-status | Mass comm SQL does not check `SIGNED_BY_BOTH_SIDES` on contract header | TC-BE-14 (gap probe) |

---

## Test data (preconditions)

Shared entity chain for API tests (create fresh data; do not reuse environment IDs).

1. Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE, customerIdentifier: auto-generated). Store `customerId`, `customerDetailId`, `customerVersionId`.
2. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activation date: on or before test anchor date). Store `podId`, `podDetailId`.
3. Create terms via `POST /terms` (type: PERIOD, value: 12, periodType: MONTH_MONTHS, status: ACTIVE).
4. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from env nomenclatures, status: ACTIVE).
5. Create a product via `POST /products` with `termId` from step 3, `priceComponentIds` from step 4, status: ACTIVE, `availableForSale: true`, `contractTypes: [SUPPLY_ONLY]`, global sales channel/area/segment: true.
6. Create a **product contract** via `POST /product-contract` linking customer (step 1), POD (step 2), product (step 5); header `contractStatus`: ACTIVE; version 1 `versionStatus`: **DRAFT**, `startDate`: anchor date − 30 days; record `productContractNumber`, `productContractId`, `productDetailIdV1Draft`, `versionId=1`.
7. Add **product contract version 2** via `PUT /product-contract/{id}?versionId=1` (or create-version flow per API): `versionStatus`: **SIGNED**, `startDate`: anchor date − 10 days; record `productDetailIdV2Signed`, `versionId=2`.
8. Optional: add **product contract version 3** with `versionStatus`: **DRAFT**, `startDate`: anchor date (newer than v2) — used to prove latest fallback ignores non-SIGNED chronology.
9. Create service (EPService) via service catalog API with ACTIVE status and active service version; store `serviceId`, `serviceVersionId`.
10. Create a **service contract** via `POST /service-contract` with customer from step 1; version 1 `contractVersionStatus`: **DRAFT**, `startDate`: anchor − 30 days; record `serviceContractNumber`, `serviceContractId`, `serviceDetailIdV1Draft`.
11. Finalize **service contract version 1 to SIGNED** via `PUT /service-contract/{id}?versionId=1` with `contractVersionStatus`: SIGNED, valid signing/entry dates; record `serviceDetailIdV1Signed`.
12. Create a penalty **action** template prerequisites: penalty definition with formula requiring contract variables and (for POD scope TC) formula type in **2, 4, 6, or 8**; store `penaltyId`, `actionTypeId` as required by `GET /actions/calculate-penalty-amount` request schema in `Cursor-Project/config/swagger/dev/swagger-spec.json`.
13. Link POD to **signed** product contract detail from step 7 via contract-POD activation on that detail; link a **second POD** only on a **future** product contract detail (step 8 or a later SIGNED version with later `startDate`) for TC-BE-7.

**Anchor date:** use a fixed `executionDate` / import “as of” date inside all version windows (e.g. today in Dev).

**API feasibility (Dev):** If `POST /product-contract` rejects version 1 with `versionStatus` DRAFT (`First version must be valid/signed`), create version 1 as **SIGNED**, then add later DRAFT versions (TC-BE-2/3/4) or cancel signed versions (TC-BE-4) per delta in each TC. Do not assume step 6 alone works on every environment without adjustment.

---

## Backend Test Cases

### TC-BE-1 (Positive): Mass Email contract import — explicit SIGNED product version is accepted

**Description:** Confirms TO-BE behavior for Story 1 when explicit version is eligible.

**Preconditions:**
1. Apply Test data steps 1–7 (product contract with version 2 SIGNED).
2. Delta: prepare Excel/import row: column A = `productContractNumber`, column B = `2` (explicit SIGNED version).

**Test steps:**
1. Call `POST /email-communication/customers-import` (multipart) with import file containing the row from preconditions and `massEmailCommunicationImportType` = contract-based import per Swagger.
2. Parse `MassCommunicationImportProcessResult` response body.
3. Verify processed rows list and error buffer.

**Expected test case results:** HTTP 200. Row is **accepted** (customer identifier present in processed results). No error containing `not in status Valid/Signed by both sides` for this row. Persisted link references contract detail for **version 2** (SIGNED), not version 1 DRAFT.

---

### TC-BE-2 (Negative): Mass Email contract import — explicit DRAFT version is rejected with documented message

**Description:** Proves invalid explicit version is not used for recipients (attachment Story 1 AC1).

**Preconditions:**
1. Apply Test data steps 1–6 (version 1 remains DRAFT; do not finalize v1 unless needed for contract existence).
2. Delta: import row: contract number from step 6, explicit version `1` (DRAFT).

**Test steps:**
1. Call `POST /email-communication/customers-import` with the DRAFT version row.
2. Read errors / report section in response.

**Expected test case results:** Row is **not** added to successful processed recipients. Error text includes fragment: `Contract with number {contractNumber} and version 1 doesn't exist or is not in status Valid/Signed by both sides` (per `EmailCommunicationService.processContractInfoVersionId`). HTTP 200 on import endpoint with business-level row failure (not silent success).

---

### TC-BE-3 (Positive): Mass Email contract import — latest fallback selects latest SIGNED, not newer DRAFT

**Description:** Validates chronology vs eligibility fix (attachment: latest **eligible** only).

**Preconditions:**
1. Apply Test data steps 1–8 (v2 SIGNED older startDate; v3 DRAFT newer startDate).
2. Delta: import row with contract number only (empty version column).

**Test steps:**
1. Call `POST /email-communication/customers-import` without version column.
2. Identify which `versionId` / contract detail was linked in processed result.

**Expected test case results:** System resolves **version 2** (SIGNED), **not** version 3 (DRAFT). No successful resolution using DRAFT solely because it has the maximum `startDate`.

---

### TC-BE-4 (Negative): Mass Email contract import — no SIGNED version yields clear failure

**Description:** Attachment AC: no eligible version → reject with reason.

**Preconditions:**
1. Apply Test data steps 1–6 only (single DRAFT version, never signed).
2. Delta: import row without version column.

**Test steps:**
1. Call `POST /email-communication/customers-import` for that contract number.

**Expected test case results:** Row fails. Error includes: `Contract with number {contractNumber} has no version in status Valid/Signed by both sides` (per `processContractInfoByLatestVersionId`). No customer added from that row.

---

### TC-BE-5 (Negative): Mass SMS contract parse — explicit non-SIGNED service version rejected

**Description:** Story 2 parity with Mass Email for service contracts.

**Preconditions:**
1. Apply Test data steps 1, 9–10 (service contract version 1 DRAFT).
2. Delta: SMS import file row: `serviceContractNumber`, explicit version `1`.

**Test steps:**
1. Call `POST /sms-communication/parse` (multipart) with contract import type per Swagger (`MassSMSImportType`).
2. Inspect parse result errors.

**Expected test case results:** Same rejection semantics as email: row not in successful list; error references not Valid/Signed by both sides (SmsCommunicationService mirrors email path). HTTP 200 with business error on row.

---

### TC-BE-6 (Positive): Mass SMS contract parse — latest SIGNED service version when version omitted

**Description:** Story 2 latest eligible fallback.

**Preconditions:**
1. Apply Test data steps 1, 9–11 (v1 SIGNED).
2. Add service v2 DRAFT with later `startDate` if API allows second version.
3. Delta: import row without version.

**Test steps:**
1. Call `POST /sms-communication/parse` without version.
2. Confirm resolved version in result.

**Expected test case results:** Latest **SIGNED** version selected; DRAFT with later start date ignored.

---

### TC-BE-7 (Negative): Penalty — POD linked only on future contract version is treated as valid today (description vs code gap)

**Description:** **Primary bug-hunting TC.** Attachment Story 3 requires PODs only from **respective** version for formulas 2/4/6/8; code still allows future versions.

**Preconditions:**
1. Apply Test data steps 1–7, 12–13: signed **respective** detail at execution date; action lists POD active only on a **later** contract detail (future `startDate`).
2. Delta: penalty formula type ∈ {2, 4, 6, 8}; `executionDate` falls in respective signed version window.

**Test steps:**
1. Call `GET /actions/calculate-penalty-amount` with query/body per Swagger (`CalculatePenaltyAmountRequest`): `contractId`, `executionDate`, `pods` = [future-only POD id], other required penalty fields populated.
2. Record `infoErrorMessages` / validation list and whether calculation proceeds.

**Expected test case results (Jira / attachment — TO-BE):** POD outside respective version is **excluded** or validation fails; penalty must not use future-version POD data.

**Actual result (current dev code — document during test):** `PointOfDeliveryRepository.findPodsBelongingToContractForAction` matches `pcd.id = respective OR pcd.start_date > respective.start_date` (`PointOfDeliveryRepository.java` ~236–243). `ActionValidatorService.validatePods` error text references **future version** acceptance (~416–447). If calculation succeeds without error, log as **confirmed defect** against PDT-2815.

---

### TC-BE-8 (Negative): Penalty — no SIGNED version covering execution date returns validation failure

**Description:** Story 3 AC: no eligible version → penalty not calculated.

**Preconditions:**
1. Apply Test data steps 1–6 with product contract where **only** DRAFT versions cover `executionDate`.
2. Delta: none.

**Test steps:**
1. Call `GET /actions/calculate-penalty-amount` with `executionDate` inside DRAFT-only window.
2. Capture error messages and HTTP status.

**Expected test case results:** No penalty amount calculated. Response contains validation/error indicating contract version for execution date not found or not signed (exact wording from `ActionValidatorService.getRespectiveContractVersion`). HTTP 4xx or 200 with empty amount + errors per API contract — verify against Swagger; must not silently use DRAFT detail.

---

### TC-BE-9 (Positive): Penalty — execution date resolves SIGNED product version and scopes variables

**Description:** Happy path for signed version resolution on product contract.

**Preconditions:**
1. Apply Test data steps 1–7 (v2 SIGNED active on execution date).
2. Delta: set known consumption/PAV/term fields on v2 distinct from v1 DRAFT.

**Test steps:**
1. Call `GET /actions/calculate-penalty-amount` with `executionDate` in v2 window, no conflicting POD scope issue.
2. Compare returned variable sources (contract detail id in debug/logs if available) to v2 field values.

**Expected test case results:** Calculation uses **version 2 signed** detail (`ProductContractDetailsRepository.getContractDetailIdByExecutionDate` filters `cd.status = 'SIGNED'`). Variable values match v2, not v1 DRAFT.

---

### TC-BE-10 (Positive): Penalty — zero price components for tag yields documented info error

**Description:** Regression — code already aligned with attachment message.

**Preconditions:**
1. Apply Test data steps 1–7, 12.
2. Delta: product version linked to product detail with **no** price component for required tag.

**Test steps:**
1. Call `GET /actions/calculate-penalty-amount` triggering tag-based component resolution.
2. Read `infoErrorMessages`.

**Expected test case results:** Message contains: `there is not valid price component for used price component tag in calculation` (per `PenaltyFormulaEvaluationService.java` ~461). Penalty amount empty or not calculated per business rules.

---

### TC-BE-11 (Negative): Penalty — duplicate price component tags for same tag

**Description:** Attachment duplicate rule.

**Preconditions:**
1. Apply Test data steps 1–7, 12.
2. Delta: two primitive price components with same tag on product detail tied to signed contract version.

**Test steps:**
1. Call `GET /actions/calculate-penalty-amount`.
2. Inspect `infoErrorMessages`.

**Expected test case results:** Message contains: `There is more then 1 price component for used price component tag in calculation`. Calculation does not proceed with arbitrary pick.

---

### TC-BE-12 (Positive): All-customers mass communication query — only SIGNED in-date versions

**Description:** Story 1/2 all-customers mode eligibility.

**Preconditions:**
1. Apply Test data steps 1–11: one customer with signed product + signed service versions active today; second customer with only DRAFT contract versions.
2. Delta: invoke mass email/SMS creation path that uses `fetchActiveContractsAndAssociatedCustomersForMassCommunication` (all customers with active contract mode).

**Test steps:**
1. Trigger mass communication recipient resolution for **all customers with active contract** (via `POST /email-communication/mass` or SMS equivalent with mode flag per UI/API).
2. Compare recipient contract detail IDs against DB: only details with `status`/`contract_version_status` = SIGNED and date window including today.

**Expected test case results:** Customer from step 2 (DRAFT-only) **excluded**. Customer from step 1 included with arrays `productContractDetailIds` / `serviceContractDetailIds` only for SIGNED rows (`CustomerRepository.java` ~1120–1150).

---

### TC-BE-13 (Negative): Cross-process inconsistency — Mass Email resolves v2 SIGNED while Penalty on same date uses different detail if windows overlap

**Description:** Jira ER bullet: all three processes consistent for same contract/date input.

**Preconditions:**
1. Apply Test data steps 1–8 with overlapping signed windows and distinct data per version.
2. Delta: same `executionDate` / import date.

**Test steps:**
1. Import contract via Mass Email without version → record resolved `versionId`.
2. Call penalty calculate for same `contractId` and date.
3. Compare resolved contract detail IDs.

**Expected test case results:** **Same** contract detail version ID used in both processes. If IDs differ, file defect: cross-process consistency broken.

---

### TC-BE-14 (Negative): Gap probe — version SIGNED but contract header not Signed by both sides still passes mass import

**Description:** Tests terminology gap: attachment says “Valid/Signed by both sides”; mass comm filters version `SIGNED` only, not header `sub_status = SIGNED_BY_BOTH_SIDES`.

**Preconditions:**
1. Apply Test data steps 1–7.
2. Delta: data setup (if possible) where `contract_details.status/versionStatus = SIGNED` but contract header `contract_status` / sub-status is **not** `SIGNED_BY_BOTH_SIDES` (requires DB/API state — skip with note if not constructible).

**Test steps:**
1. Attempt Mass Email explicit import for that version.
2. Document whether import accepts row.

**Expected test case results (Jira strict reading):** Row should be **rejected** if header not signed by both sides.

**Actual result (code expectation):** Import may **accept** if only version-level `SIGNED` is checked — documents potential **missing** header-level validation for QA/Dev discussion.

---

### TC-BE-15 (Negative): Penalty — service contract with no SIGNED version on execution date is rejected

**Description:** Story 3 AC parity for **service contracts**: penalty must not calculate when no version is Valid/Signed for the execution date. Mirrors TC-BE-8 on product but uses `SERVICE_CONTRACT` and service-contract setup.

**Preconditions:**
1. Apply Test data steps 1, 9–10 (service contract version 1 **DRAFT** only; do **not** apply step 11).
2. Apply Test data step 12 (penalty definition and `actionTypeId` usable with service contract).
3. Delta: `executionDate` falls inside the DRAFT v1 `startDate` window; no other SIGNED service version covers that date.

**Test steps:**
1. Call `GET /actions/calculate-penalty-amount` with `CalculatePenaltyAmountRequest`: `contractType` = `SERVICE_CONTRACT`, `contractId` = service contract from step 10, `executionDate` = anchor date, required penalty fields (`penaltyId`, `actionTypeId`, `penaltyPayer`, `customerId`, `terminationId` if required by schema).
2. Capture HTTP status, response body (`infoErrorMessages`, `amount`, `empty` / `notEmpty` flags per Swagger).
3. Optionally call `ServiceContractDetailsRepository.getContractDetailIdByExecutionDate` equivalent via DB read: confirm no row when only DRAFT versions exist (read-only verification).

**Expected test case results:** No penalty amount calculated (`amount` empty/null or `notEmpty` false per API contract). Response includes validation referencing missing respective version, e.g. fragment `Respective contract version not found` (from `ActionValidatorService.getRespectiveContractVersion` when repository returns null). HTTP status per Swagger for validation failure (often 200 with errors or 4xx — document actual; must **not** silently use DRAFT detail). No successful calculation scoped to DRAFT `serviceDetailIdV1Draft`.

---

### TC-BE-16 (Positive): Penalty — recalculation with unchanged execution date keeps original signed version context

**Description:** Jira ER + Story 3 AC: historical actions stay tied to the version valid at **original** `executionDate`; adding a newer signed version must **not** change penalty context on recalculate/reprint unless `executionDate` changes.

**Preconditions:**
1. Apply Test data steps 1–7, 12 (product contract: v2 SIGNED covers anchor `executionDate`; POD linked on v2 detail).
2. Delta: record `signedV2DetailId`, `executionDate` = anchor inside v2 window.
3. Delta: penalty formula does **not** require POD scope types 2/4/6/8 (avoid TC-BE-7 POD noise) OR use POD on v2 only.

**Test steps:**
1. Call `GET /actions/calculate-penalty-amount` with `contractType` = `PRODUCT_CONTRACT`, `contractId`, `executionDate` = anchor, populated penalty request → store `amountFirst` and `infoErrorMessagesFirst`.
2. Add **product contract version 3** via version-creation flow: `versionStatus` = **SIGNED**, `startDate` = anchor + 30 days (strictly **after** anchor so v3 is not the respective version for step 1 date). Record `signedV3DetailId`.
3. Call `GET /actions/calculate-penalty-amount` again with the **same** `executionDate` and same `contractId` → store `amountSecond`.
4. Delta: call `GET /actions/calculate-penalty-amount` with `executionDate` moved into version 3 window only (between v3 `startDate` and its end/open end) → store `amountThird`.

**Expected test case results:**
- Steps 1 vs 3: `amountSecond` equals `amountFirst` (or both empty with same error set) — newer v3 must **not** alter calculation for the original execution date.
- Step 4: `amountThird` may differ from `amountFirst` (version context changed when execution date enters v3 window) — proves resolution is date-driven, not “latest signed always”.
- Document resolved detail context in test notes if API exposes `contractDetailId` in response/logs; expected respective detail for steps 1–3 = v2 (`signedV2DetailId`), for step 4 = v3 when date in v3 window.

---

### TC-BE-17 (Negative): All-customers mass communication — service contract DRAFT-only customer exclusion (gap probe)

**Description:** Story 2 AC: all-customers mode must include only customers tied to **eligible** (Valid/Signed) versions for **both** product and service. Cross-dep flagged service CTE may not filter `contract_version_status` like product (`CustomerRepository.fetchActiveContractsAndAssociatedCustomersForMassCommunication`).

**Preconditions:**
1. Apply Test data steps 1–6 for **Customer A** (product contract; finalize at least one SIGNED product version per API feasibility note).
2. Apply Test data steps 9–10 for **Customer A** (service contract v1 DRAFT only — skip step 11).
3. Create **Customer B** via `POST /customer` (ACTIVE); apply steps 9–11 for Customer B so service v1 is **SIGNED** and active today.
4. Delta: prepare to trigger all-customers recipient resolution (`POST /email-communication/mass` or `POST /sms-communication/mass` with “all customers with active contract” mode per Swagger/UI parity).

**Test steps:**
1. Trigger mass communication recipient resolution for **all customers with active contract** (email or SMS endpoint per test focus).
2. Inspect response/recipient payload for Customer A and Customer B (`customerIdentifier` or internal id).
3. Read-only DB check (optional): `serviceContractDetailIds` / `productContractDetailIds` arrays returned for each customer — compare to `service_contract.contract_details.contract_version_status` (or `status`) for linked rows.

**Expected test case results (Jira TO-BE):** Customer A (**DRAFT-only** service version) is **excluded** from recipient list or has **empty** `serviceContractDetailIds`. Customer B (SIGNED service version) is **included** with only SIGNED detail ids. Product side for Customer A follows same eligibility if product versions are also non-SIGNED.

**Actual result (gap probe):** If Customer A appears with a DRAFT `serviceDetailId` in `serviceContractDetailIds`, log **confirmed defect** against Story 2 / `CustomerRepository` service CTE. HTTP 200 on mass endpoint with business filtering is acceptable.

---

## References

- Jira: PDT-2815 — `Cursor-Project/config/jira/attachments/PDT-2815/issue-rest.json`
- Attachment: `Using validity in 3 processes.docx` / `.extracted.txt`
- Linked: PDT-2599
- Code: `EmailCommunicationService.java` (import ~3259–3335), `ProductContractRepository.java` (~1375–1400), `ServiceContractsRepository.java` (~808–838), `CustomerRepository.java` (~1120–1160), `PointOfDeliveryRepository.java` (~228–253), `ActionValidatorService.java` (~93–118, ~416–447), `ServiceContractDetailsRepository.java` (~142–161, ~197–228), `ProductContractDetailsRepository.java` (~203–222), `PenaltyFormulaEvaluationService.java` (~454–464)
- Cross-deps: `Cursor-Project/cross_dependencies/2026-05-20_PDT-2815_version_validity_three_processes.json`
- Swagger (Dev): `Cursor-Project/config/swagger/dev/swagger-spec.json` — `/email-communication/customers-import`, `/sms-communication/parse`, `/actions/calculate-penalty-amount`
