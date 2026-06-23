# Request for Disconnection – Supplier Type POD Filtering (PDT-2957)

**Jira:** [PDT-2957](https://oppa-support.atlassian.net/browse/PDT-2957) (PDT)  
**Type:** Task  
**Summary:** POD list for Request for Disconnection must respect `supplierType`: `CURRENT` = POD active on current date in reminder customer's product contract; `PREVIOUS` = POD not active for that customer. Existing EXECUTED requests stay unchanged.

**Scope:** Backend APIs on Dev. Primary endpoint: `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply`.

---

## Test data (preconditions)

Reference only — **each TC below repeats its own full numbered chain** (Rule TC-STANDALONE-PRE.0).

- **Environment:** Dev.
- **Active contract-POD (runtime):** `product_contract.contract_pods.status=ACTIVE`, `activation_date <= today`, `deactivation_date IS NULL OR >= today`, contract detail/contract headers not draft/cancelled, `contracts.status=ACTIVE`, `contract_status NOT IN (DRAFT, READY, CANCELLED)`, customer matches reminder row.
- **Response DTO fields (Swagger/code):** `CustomersForDPSResponse`: `podId`, `podIdentifier`, `customers`, `customerId`, `liabilitiesInPod`, `invoiceNumber`, `isChecked`.

---

## Backend Test Cases

### TC-BE-1 (Positive): CURRENT lists POD when reminder customer has active contract-POD today

**Description:** Happy path — `supplierType=CURRENT` returns the POD/customer row from the reminder when contract-POD is active on current date.

**Preconditions:**
1. Create terms via `POST /terms` body `{ "type": "PERIOD", "value": 100, "periodType": "DAY_DAYS" }`; store `termId`.
2. Create price component via `POST /price-components` (type `ELECTRICITY`, valid `vatRateId` from Dev env); store `priceComponentId`.
3. Create product via `POST /products` with `termId`, `priceComponentIds: [priceComponentId]`, `status: ACTIVE`, sale availability flags enabled; store `productId`.
4. Create customer via `POST /customer` (`customerType: PRIVATE`, status ACTIVE); store `customerId`, note `customerIdentifier` from response.
5. Create POD via `POST /pod` (electricity, status ACTIVE, `gridOperatorId` from Dev env, activation date ≤ today); store `podId`, `podIdentifier`.
6. Create product contract via `POST /product-contract` linking customer step 4, POD step 5, product step 3; contract-POD: `activationDate` ≤ today, `deactivationDate: null`, contract status ACTIVE; store `contractId`.
7. Create billing run via `POST /billing-run` for `contractId` (type STANDARD, period ending ≥30 days ago); execute run to completion; store `invoiceId` from run output.
8. Call `POST /invoice/listing` filtered by `invoiceId`; confirm `currentAmount > 0`; note linked liability `liabilityNumber` with `dueDate <= today`.
9. Create reminder via `POST /power-supply-disconnection-reminder` including the overdue liability, `liabilitiesMaxDueDate >= liability dueDate`; store `reminderId`, `{gridOperatorId}`.

**Test steps:**
1. Send `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS`.
2. In response JSON `content`, find element where `podId={podId}` and `customers` contains `customerIdentifier`.

**Expected test case results:**
- HTTP **206 Partial Content**.
- Matching row exists with `podIdentifier={podIdentifier}`, `podId={podId}`, non-null `liabilitiesInPod` containing `liabilityNumber` from step 8.

---

### TC-BE-2 (Positive): PREVIOUS lists POD when reminder customer has no active contract-POD today

**Description:** `supplierType=PREVIOUS` returns POD when contract-POD is deactivated (yesterday) for reminder customer.

**Preconditions:**
1. Create terms via `POST /terms` (`PERIOD`, 100, `DAY_DAYS`); store `termId`.
2. Create price component via `POST /price-components` (ELECTRICITY + env `vatRateId`); store `priceComponentId`.
3. Create product via `POST /products` (`termId`, `[priceComponentId]`, ACTIVE); store `productId`.
4. Create customer via `POST /customer` (PRIVATE, ACTIVE); store `customerId`, `customerIdentifier`.
5. Create POD via `POST /pod` (ACTIVE, env `gridOperatorId`); store `podId`, `podIdentifier`.
6. Create product contract via `POST /product-contract` for customer + POD + product; set contract-POD **`deactivationDate` = yesterday**, `activationDate` ≤ yesterday; store `contractId`.
7. Create and complete billing run via `POST /billing-run` for `contractId`; obtain overdue liability (`dueDate <= today`, `currentAmount > 0`); store `liabilityNumber`.
8. Create reminder via `POST /power-supply-disconnection-reminder` for that liability; store `reminderId`, `{gridOperatorId}`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=PREVIOUS&conditionType=ALL_CUSTOMERS`.
2. Locate row with `podId={podId}` and `customers` containing `customerIdentifier`.
3. Repeat step 1 with `supplierType=CURRENT`.

**Expected test case results:**
- Step 1: HTTP **206**; row present with non-null `liabilitiesInPod`.
- Step 3: HTTP **206**; **no** row with `podId={podId}` for that customer.

---

### TC-BE-3 (Negative): CURRENT excludes POD when contract-POD deactivated for reminder customer

**Description:** Filter negative — inactive contract-POD must not appear under CURRENT.

**Preconditions:**
1. Create terms via `POST /terms` (`PERIOD`, 100, `DAY_DAYS`); store `termId`.
2. Create price component via `POST /price-components` (ELECTRICITY); store `priceComponentId`.
3. Create product via `POST /products` (`termId`, `[priceComponentId]`, ACTIVE); store `productId`.
4. Create customer via `POST /customer` (PRIVATE, ACTIVE); store `customerId`, `customerIdentifier`.
5. Create POD via `POST /pod` (ACTIVE, env `gridOperatorId`); store `podId`, `podIdentifier`.
6. Create product contract with contract-POD **`deactivationDate=yesterday`** (inactive today); store `contractId`.
7. Billing run + overdue liability (`dueDate <= today`); store `liabilityNumber`.
8. Create reminder via `POST /power-supply-disconnection-reminder`; store `reminderId`, `{gridOperatorId}`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS`.
2. Count rows where `podId={podId}` AND `customerId={customerId}`.

**Expected test case results:**
- HTTP **206 Partial Content**.
- Matching row count = **0** (no row in `content` with `podId={podId}` and `customerId={customerId}`).
- No `POST /disconnection-of-power-supply-requests` side effect from this read.

---

### TC-BE-4 (Negative): PREVIOUS excludes POD when contract-POD active for reminder customer

**Description:** Active contract-POD today must not appear under PREVIOUS.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customer via `POST /customer` (PRIVATE, ACTIVE); store `customerId`, `customerIdentifier`.
5. Create POD via `POST /pod` (ACTIVE, env `gridOperatorId`); store `podId`, `podIdentifier`.
6. Create product contract with contract-POD **active today** (`activationDate <= today`, `deactivationDate: null`); store `contractId`.
7. Billing run + overdue liability; store `liabilityNumber`.
8. Create reminder; store `reminderId`, `{gridOperatorId}`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=PREVIOUS&conditionType=ALL_CUSTOMERS`.
2. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS`.

**Expected test case results:**
- PREVIOUS call: HTTP **206**; zero rows with `podId={podId}`.
- CURRENT call: HTTP **206**; row with `podId={podId}` present.

---

### TC-BE-5 (Positive): Shared POD — CURRENT/PREVIOUS evaluated per reminder customer

**Description:** Cross-dep risk [PDT-2971](https://oppa-support.atlassian.net/browse/PDT-2971) — one POD, two reminder customers, different contract-POD activity.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create **customerA** and **customerB** via `POST /customer` (PRIVATE, ACTIVE); store IDs and identifiers.
5. Create **sharedPod** via `POST /pod` (ACTIVE, env `gridOperatorId`); store `sharedPodId`, `sharedPodIdentifier`.
6. Create contract **PC-A**: customerA + sharedPod, contract-POD active today.
7. Create contract **PC-B**: customerB + sharedPod, contract-POD **`deactivationDate=yesterday`**.
8. Billing run + overdue liability for customerA on sharedPod; store `liabilityA`.
9. Billing run + overdue liability for customerB on sharedPod; store `liabilityB`.
10. Create reminder via `POST /power-supply-disconnection-reminder` containing **both** liabilities; store `reminderId`, `{gridOperatorId}`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS`.
2. `GET` same with `supplierType=PREVIOUS`.

**Expected test case results:**
- CURRENT: row (`podId=sharedPodId`, customerA identifier in `customers`); **no** row for customerB on sharedPod.
- PREVIOUS: row for customerB on sharedPod; **no** row for customerA on sharedPod.

---

### TC-BE-6 (Positive): Future contract-POD activation — PREVIOUS until activation date

**Description:** `activationDate=tomorrow` → not active today.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customer via `POST /customer`; store `customerId`, `customerIdentifier`.
5. Create POD via `POST /pod` (env `gridOperatorId`); store `podId`, `podIdentifier`.
6. Create product contract with contract-POD **`activationDate=tomorrow`**, `deactivationDate=null`; store `contractId`.
7. Billing run via `POST /billing-run` + overdue liability; store `liabilityNumber`.
8. Create reminder via `POST /power-supply-disconnection-reminder`; store `reminderId`, `{gridOperatorId}`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS`.
2. `GET` same URL with `supplierType=PREVIOUS`.

**Expected test case results:**
- CURRENT: HTTP **206**; no row with `podId={podId}`.
- PREVIOUS: HTTP **206**; row with `podId={podId}` and `liabilitiesInPod` non-null.

---

### TC-BE-7 (Positive): Past contract-POD deactivation — PREVIOUS after deactivation

**Description:** `deactivationDate=yesterday` excludes POD from CURRENT.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customer via `POST /customer`; store `customerId`, `customerIdentifier`.
5. Create POD via `POST /pod` (env `gridOperatorId`); store `podId`.
6. Create contract with contract-POD **`deactivationDate=yesterday`**; store `contractId`.
7. Billing run + overdue liability; store `reminderId` via `POST /power-supply-disconnection-reminder`; store `{gridOperatorId}`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS`.
2. `GET` same with `supplierType=PREVIOUS`.

**Expected test case results:**
- CURRENT: no matching `podId`.
- PREVIOUS: matching row present.

---

### TC-BE-8 (Positive): POST create DRAFT with allSelected=true and CURRENT checks only CURRENT-eligible PODs

**Description:** Create path applies supplier filter when selecting all PODs.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customerA and customerB via `POST /customer` (PRIVATE, ACTIVE).
5. Create sharedPod via `POST /pod` (env `gridOperatorId`).
6. Contract PC-A active today for customerA on sharedPod; contract PC-B deactivated yesterday for customerB on sharedPod.
7. Two billing runs + overdue liabilities for both customers.
8. Create reminder via `POST /power-supply-disconnection-reminder` with both liabilities; store `reminderId`, `{gridOperatorId}`.
9. Resolve `reasonOfDisconnectionId` from nomenclature; store value.
10. Resolve disconnection request document `templateId` from env/nomenclature.

**Test steps:**
1. `POST /disconnection-of-power-supply-requests` body: `{ "supplierType": "CURRENT", "disconnectionRequestsStatus": "DRAFT", "reminderForDisconnectionId": {reminderId}, "gridOperatorId": {gridOperatorId}, "reasonOfDisconnectionId": {reasonOfDisconnectionId}, "gridOpRequestRegDate": "2026-06-19", "conditionType": "ALL_CUSTOMERS", "allSelected": true, "podWithHighestConsumption": false, "templateIds": [{templateId}], "pods": [], "excludePodIds": [], "files": [] }`.
2. `GET /disconnection-of-power-supply-requests/get-checked-pods/{reminderId}/{gridOperatorId}/{requestId}` from step 1 response.
3. `GET /disconnection-of-power-supply-requests/{requestId}`.

**Expected test case results:**
- POST: HTTP **200**, body numeric `requestId`.
- GET request: `supplierType` field = `CURRENT`.
- Checked pods: contains customerA/sharedPod (`isChecked=true`); **excludes** customerB/sharedPod.

---

### TC-BE-9 (Positive): POST create DRAFT with allSelected=true and PREVIOUS checks only PREVIOUS-eligible PODs

**Description:** Mirror create path for PREVIOUS.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customerA + customerB via `POST /customer`.
5. Create sharedPod via `POST /pod`.
6. Contract PC-A active today on sharedPod for customerA; contract PC-B deactivated yesterday for customerB.
7. Two overdue liabilities + reminder (both customers); store `reminderId`, `{gridOperatorId}`, `reasonOfDisconnectionId`, `templateId`.

**Test steps:**
1. `POST /disconnection-of-power-supply-requests` with `"supplierType": "PREVIOUS"`, `"allSelected": true`, same required fields as TC-BE-8 step 1.
2. `GET /disconnection-of-power-supply-requests/get-checked-pods/{reminderId}/{gridOperatorId}/{requestId}`.

**Expected test case results:**
- POST HTTP **200**.
- Checked pods list includes customerB/sharedPod only; customerA/sharedPod absent.

---

### TC-BE-10 (Positive): EXECUTED request view-pod-tab returns stored results (regression)

**Description:** Ticket rule — existing EXECUTED requests untouched; pod tab reads stored results, not re-filtered SQL.

**Preconditions:**
1. `POST /disconnection-of-power-supply-requests/filter` body `{ "page": 0, "size": 1, "disconnectionRequestsStatuses": ["EXECUTED"] }`; store `legacyRequestId`, `legacyReminderId`, `legacyGridOperatorId`, `legacySupplierType` from first hit.
2. `GET /disconnection-of-power-supply-requests/get-checked-pods/{legacyReminderId}/{legacyGridOperatorId}/{legacyRequestId}`; record baseline array `legacyPodIds[]` from each item's `podId`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/view-pod-tab/{legacyRequestId}?page=0&size=100&conditionType=ALL_CUSTOMERS&supplierType=PREVIOUS` (intentionally mismatched query param).
2. Collect `podId` values from response `content`.

**Expected test case results:**
- HTTP **206 Partial Content**.
- Set of `podId` in step 2 **equals** `legacyPodIds[]` from precondition step 2 (no pods dropped/added vs baseline).
- `GET /disconnection-of-power-supply-requests/{legacyRequestId}` still shows `disconnectionRequestsStatus=EXECUTED`.

---

### TC-BE-11 (Negative): Omit supplierType on load-customer — validation error

**Description:** Mandatory field validation on POD load API.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customer via `POST /customer` (PRIVATE, ACTIVE).
5. Create POD via `POST /pod` (env `gridOperatorId`).
6. Create active product contract linking customer, POD, product.
7. Billing run via `POST /billing-run` + overdue liability (`dueDate <= today`).
8. Create reminder via `POST /power-supply-disconnection-reminder`; store `reminderId`, `{gridOperatorId}`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=10&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&conditionType=ALL_CUSTOMERS` (**no** `supplierType` param).

**Expected test case results:**
- HTTP **400 Bad Request**.
- Response contains validation message including **`supplierType-supplierType must not be null`** (service validation in `DisconnectionPowerSupplyRequestsService.loadCustomerForDisconnectionPowerSupply`).

---

### TC-BE-12 (Positive): DRAFT view-pod-tab uses entity supplierType, not query override

**Description:** `viewPodTab` for DRAFT overwrites request param with stored supplier type.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customerA and customerB via `POST /customer` (PRIVATE, ACTIVE).
5. Create sharedPod via `POST /pod` (env `gridOperatorId`).
6. Contract PC-A active today for customerA on sharedPod; contract PC-B deactivated yesterday for customerB on sharedPod.
7. Two billing runs + overdue liabilities; reminder with both liabilities; store `reminderId`, `{gridOperatorId}`, `reasonOfDisconnectionId`, `templateId`.
8. `POST /disconnection-of-power-supply-requests` with `"supplierType":"CURRENT"`, `"disconnectionRequestsStatus":"DRAFT"`, `"allSelected":true`, `"reminderForDisconnectionId":{reminderId}`, `"gridOperatorId":{gridOperatorId}`, `"reasonOfDisconnectionId":{reasonOfDisconnectionId}`, `"gridOpRequestRegDate":"2026-06-19"`, `"conditionType":"ALL_CUSTOMERS"`, `"templateIds":[{templateId}]`, `"pods":[]`, `"excludePodIds":[]`, `"files":[]`; store `draftRequestId`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/view-pod-tab/{draftRequestId}?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=PREVIOUS&conditionType=ALL_CUSTOMERS`.
2. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS`.

**Expected test case results:**
- Both calls HTTP **206**.
- Pod/customer pairs in step 1 **match** step 2 (CURRENT filter), proving stored CURRENT wins over `supplierType=PREVIOUS` query param.

---

### TC-BE-13 (Positive): isHighestConsumption=true still applies supplierType filter

**Description:** Cross-dep risk — highest-consumption flag must not bypass supplier filter.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customerA and customerB via `POST /customer` (PRIVATE, ACTIVE).
5. Create sharedPod via `POST /pod` (env `gridOperatorId`).
6. Contract PC-A: customerA + sharedPod, contract-POD active today.
7. Contract PC-B: customerB + sharedPod, contract-POD `deactivationDate=yesterday`.
8. Two billing runs + overdue liabilities; reminder with both via `POST /power-supply-disconnection-reminder`; store `reminderId`, `{gridOperatorId}`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS&isHighestConsumption=true`.
2. `GET` same URL with `supplierType=PREVIOUS&isHighestConsumption=true`.

**Expected test case results:**
- CURRENT: only customerA/sharedPod rows (if any); zero customerB/sharedPod rows.
- PREVIOUS: only customerB/sharedPod rows; zero overlap with step 1 `podId`+`customerId` pairs.

---

### TC-BE-14 (Positive): TERMINATED contract_status with active contract-POD dates — CURRENT lists (runtime)

**Description:** Validates runtime SQL allowing TERMINATED contract header when contract-POD dates active.

**Preconditions:**
1. Create terms via `POST /terms`; store `termId`.
2. Create price component via `POST /price-components`; store `priceComponentId`.
3. Create product via `POST /products`; store `productId`.
4. Create customer via `POST /customer`; store `customerId`, `customerIdentifier`.
5. Create POD via `POST /pod` (env `gridOperatorId`); store `podId`.
6. Create product contract with **`contractStatus: TERMINATED`** but contract-POD active today (`activationDate <= today`, `deactivationDate null`); store `contractId`.
7. Billing run via `POST /billing-run` + overdue liability; create reminder via `POST /power-supply-disconnection-reminder`; store `reminderId`, `{gridOperatorId}`, `podId`.

**Test steps:**
1. `GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply?page=0&size=50&powerSupplyDisconnectionReminderId={reminderId}&gridOperatorId={gridOperatorId}&supplierType=CURRENT&conditionType=ALL_CUSTOMERS`.
2. `GET` same with `supplierType=PREVIOUS`.

**Expected test case results:**
- CURRENT: row with `podId={podId}`.
- PREVIOUS: no row with `podId={podId}`.

---

## References

| Source | Detail |
|--------|--------|
| [PDT-2957](https://oppa-support.atlassian.net/browse/PDT-2957) | Supplier type rules; keep existing requests untouched |
| [Request for disconnection – Create (72155868)](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72155868/Request+for+disconnection+of+the+power+supply+-+Create) | Create flow, supplier type field |
| `DisconnectionPowerSupplyRequestRepository.java:1964-2007` | CURRENT EXISTS / PREVIOUS NOT EXISTS |
| `DisconnectionPowerSupplyRequestsService.java:390-430` | EXECUTED uses results repo, not live filter |
| `CustomersForDPSResponse.java` | Response field names |

**Jira source:** REST fallback. **Confluence source:** REST fallback (page 72155868).

### Finding: TERMINATED contract_status with active contract-POD still CURRENT-eligible
- **Type:** Doc gap
- **User impact:** Medium
- **Spec / doc says:** PDT-2957 — “active in current date in the contract of the customer”
- **Code / runtime does:** `contract_status NOT IN ('DRAFT','READY','CANCELLED')` allows TERMINATED when contract-POD dates active
- **Gap:** Confluence silent on TERMINATED edge case
- **Recommendation:** PO clarification / Confluence update; TC-BE-14 validates runtime
