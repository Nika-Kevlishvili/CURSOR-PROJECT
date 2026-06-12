# PUT: Update Existing Contract — Sales Portal API (PHN-2130)

**Jira:** PHN-2130 (Phoenix)  
**Type:** Story  
**Summary:** Validates the Sales Portal `PUT /sales-portal/contract` endpoint for updating existing product contracts — field omission semantics, sub-object proxy/manager rules, customer in-place vs swap, direct debit toggling, KYC checkbox rules, and validation error handling.

**Scope:** Backend API test cases for the planned `PUT /sales-portal/contract` endpoint. **Default customer types:** `PRIVATE_CUSTOMER` and `PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY` (where managers apply); `LEGAL_ENTITY` only in TC-BE-22 (multi-proxy). Covers: happy-path full update, direct debit enable/disable/leave-as-is, proxy create vs leave-as-is, manager exception (email/phone not removed), customer in-place update vs customer swap, **adding/changing to a different product on the contract** (allowed), **third-tab contract product details** via `productParameters` (allowed), price-component value-filled restriction, **fixed interim advance payment accepted** (positive), **adding a second POD to a contract** that already has one, **customer local address update** (positive) and **foreign address missing mandatory fields** (negative), **product swap with mismatched product version** (negative), **contract status transitions** (`ENTERED_INTO_FORCE`, `ACTIVE_IN_TERM`, `TERMINATED` and status-mandatory-field validation), KYC boolean rules, and validation error scenarios.

**Product editing rule (PHN-2130 test scope):**
- **Allowed:** User may **add or change to a different product** on the contract (send a different `productId` / matching `productVersionId` — contract product swap). See TC-BE-23.
- **Allowed:** User may edit **product details on the contract** (third tab) via `productParameters` — not the product master/catalog object. See TC-BE-25, TC-BE-26.
- **Forbidden:** Product with a price component that has **no value filled** — update rejected. See TC-BE-24.

**Note:** As of the test-case authoring date the `PUT /sales-portal/contract` endpoint is planned but not yet deployed on dev2. Confluence page 895254530 (v27): *“If different product is sent, we are changing product in contract.”* Third-tab discovery uses `GET /product-contract/third-tab-fields?productDetailId=...`. All Sales Portal calls use the `SPRequest` fixture (OAuth2 client credentials token).

---

## Test data (preconditions)

Shared creation-order reference — used (repeated verbatim) in every TC per TC-STANDALONE-PRE.0:

- **Environment:** dev2
- **Auth:** `SPRequest` fixture uses `salesPortalTokenAuth()` (OAuth2 client credentials); standard `Request` fixture used for setup calls.
- **Dependency order:** Terms → Price component → Product → Customer → POD → Communication data × 2 → Product contract.
- **Customer types:** Use `PRIVATE_CUSTOMER` (no business activity) unless the case needs a **manager** or **private-with-business proxy rules** — then use `PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY` (`GeneratePayload.customers.customer_private_business()`). **TC-BE-22** keeps `LEGAL_ENTITY` for multi-proxy. Payloads align with PHN-2208 / express-contract private customer shapes.

---

## Backend Test Cases

### TC-BE-1 (Positive): Full happy-path contract update with all mandatory fields

**Description:** Verify that `PUT /sales-portal/contract` returns HTTP 200 and `{ "status": "success" }` when all mandatory contract fields are provided, and that the updated value (estimatedTotalConsumption) is persisted.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no interim advance payment configured; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customerId from step 4, podIdentifier from step 5, productId from step 3, productVersionId from step 3; status: SIGNED, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: 2025-01-01, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with the following body (all mandatory fields):
   `{ "contractId": <contractId>, "contractVersionId": <contractVersionId>, "contractStatus": "SIGNED", "contractSubStatus": "IN_PROCESS", "signingDate": "2025-01-01", "estimatedTotalConsumption": 5000, "productId": <productId>, "productVersionId": <productVersionId>, "contractType": "SUPPLY_ONLY", "contractTerm": "DEFINED_TERM", "invoicePaymentTerm": 30, "paymentGuarantee": "NO", "cashDepositAmount": 0, "cashDepositCurrencyId": <currencyId from envVariables>, "bankGuaranteeAmount": 0, "bankGuaranteesCurrencyId": <currencyId from envVariables>, "enteringIntoForceDate": "2025-01-01", "startOfInitialTerm": "2025-01-01", "supplyActivationAfterResigning": false, "waitForOldContractTermToExpire": false, "customerUic": <customerUic>, "communicationDataForContractId": <commContractId>, "communicationDataForBillingId": <commBillingId>, "podIdentifiers": [<podIdentifier>] }`.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the returned `estimatedTotalConsumption` field.

**Expected test case results:** Step 1 returns HTTP 200 with body `{ "status": "success" }`. Step 3 returns `estimatedTotalConsumption: 5000` (updated from the original 1000), confirming the change was persisted.

---

### TC-BE-2 (Positive): Enable direct debit — directDebit=true with bankId, IBAN, and BIC provided

**Description:** Verify that sending `directDebit: true` together with bankId, IBAN, and BIC enables direct debit on the contract and persists all three bank fields.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Retrieve a valid bank ID via `GET /nomenclatures/banks` or from envVariables. Save **bankId**.
9. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; directDebit: false; all other mandatory fields as in TC-BE-1 step 8). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (contractId, contractVersionId, status, signingDate, productId, productVersionId, contractType, contractTerm, invoicePaymentTerm, paymentGuarantee, cashDepositAmount, cashDepositCurrencyId, bankGuaranteeAmount, bankGuaranteesCurrencyId, enteringIntoForceDate, startOfInitialTerm, supplyActivationAfterResigning, waitForOldContractTermToExpire, customerUic, communicationDataForContractId, communicationDataForBillingId, podIdentifiers) and additionally: `"directDebit": true, "bankId": <bankId from step 8>, "iban": "BG80BNBG96611020345678", "bic": "BNBGBGSD"`.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `directDebit`, `bankId`, `iban`, and `bic` fields.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `directDebit: true`, `bankId` matching step 8, `iban: "BG80BNBG96611020345678"`, `bic: "BNBGBGSD"`.

---

### TC-BE-3 (Positive): Disable direct debit — directDebit=false unchecks checkbox only

**Description:** Verify that sending `directDebit: false` on a contract that has direct debit enabled unchecks the direct debit checkbox per Confluence checkbox semantics. Omitted `bankId`, `iban`, and `bic` are left unchanged (leave-as-is); bank details are not cleared solely because direct debit is disabled.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Retrieve a valid bank ID from envVariables. Save **bankId**.
9. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; directDebit: true, bankId from step 8, iban: "BG80BNBG96611020345678", bic: "BNBGBGSD"; all other mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields and `"directDebit": false`. Omit `bankId`, `iban`, and `bic` from the request body.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `directDebit`, `bankId`, `iban`, and `bic`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `directDebit: false`; `bankId`, `iban`, and `bic` remain unchanged from the precondition (`bankId` from step 8, `iban: "BG80BNBG96611020345678"`, `bic: "BNBGBGSD"`).

---

### TC-BE-4 (Positive): Direct debit field omitted — existing direct debit setting left unchanged

**Description:** Verify the leave-as-is rule for the `directDebit` checkbox: omitting the field from the PUT request leaves the current direct debit value and bank fields unchanged.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Retrieve a valid bank ID from envVariables. Save **bankId**.
9. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; directDebit: true, bankId from step 8, iban: "BG80BNBG96611020345678", bic: "BNBGBGSD"; all other mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields but **omit the `directDebit` field entirely** from the request body (do not include it as null or false — simply absent).
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `directDebit`, `iban`, and `bic`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `directDebit: true`, `iban: "BG80BNBG96611020345678"`, `bic: "BNBGBGSD"` — all unchanged from before the update (leave-as-is rule applied to checkbox field).

---

### TC-BE-5 (Positive): Same customer sent — customer version updated in place

**Description:** Verify that when the customer UIC already linked to the contract is re-sent in the PUT request, the system updates that customer's version in place (no new customer created, no customer swap).

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields and `"customerUic": <customerUic from step 4>` (the same customer already linked), and update the customer name field: `"name": "Ivan Updated"`, `"nameTransliterated": "Ivan Updated"` (with other mandatory private-customer fields from envVariables / `GeneratePayload.customers.customer_private()`).
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the customerUic field.
4. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and record the customer `name` field.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows the same customerUic from step 4 (no customer swap occurred). Step 4 shows `name: "Ivan Updated"` — the existing customer version was updated in place.

---

### TC-BE-6 (Positive): Different customer sent — customer swapped and latest version updated

**Description:** Verify that providing a different customer UIC in the PUT request swaps the customer linked to the contract and updates the new customer's latest created version with the supplied fields.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create the **original private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables per `GeneratePayload.customers.customer_private()`; name: "Ivan", surname: "Georgiev"). Save **originalCustomerUic**, **originalCustomerId**.
5. Create a **new private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables per `GeneratePayload.customers.customer_private()`; name: "Maria", surname: "Dimitrova"). Save **newCustomerUic**, **newCustomerId**.
6. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
7. Create communication data for contract via `POST /customer/{originalCustomerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
8. Create communication data for billing via `POST /customer/{originalCustomerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
9. Create a product contract via `POST /product-contract` (originalCustomer from step 4, POD from step 6, product from step 3; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields and `"customerUic": <newCustomerUic from step 5>` (different from the original), supplying all mandatory private-customer fields for the new customer (name: "Maria Updated", nameTransliterated: "Maria Updated", surname: "Dimitrova", surnameTransliterated: "Dimitrova", plus other required individual fields from envVariables / `GeneratePayload.customers.customer_private()`).
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the customerUic field.
4. Call `GET /sales-portal/customer/{newCustomerUic}` via `SPRequest` and record the customer `name` field.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `customerUic: <newCustomerUic>` (customer was swapped). Step 4 shows `name: "Maria Updated"` — the new customer's latest version was updated with the provided fields.

---

### TC-BE-7 (Positive): Proxy and manager both absent — existing proxy and manager left unchanged

**Description:** Verify that when neither proxy ID nor proxy parameters (and neither manager ID nor manager parameters) are sent in the request, any existing proxy and manager on the **private customer with business activity** remain untouched.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer with business activity** via `POST /customer` (type: **PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY**, status: ACTIVE, businessActivity: true; required individual + business nomenclature IDs from envVariables per `GeneratePayload.customers.customer_private_business()`; include a manager: firstName: "Georgi", surname: "Nikolov", mobileNumber: "+359888111111", email: "mgr1@test.com"). Save **customerUic**, **customerId**, **managerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; include a proxy: proxyName: "Existing Proxy", proxyDate: "2025-01-01", managerIds: [<managerId from step 4>]; all other mandatory fields). Save **contractId**, **contractVersionId**, **existingProxyId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields but **omit both the proxy sub-object and the manager sub-object entirely** from the body (no proxy or manager keys at all).
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the proxy name and proxyDate.
4. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and record the manager's `firstName`, `surname`, `mobileNumber`, and `email`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows proxy name: "Existing Proxy" and proxyDate: "2025-01-01" — the existing proxy was not removed or changed because both proxy ID and parameters were absent from the request. Step 4 shows the manager unchanged (`firstName: "Georgi"`, `surname: "Nikolov"`, `mobileNumber: "+359888111111"`, `email: "mgr1@test.com"`).

---

### TC-BE-8 (Positive): New proxy created when proxy ID is absent but proxy parameters are provided

**Description:** Verify that providing proxy parameters without a proxy ID causes the system to create a new proxy on the contract.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; **no proxy added**; all other mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields and a proxy sub-object **without a proxyId** and with parameters: `"proxyName": "New Proxy Person"`, `"proxyDate": "2025-06-01"`, `"uicOrPin": "7501011234"`.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the proxy list.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows a new proxy entry with `proxyName: "New Proxy Person"` and `proxyDate: "2025-06-01"` present in the contract's proxy list — a new proxy was created (not an existing one updated).

---

### TC-BE-9 (Positive): Manager details sent without email and phone — existing manager email and phone are not removed

**Description:** Verify the manager exception rule when the manager sub-object is present in the request: if the request includes manager details (e.g. managerId and other parameters such as firstName and surname) but omits `email` and `mobileNumber`, the customer's existing manager email and phone number are preserved and not treated as removal.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer with business activity** via `POST /customer` (type: **PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY**, status: ACTIVE, businessActivity: true; required individual + business nomenclature IDs from envVariables; name, surname, and transliterated fields per `GeneratePayload.customers.customer_private_business()`; include a manager: firstName: "Ivan", surname: "Petrov", mobileNumber: "+359888123456", email: "manager@test.com"). Save **customerUic**, **customerId**, **managerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract and customer fields and a manager sub-object containing `"managerId": <managerId from step 4>`, `"firstName": "Ivan"`, `"surname": "Petrov"` (and any other manager parameters required by the API), but **omit `email` and `mobileNumber`** from the manager object (do not send them as null — absent from the request).
2. Read and record the response status code and body.
3. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and record the manager's `mobileNumber`, `email`, `firstName`, and `surname`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows the manager retains `mobileNumber: "+359888123456"` and `email: "manager@test.com"` (not removed despite being absent from the request); `firstName` and `surname` reflect the values sent in step 1 where applicable.

---

### TC-BE-10 (Positive): KYC pass boolean = true with KYC expiration date — KYC fields updated

**Description:** Verify that sending `kycPassBoolean: true` with a valid `kycExpirationDate` in the customer section updates both KYC fields on the customer.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"; kycPassBoolean: false). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract and customer fields, adding `"kycPassBoolean": true` and `"kycExpirationDate": "2027-06-01"` in the customer section.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and record `kycPassBoolean` and `kycExpirationDate`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `kycPassBoolean: true` and `kycExpirationDate: "2027-06-01"` — both KYC fields were updated.

---

### TC-BE-11 (Positive): KYC pass boolean omitted — existing KYC status left unchanged

**Description:** Verify the leave-as-is rule for `kycPassBoolean`: omitting the field from the PUT request leaves the customer's existing KYC status and expiration date untouched.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"; kycPassBoolean: true, kycExpirationDate: "2028-01-01"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract and customer fields but **omit `kycPassBoolean` and `kycExpirationDate`** entirely from the request body.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and record `kycPassBoolean` and `kycExpirationDate`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `kycPassBoolean: true` and `kycExpirationDate: "2028-01-01"` — both unchanged (leave-as-is rule for radio/checkbox fields).

---

### TC-BE-12 (Negative): Missing mandatory field — signingDate omitted causes validation error

**Description:** Verify that omitting the mandatory `signingDate` field from the PUT request (scalar omission = removal) causes the API to return a validation error, since a mandatory field cannot be removed.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; signingDate: 2025-01-01; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all normally required fields **except `signingDate`** (omit it entirely — do not send null, simply absent).
2. Read and record the response status code and error body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the `signingDate` to confirm no change was made.

**Expected test case results:** Step 1 returns HTTP 400; response body contains an error referencing `signingDate` as mandatory / required (e.g., error message contains "signingDate" or "mandatory field missing"). Step 3 shows the original `signingDate: "2025-01-01"` — the contract was not modified.

---

### TC-BE-13 (Negative): Proxy ID sent that is not linked to the contract

**Description:** Verify that sending a proxy ID that does not belong to the contract being updated returns the error "Provided proxy is not added in contract".

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; **no proxy added**; all mandatory fields). Save **contractId**, **contractVersionId**.
9. Use a non-existent proxy ID: `9999999` (an ID not linked to the contract from step 8).

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields and a proxy sub-object containing `"proxyId": 9999999` plus valid proxy parameters (`"proxyName": "Test Proxy"`, `"proxyDate": "2025-06-01"`).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and verify the proxy list is empty/unchanged.

**Expected test case results:** Step 1 returns HTTP 400 (or 422); response body contains error message: "Provided proxy is not added in contract". Step 3 confirms no proxy was created or linked to the contract.

---

### TC-BE-14 (Negative): Proxy ID present but no proxy parameters sent

**Description:** Verify that providing a valid contract proxy ID with no accompanying proxy parameters returns the error "proxy parameters are missing".

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; include proxy: proxyName: "Existing Proxy", proxyDate: "2025-01-01"; all other mandatory fields). Save **contractId**, **contractVersionId**, **existingProxyId** from the response proxy list.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields and a proxy sub-object containing **only** `"proxyId": <existingProxyId from step 8>` with no other proxy fields (empty proxy object besides the ID, or all proxy parameters absent).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and verify the proxy remains unchanged.

**Expected test case results:** Step 1 returns HTTP 400; response body contains error message: "proxy parameters are missing". Step 3 confirms proxy data (proxyName: "Existing Proxy") is unchanged.

---

### TC-BE-15 (Negative): Manager ID does not belong to the contract customer

**Description:** Verify that providing a manager ID in the customer section that belongs to a different customer (not the one on the contract) returns the error "Provided ID is not in the customer".

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create the **contract private customer with business activity** via `POST /customer` (type: **PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY**, status: ACTIVE, businessActivity: true; required individual + business nomenclature IDs from envVariables per `GeneratePayload.customers.customer_private_business()`; name: "Ivan", surname: "Georgiev"; include a manager: firstName: "Georgi", surname: "Nikolov"). Save **contractCustomerUic**, **contractCustomerId**, **contractCustomerManagerId**.
5. Create an **unrelated private customer with business activity** via `POST /customer` (type: **PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY**, status: ACTIVE, businessActivity: true; required IDs from envVariables per `GeneratePayload.customers.customer_private_business()`; name: "Maria", surname: "Dimitrova"; include manager: firstName: "Dimitar", surname: "Stoev"). Save **otherCustomerManagerId** from the response.
6. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
7. Create communication data for contract via `POST /customer/{contractCustomerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
8. Create communication data for billing via `POST /customer/{contractCustomerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
9. Create a product contract via `POST /product-contract` (contractCustomer from step 4, POD from step 6, product from step 3; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields, `"customerUic": <contractCustomerUic from step 4>`, and a manager sub-object containing `"managerId": <otherCustomerManagerId from step 5>` (a manager ID that belongs to the OTHER customer) plus manager parameters (firstName: "Dimitar", surname: "Stoev").
2. Read and record the response status code and error message.

**Expected test case results:** HTTP 400; response body contains error message: "Provided ID is not in the customer". Neither the contract nor the customers are modified.

---

### TC-BE-16 (Negative): Manager ID present but no manager parameters sent

**Description:** Verify that providing a valid manager ID with no accompanying manager parameters returns the error "manager parameters are missing".

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer with business activity** via `POST /customer` (type: **PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY**, status: ACTIVE, businessActivity: true; required individual + business nomenclature IDs from envVariables; name, surname, and transliterated fields per `GeneratePayload.customers.customer_private_business()`; include manager: firstName: "Georgi", surname: "Nikolov"). Save **customerUic**, **customerId**, **managerId** from the response manager list.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract and customer fields, and include a manager sub-object containing **only** `"managerId": <managerId from step 4>` with **no other manager parameters** (all other manager fields absent).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and verify the manager's name (Georgi Nikolov) is unchanged.

**Expected test case results:** HTTP 400; response body contains error message: "manager parameters are missing". Step 3 confirms manager data is unchanged (firstName: "Georgi", surname: "Nikolov" still present).

---

### TC-BE-17 (Negative): Contract status changed to active-in-term without entering-into-force date

**Description:** Verify that changing the contract status to one that requires the "entering into force" date without providing that date causes the API to return an error. Confluence (pages 895254530, 898465794): e.g. status `ENTERED_INTO_FORCE` or `ACTIVE_IN_TERM` with mandatory `enteringIntoForceDate` omitted. This case uses `ACTIVE_IN_TERM` as the target status.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED, subStatus: IN_PROCESS; enteringIntoForceDate: not set / null; all other mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields, setting `"contractStatus": "ACTIVE_IN_TERM"` (or equivalent status that requires entering-into-force date) and **omitting `enteringIntoForceDate`** from the body (absent or null).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the contractStatus to confirm it was not changed.

**Expected test case results:** HTTP 400; response body contains an error indicating that a field mandatory for the target status is missing (error message references entering-into-force date or the status transition rule). Step 3 shows `contractStatus: "SIGNED"` — unchanged.

---

### TC-BE-18 (Negative): Product with non-fixed interim advance payment — API rejects the update

**Description:** Verify that the API returns an error when the contract's product has a non-fixed (variable) interim advance payment (IAP) configuration, since the PUT endpoint only supports products without IAP or with fixed-only IAP.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product **with a non-fixed (variable) interim advance payment** via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; configure IAP with valueType: VARIABLE or PERCENT). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, **non-fixed IAP product from step 3**; status: SIGNED; all other mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields referencing the contract from step 8 (which uses the non-fixed IAP product).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` to confirm the contract was not changed.

**Expected test case results:** HTTP 400 (or 422); response body contains an error indicating the product contains non-fixed interim advance payment data that is not supported by this API. Step 3 confirms the contract remains unchanged.

---

### TC-BE-19 (Negative): Direct debit = true with Bank ID, IBAN, and BIC absent

**Description:** Verify that enabling direct debit (`directDebit: true`) without providing the mandatory bank fields (bankId, IBAN, BIC) returns a validation error.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; directDebit: false; all other mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields, `"directDebit": true`, but **omit `bankId`, `iban`, and `bic`** entirely from the body.
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `directDebit` to confirm it was not changed.

**Expected test case results:** HTTP 400; response body contains an error indicating that bankId, IBAN, and/or BIC are mandatory when `directDebit` is true (error message references the missing required bank field(s)). Step 3 shows `directDebit: false` — unchanged.

---

### TC-BE-20 (Negative): KYC pass boolean = true without KYC expiration date

**Description:** Verify that setting `kycPassBoolean: true` in the customer section without providing `kycExpirationDate` returns a validation error, since the expiration date is mandatory when KYC is set to yes.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"; kycPassBoolean: false). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract and customer fields, setting `"kycPassBoolean": true` but **omitting `kycExpirationDate`** from the customer section (absent or null).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and record `kycPassBoolean` to confirm it was not changed.

**Expected test case results:** HTTP 400; response body contains an error indicating that `kycExpirationDate` is mandatory when `kycPassBoolean` is true (error message references the missing KYC expiration date). Step 3 shows `kycPassBoolean: false` — unchanged.

---

### TC-BE-21 (Negative): Private customer with business activity — second proxy rejected when proxy already on contract and request has proxy params without proxyId

**Description:** Verify that for a **PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY** customer, only one proxy may exist on the contract. When a proxy is already linked to the contract and the PUT request includes proxy parameters **without** `proxyId` (which would otherwise create a new proxy), the API returns an error and does not add a second proxy.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer with business activity** via `POST /customer` (type: **PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY**, status: ACTIVE, businessActivity: true; required individual + business nomenclature IDs from envVariables; name, surname, and transliterated fields per `GeneratePayload.customers.customer_private_business()`; include at least one manager required for this customer type). Save **customerUic**, **customerId**, **managerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; **include exactly one proxy**: proxyName: "Existing Proxy", proxyDate: "2025-01-01", managerIds: [<managerId from step 4>]; all other mandatory fields). Save **contractId**, **contractVersionId**, **existingProxyId** from the contract proxy list.
9. Confirm via `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` that the contract has **exactly one** proxy linked (proxyName: "Existing Proxy").

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract and customer fields for the **PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY** customer from step 4, and a proxy sub-object **without `proxyId`** containing parameters for a **new** proxy: `"proxyName": "Second Proxy Person"`, `"proxyDate": "2025-06-15"`, `"uicOrPin": "8501014321"` (and any other required proxy fields per API spec).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the proxy list (count and proxy names).

**Expected test case results:** Step 1 returns HTTP 400 (or 422); response body contains an error indicating that **only one proxy is allowed** for a private / private-with-business-activity customer and a second proxy cannot be added (e.g. error message contains a fragment such as "not possible to add more than one proxy" or "you cant add multiple proxy when customer type is not legal entity"). Step 3 shows **exactly one** proxy remains on the contract (`proxyName: "Existing Proxy"`); no second proxy was created.

---

### TC-BE-22 (Positive): Legal entity — second proxy added when proxy already on contract and request has proxy params without proxyId

**Description:** Verify that for a **LEGAL_ENTITY** customer, multiple proxies are allowed on the contract. When one proxy is already linked and the PUT request includes proxy parameters **without** `proxyId`, the API creates and links an additional proxy while leaving the existing proxy unchanged.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **legal entity** customer via `POST /customer` (type: **LEGAL_ENTITY**, status: ACTIVE; required nomenclature IDs from envVariables; name: "Legal Entity Corp", nameTransliterated: "Legal Entity Corp", surname: "Corp", surnameTransliterated: "Corp"; include a manager: firstName: "Georgi", surname: "Nikolov"). Save **customerUic**, **customerId**, **managerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, product from step 3; status: SIGNED; **include exactly one proxy**: proxyName: "First Proxy", proxyDate: "2025-01-01", managerIds: [<managerId from step 4>]; all other mandatory fields). Save **contractId**, **contractVersionId**, **existingProxyId** from the contract proxy list.
9. Confirm via `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` that the contract has **exactly one** proxy linked (proxyName: "First Proxy").

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract and customer fields for the **LEGAL_ENTITY** customer from step 4, and a proxy sub-object **without `proxyId`** containing parameters for a **new** proxy: `"proxyName": "Second Proxy Person"`, `"proxyDate": "2025-06-15"`, `"uicOrPin": "131234567"`, `"email": "second.proxy@test.com"`, `"mobileNumber": "+359888999888"`, `"managerIds": [<managerId from step 4>]` (and any other required proxy fields per API spec).
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record the proxy list (count, proxy names, and proxy IDs).

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows **two** proxies on the contract: the original (`proxyName: "First Proxy"`, proxyId matching **existingProxyId** from step 8) and a newly created proxy (`proxyName: "Second Proxy Person"`, proxyDate: "2025-06-15"`) with a **new** proxyId distinct from **existingProxyId**.

---

### TC-BE-23 (Positive): Different product sent — user can add or change product on the contract

**Description:** Verify that the user **may** add or change to a **different product** on the contract by sending a different `productId` (and matching `productVersionId`) in the PUT request. Per Confluence page 895254530 (v27): *“If different product is sent, we are changing product in contract.”* This is **not** direct editing of the product master object — it is changing which product is linked to the contract. Third-tab details are edited separately via `productParameters` (TC-BE-25).

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh). Save **priceComponentId**.
3. Create the **original** product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **second** (different) product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId2**, **productVersionId2**.
5. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
6. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
7. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
8. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
9. Create a product contract via `POST /product-contract` (customer from step 5, POD from step 6, **original product from step 3**; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**, **originalProductId** (= productId from step 3).

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields and `"productId": <productId2 from step 4>`, `"productVersionId": <productVersionId2 from step 4>` (change to a different product on the contract).
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `contractProduct.id` and `contractProduct.versionId`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `contractProduct.id: <productId2>` and `contractProduct.versionId: <productVersionId2>` — the contract now references the **different product** added by the user.

---

### TC-BE-24 (Negative): Product with price component that has no value filled — API rejects the update

**Description:** Verify that the API returns an error when the contract's product references a price component that does not have its value filled, since the PUT endpoint only works for price components which have a value — per Confluence "The API will only work for price components which have value filled."

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` **without a filled value** (type: ELECTRICITY, vatRateId from envVariables; value omitted/null — no price value set). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2 — the unfilled-value component]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customer from step 4, POD from step 5, **product with unfilled-value price component from step 3**; status: SIGNED; all mandatory fields). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields referencing the contract from step 8 (whose product uses a price component without a filled value).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` to confirm the contract was not changed.

**Expected test case results:** HTTP 400 (or 422); response body contains an error indicating that the product's price component does not have a filled value and is not supported by this API. Step 3 confirms the contract remains unchanged.

---

### TC-BE-25 (Positive): Update a product detail (third tab) without changing the product

**Description:** Verify that `PUT /sales-portal/contract` can update the contract's **product details** (the "third tab" — e.g. an additional-parameter value) while keeping the **same product** (`productId` / `productVersionId` unchanged), and that the new value is saved. Field names (`productParameters`, `productAdditionalParams`) follow the Phoenix / express-contract request shape (`ProductParameterBaseRequest`); Confluence requires reusing the existing third-tab retrieval API for product parameters.

**Preconditions:** Same setup as **TC-BE-1** (steps 1–8), with these extras:
1. From the terms entry (TC-BE-1 step 1), also save **productContractTermId** (term usable on the product contract).
2. On the product (TC-BE-1 step 3), include at least one **additional parameter** (known label + value). Also save **productDetailId**.
3. Read the third-tab values: `GET /product-contract/third-tab-fields?productDetailId=<productDetailId>` via `Request`. From `productAdditionalParams`, save one **paramId** and its current **validParamValue**.

**Test steps:**
1. Send `PUT /sales-portal/contract` via `SPRequest` with:
   - the full mandatory contract body (same as TC-BE-1);
   - the **same** `productId` / `productVersionId` (no product change);
   - a `productParameters` object carrying the third-tab fields (`contractType`, `productContractTermId`, `paymentGuarantee`, `contractFormulas: []`, `invoicePaymentTermId` / `invoicePaymentTermValue`, `entryIntoForce`, `startOfContractInitialTerm`, `supplyActivation`, `productContractWaitForOldContractTermToExpires`, `interimAdvancePayments: []`), with a **new value** for the additional parameter: `productAdditionalParams: [{ "id": <paramId>, "value": "<newValidValue>" }]` (any value valid for that parameter; may differ from **validParamValue**).
2. Record the response status code and body.
3. `GET /product-contract/third-tab-fields?productDetailId=<productDetailId>` via `Request` and check the value for **paramId**.
4. `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and confirm the product is unchanged.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows the additional parameter (**paramId**) now holds `<newValidValue>` (persisted). Step 4 confirms the contract still references the **same product** from the preconditions.

---

/* **NOTE** - This test case was removed because it is not technically feasible to implement. */

---

### TC-BE-27 (Positive): Product with fixed interim advance payment — update succeeds

**Description:** Verify that `PUT /sales-portal/contract` succeeds when the contract's product has an interim advance payment (IAP) with **fixed** parameters only. Confluence: the API works for products with **no IAP or with fixed IAP only** (pages 895254530, 898465794). This is the positive counterpart to TC-BE-18 (variable/percent IAP rejected).

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create a product **with a fixed interim advance payment** via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; availableForSale: true; configure IAP with `valueType: EXACT_AMOUNT` and a numeric `value` — fixed, not `PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT`/variable). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customerId from step 4, podIdentifier from step 5, productId from step 3, productVersionId from step 3; status: SIGNED, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: 2025-01-01, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (contractId, contractVersionId, contractStatus: SIGNED, contractSubStatus: IN_PROCESS, signingDate: 2025-01-01, productId from step 3, productVersionId from step 3, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId, enteringIntoForceDate: 2025-01-01, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, customerUic from step 4, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, podIdentifiers: [podIdentifier from step 5]) and `estimatedTotalConsumption: 5000`. Do not send IAP data (`productParameters.interimAdvancePayments: []` if product params are nested; IAP is not part of the PUT per Confluence).
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `estimatedTotalConsumption` and the product's IAP `valueType`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `estimatedTotalConsumption: 5000` (updated from 1000) and the product still carries its fixed IAP (`valueType: EXACT_AMOUNT`) — the fixed-IAP product is accepted and the update is persisted.

---

### TC-BE-28 (Positive): Add a second POD to the contract and update a contract value

**Description:** Verify that `PUT /sales-portal/contract` can add an additional POD to a contract that already has one (`podIdentifiers` is optional once at least one POD is added, per Confluence) while persisting another updated contract field.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create the **first** POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier1**.
6. Create the **second** POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier2**.
7. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
8. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
9. Create a product contract via `POST /product-contract` (customerId from step 4, podIdentifier from step 5 — **only podIdentifier1**, productId from step 3, productVersionId from step 3; status: SIGNED, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: 2025-01-01, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 7, communicationDataForBillingId from step 8, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (contractId, contractVersionId, contractStatus: SIGNED, contractSubStatus: IN_PROCESS, signingDate: 2025-01-01, productId from step 3, productVersionId from step 3, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId, enteringIntoForceDate: 2025-01-01, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, customerUic from step 4, communicationDataForContractId from step 7, communicationDataForBillingId from step 8), `estimatedTotalConsumption: 6000`, and `podIdentifiers: [<podIdentifier1>, <podIdentifier2>]`.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `estimatedTotalConsumption` and the linked POD identifiers.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `estimatedTotalConsumption: 6000` and the contract linked to **both** `podIdentifier1` and `podIdentifier2`.

---

### TC-BE-29 (Positive): Update customer local address via PUT

**Description:** Verify that `PUT /sales-portal/contract` can update the contract customer's **local** address (Bulgarian / nomenclature-ID based) in place. Confluence address matrix (page 898465794): for a local address (`isForeign: false`), `countryId` is mandatory and `regionId` / `municipalityId` / `populatedPlaceId` / `zipCodeId` are mandatory when the address is local. The Phoenix Sales Portal DTO models this with `registered: true` and nomenclature ID fields (`SalesPortalAddressRequest`).

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"; local address with `countryId`, `regionId`, `municipalityId`, `populatedPlaceId`, `zipCodeId`, `streetId` from envVariables — call the baseline `streetId` **streetIdA**). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customerId from step 4, podIdentifier from step 5, productId from step 3, productVersionId from step 3; status: SIGNED, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: 2025-01-01, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.
9. From envVariables, pick a second valid street id **streetIdB** (different from **streetIdA**) to send in the PUT.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (as in TC-BE-1 step 1) and the **same** `customerUic` from step 4 (in-place customer update, as in TC-BE-5), plus a customer address block: `registered: true` (local) with mandatory IDs `countryId`, `regionId`, `municipalityId`, `populatedPlaceId`, `zipCodeId` from step 4 and the changed `streetId: <streetIdB from step 9>`.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and record the address `streetId` and `registered` flag.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows the same customerUic (no customer swap), `registered: true`, and `address.streetId == streetIdB` (other mandatory IDs unchanged) — the local address was updated in place.

---

### TC-BE-30 (Negative): Foreign customer address missing mandatory fields — API rejects the update

**Description:** Verify that `PUT /sales-portal/contract` returns an error when a **foreign** customer address (`isForeign: true` → `registered: false`) is sent without its mandatory text fields. Confluence foreign-address matrix (page 898465794): `countryId` plus `region`, `municipality`, `populatedPlace`, `zipCode` are mandatory when the address is foreign.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customerId from step 4, podIdentifier from step 5, productId from step 3, productVersionId from step 3; status: SIGNED, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: 2025-01-01, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (as in TC-BE-1 step 1) and the same `customerUic` from step 4, plus a customer address block with `registered: false` (foreign), `countryId` set, but **omitting** the mandatory foreign text fields `region`, `municipality`, `populatedPlace`, `zipCode`.
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/customer/{customerUic}` via `SPRequest` and confirm the customer address is unchanged.

**Expected test case results:** Step 1 returns HTTP 400 (TBD: confirm 400 vs 422 once deployed); response body contains a validation error naming the missing required-when-foreign fields (`region`, `municipality`, `populatedPlace`, `zipCode`). Step 3 confirms the customer address (and contract) are unchanged.

---

### TC-BE-31 (Negative): Product swap with mismatched productVersionId — API rejects the update

**Description:** Verify that `PUT /sales-portal/contract` rejects a product change where the `productVersionId` does not belong to the supplied `productId`. Confluence allows changing to a different product (page 895254530: *“If different product is sent, we are changing product in contract”*), which requires a consistent `productId` / `productVersionId` pair. This is the negative counterpart to TC-BE-23.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create the **original** product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **second** (different) product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId2**, **productVersionId2**.
5. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
6. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
7. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 5; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
8. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 5; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
9. Create a product contract via `POST /product-contract` (customerId from step 5, podIdentifier from step 6, **original product from step 3** (productId, productVersionId); status: SIGNED, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: 2025-01-01, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 7, communicationDataForBillingId from step 8, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (as in TC-BE-1 step 1) but with `productId: <productId2 from step 4>` and `productVersionId: <productVersionId from step 3>` (a version that belongs to the **original** product, not to `productId2`).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `contractProduct.id` and `contractProduct.versionId`.

**Expected test case results:** Step 1 returns HTTP 400 (TBD: confirm 400 vs 422 once deployed); response body contains an error indicating that the supplied `productVersionId` does not belong to `productId` (product/version mismatch). Step 3 confirms the contract still references the **original** product — both `contractProduct.id == productId` and `contractProduct.versionId == productVersionId` from step 3 (no partial swap).

---

### TC-BE-32 (Positive): Contract status change — SIGNED to ENTERED_INTO_FORCE

**Description:** Verify that `PUT /sales-portal/contract` can transition a contract from `SIGNED` to `ENTERED_INTO_FORCE` when the mandatory field for that status (`enteringIntoForceDate`) and the valid sub-status (`AWAITING_ACTIVATION`) are provided. Confluence status validation (pages 895254530, 898465794): changing status requires parameters mandatory for the target status. Phoenix status chain (`ProductContractStatusChainUtil`): `SIGNED` → `ENTERED_INTO_FORCE` is allowed.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customerId from step 4, podIdentifier from step 5, productId from step 3, productVersionId from step 3; status: SIGNED, subStatus: IN_PROCESS, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: not set / null, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (contractId, contractVersionId, signingDate: 2025-01-01, productId from step 3, productVersionId from step 3, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, startOfInitialTerm: 2025-01-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, customerUic from step 4, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, podIdentifiers: [podIdentifier from step 5], estimatedTotalConsumption: 1000) and the status transition: `"contractStatus": "ENTERED_INTO_FORCE"`, `"contractSubStatus": "AWAITING_ACTIVATION"`, `"enteringIntoForceDate": "2025-02-01"`.
2. Read and record the response status code and body.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `contractStatus`, `contractSubStatus` (or equivalent sub-status field), and `enteringIntoForceDate`.

**Expected test case results:** Step 1 returns HTTP 200 with `{ "status": "success" }`. Step 3 shows `contractStatus: "ENTERED_INTO_FORCE"`, `contractSubStatus: "AWAITING_ACTIVATION"`, and `enteringIntoForceDate: "2025-02-01"` — the status transition and mandatory date are persisted.

---

### TC-BE-33 (Negative): Manual status change — ENTERED_INTO_FORCE to ACTIVE_IN_TERM rejected

**Description:** Verify that `PUT /sales-portal/contract` **rejects** a manual transition from `ENTERED_INTO_FORCE` to `ACTIVE_IN_TERM`. Confluence — Product contract edit (page 2228419): from **Entered into force**, **Active in term** is allowed **automatically only** (POD activation / scheduler), not via manual Sales Portal status change. `ProductContractStatusChainUtil.canBeChanged()` currently allows the transition in code, but SP must enforce the manual/automatic matrix per Confluence. **Regression test:** fails on current backend until manual EIF → AIT is blocked on the SP path.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customerId from step 4, podIdentifier from step 5, productId from step 3, productVersionId from step 3; status: ENTERED_INTO_FORCE, subStatus: AWAITING_ACTIVATION, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: past or today, startOfInitialTerm: 2025-02-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (including valid past/today `enteringIntoForceDate`) and the status transition: `"contractStatus": "ACTIVE_IN_TERM"`, `"contractSubStatus": "DELIVERY"`.
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `contractStatus` and `contractSubStatus`.

**Expected test case results:** HTTP **400**; response body indicates the manual transition is not allowed (e.g. references status transition / manual vs automatic rule / `ACTIVE_IN_TERM`). Step 3 shows `contractStatus: "ENTERED_INTO_FORCE"` and `contractSubStatus: "AWAITING_ACTIVATION"` — unchanged.

---

### TC-BE-34 (Negative): Contract status changed to TERMINATED without termination date

**Description:** Verify that changing the contract status to `TERMINATED` without a termination date causes the API to return an error. PHN-2130 Confluence (pages 895254530, 898465794) does not expose `terminationDate` on `SalesPortalContractUpdateRequest` (Swagger: `SalesPortalContractUpdateRequest` has no termination-date field). Phoenix product-contract edit (`ProductContractDateService.validatePerpetuity`) requires `basicParameters.terminationDate` when status is `TERMINATED`. Status chain allows `ACTIVE_IN_TERM` → `TERMINATED` (`ProductContractStatusChainUtil`), but the SP PUT cannot supply the mandatory date until the API is extended.

**Preconditions:**
1. Create a terms entry via `POST /terms` (type: PERIOD, periodType: DAY_DAYS, value: 30). Save **termId**.
2. Create an electricity price component via `POST /price-components` (type: ELECTRICITY, vatRateId from envVariables; value: 0.15 BGN/kWh — value must be filled). Save **priceComponentId**.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds: [priceComponentId from step 2]; status: ACTIVE, contractTypes: [SUPPLY_ONLY], paymentGuarantees: [NO]; no IAP; availableForSale: true). Save **productId**, **productVersionId**.
4. Create a **private customer** via `POST /customer` (type: **PRIVATE_CUSTOMER**, status: ACTIVE, businessActivity: false; required individual nomenclature IDs from envVariables — firstName, lastName, middleName and transliterated fields, dateOfBirth, placeOfBirth, identity document fields per `GeneratePayload.customers.customer_private()`; name: "Ivan", nameTransliterated: "Ivan", surname: "Georgiev", surnameTransliterated: "Georgiev"). Save **customerUic**, **customerId**.
5. Create a POD via `POST /pod` (type: ELECTRICITY, status: ACTIVE, activationDate: 2025-01-01). Save **podIdentifier**.
6. Create communication data for contract via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "contract@test.com"). Save **commContractId**.
7. Create communication data for billing via `POST /customer/{customerId}/communication-data` (customerId from step 4; method: ELECTRONIC, contactType: EMAIL, contactValue: "billing@test.com"). Save **commBillingId**.
8. Create a product contract via `POST /product-contract` (customerId from step 4, podIdentifier from step 5, productId from step 3, productVersionId from step 3; status: ACTIVE_IN_TERM, subStatus: DELIVERY, signingDate: 2025-01-01, contractType: SUPPLY_ONLY, contractTerm: DEFINED_TERM, invoicePaymentTerm: 30, paymentGuarantee: NO, cashDepositAmount: 0, cashDepositCurrencyId from envVariables, bankGuaranteeAmount: 0, bankGuaranteesCurrencyId from envVariables, enteringIntoForceDate: 2025-02-01, startOfInitialTerm: 2025-02-01, supplyActivationAfterResigning: false, waitForOldContractTermToExpire: false, communicationDataForContractId from step 6, communicationDataForBillingId from step 7, estimatedTotalConsumption: 1000). Save **contractId**, **contractVersionId**.

**Test steps:**
1. Call `PUT /sales-portal/contract` via `SPRequest` with all mandatory contract fields (including valid past/today `enteringIntoForceDate`) and the status transition: `"contractStatus": "TERMINATED"`, `"contractSubStatus": "FROM_CUSTOMER_WITH_NOTICE"`. Do **not** send `terminationDate` (field is absent from the SP contract update schema).
2. Read and record the response status code and error message.
3. Call `GET /sales-portal/product-contract/{contractId}/version/{contractVersionId}` via `SPRequest` and record `contractStatus` and `contractSubStatus`.

**Expected test case results:** HTTP 400; response body contains an error indicating termination date is mandatory for `TERMINATED` (e.g. `basicParameters.terminationDate` / `termination date should not be empty`). Step 3 shows `contractStatus: "ACTIVE_IN_TERM"` and `contractSubStatus: "DELIVERY"` — unchanged.

---

## References

- **Jira:** [PHN-2130](https://oppa-support.atlassian.net/browse/PHN-2130) — Put: Update existing contract Part 1 (Development)
- **Confluence:** [Put: Update existing contract](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/895254530) (page 895254530) — user story + business rules; third-tab product parameters retrieval
- **Confluence:** [Technical documentation: update existing contract](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/898465794) (page 898465794) — request/response field matrix; third-tab retrieval rule
- **Direct debit rules in test scope (Confluence 895254530):** Enable with mandatory bank fields when `directDebit: true` (TC-BE-2); **`directDebit: false` unchecks only — bank fields leave-as-is** (TC-BE-3); omit `directDebit` → unchanged (TC-BE-4); enable without bank fields **rejected** (TC-BE-19).
- **Product rules in test scope:** Different product on contract **allowed** (TC-BE-23); price component with **no value filled** **rejected** (TC-BE-24); product swap with **mismatched** `productVersionId` **rejected** (TC-BE-31); third-tab `productParameters` on contract **allowed** (TC-BE-25, TC-BE-26); IAP with fixed parameters **accepted** (TC-BE-27), non-fixed/variable IAP **rejected** (TC-BE-18).
- **Customer types in test scope:** Most cases use **private** or **private with business activity**; **legal entity** only for multi-proxy (TC-BE-22); private-with-business single-proxy limit (TC-BE-21).
- **Customer / POD rules in test scope:** Add a second POD when the contract already has one (TC-BE-28); update customer **local** address in place (TC-BE-29); **foreign** address missing mandatory fields **rejected** (TC-BE-30).
- **Status transition rules in test scope:** `SIGNED` → `ENTERED_INTO_FORCE` with mandatory `enteringIntoForceDate` (TC-BE-32); status change without mandatory `enteringIntoForceDate` **rejected** (TC-BE-17); manual `ENTERED_INTO_FORCE` → `ACTIVE_IN_TERM` **rejected** — Confluence 2228419 automatic-only (TC-BE-33, regression until backend fix); `ACTIVE_IN_TERM` → `TERMINATED` without `terminationDate` **rejected** — SP API gap (TC-BE-34). Phoenix chain: `ProductContractStatusChainUtil`, `ContractDetailsStatus`; date rule: `ProductContractDateService.validatePerpetuity`.
- **Related:** PHN-2983 — Add KYC Fields to Put: Update existing contract (Duplicate / Done)
- **Phoenix source (read-only):** `SalesPortalContractController`, `SalesPortalCustomerUpdateService`, `ProductContractProductParametersService` (`productAdditionalParams` validation), `ProductContractController` (`GET /product-contract/third-tab-fields`), `ProductParameterBaseRequest`, `ExpressContractProductParametersRequest`, `SalesPortalAddressRequest` / `SalesPortalCustomerUpdateRequest` (`registered` flag — local vs foreign address)
- **Environment:** dev2
