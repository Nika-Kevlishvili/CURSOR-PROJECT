# Product Contract Creation – Backend Test Cases (General)

**Jira:** N/A (General test cases)  
**Type:** Feature  
**Summary:** Test cases for Product Contract creation via `POST /product-contract` endpoint covering happy path, POD linkage, and validation scenarios.

**Scope:** Product Contract creation flow in Phoenix. The endpoint creates a contract header in `product_contract.contracts`, version details in `contract_details`, and optionally links PODs via `contract_pods` with initial `contract_billing_groups`. Tests cover successful creation, POD integration, and validation rejections for missing/invalid data.

---

## Test data (preconditions)

Shared setup for all test cases in this file. Every entity must be created from scratch.

- **Environment:** Dev

1. Create a customer via `POST /customer` with:
   - `customerType`: `PRIVATE`
   - `status`: `ACTIVE`
   - `customerIdentifier`: auto-generated
   - `segment`: valid segment from `envVariables`
   - Store `customerId` and `customerDetailId` for subsequent steps.

2. Create a POD (Point of Delivery) via `POST /pod` with:
   - `podType`: `ELECTRICITY`
   - `status`: `ACTIVE`
   - `activationDate`: today minus 30 days
   - `gridOperatorId`: valid grid operator from `envVariables` compatible with the product
   - `consumptionType`: `NON_HOUSEHOLD`
   - `meteringType`: valid metering type
   - `voltageLevel`: valid voltage level
   - Store `podId` and `podDetailId` for subsequent steps.

3. Create terms via `POST /terms` with:
   - `termType`: `PERIOD`
   - `value`: `100`
   - `periodType`: `DAY_DAYS`
   - Store `termId`.

4. Create an electricity price component via `POST /price-components` with:
   - `priceComponentType`: `ELECTRICITY`
   - `vatRateId`: from `envVariables`
   - Store `priceComponentId`.

5. Create a product via `POST /products` with:
   - `termId`: from step 3
   - `priceComponentIds`: [id from step 4]
   - `productStatus`: `ACTIVE`
   - `availableForSale`: `true`
   - `isIndividual`: `false`
   - `globalSalesChannel`: `true` (ALL channels)
   - `globalSalesArea`: `true` (ALL areas)
   - `globalSegment`: `true` (ALL segments)
   - `contractTypes`: `[SUPPLY_ONLY]`
   - `paymentGuarantees`: `[NO]`
   - `gridOperatorId`: same as POD's grid operator (step 2) for compatibility
   - Store `productId` and `productDetailId`.

---

## Backend Test Cases

### TC-BE-1 (Positive): Create product contract with all required fields – happy path

**Description:** Verify that a product contract can be successfully created via `POST /product-contract` when all required fields are provided and the customer, product, and POD are valid and compatible.

**Preconditions:**

1. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`, segment: valid from `envVariables`). Store `customerId`, `customerDetailId`.
2. Create a POD via `POST /pod` (type: `ELECTRICITY`, status: `ACTIVE`, activationDate: today − 30 days, gridOperatorId: valid, consumptionType: `NON_HOUSEHOLD`). Store `podId`, `podDetailId`.
3. Create terms via `POST /terms` (termType: `PERIOD`, value: `100`, periodType: `DAY_DAYS`). Store `termId`.
4. Create a price component via `POST /price-components` (type: `ELECTRICITY`, vatRateId: from `envVariables`). Store `priceComponentId`.
5. Create a product via `POST /products` (termId from step 3, priceComponentIds from step 4, status: `ACTIVE`, availableForSale: `true`, contractTypes: `[SUPPLY_ONLY]`, paymentGuarantees: `[NO]`, gridOperatorId: same as step 2). Store `productId`, `productDetailId`.

**Test steps:**

1. Build `ProductContractCreateRequest` payload with:
   - `customerDetailId`: from precondition step 1
   - `productDetailId`: from precondition step 5
   - `entryIntoForceDate`: today
   - `contractStatus`: `ACTIVE`
   - `versionStatus`: `SIGNED`
   - `contractType`: `SUPPLY_ONLY`
   - Other required fields per Swagger spec
2. Send `POST /product-contract` with the payload.
3. Capture response status and body.
4. Verify the contract was persisted: call `GET /product-contract/{id}` using the returned contract ID.

**Expected test case results:**

- HTTP 201 Created.
- Response body contains `id` (contract ID), `contractNumber` (auto-generated, not `TMP-TEMP`), `contractStatus`: `ACTIVE`.
- `GET /product-contract/{id}` returns the created contract with matching `customerDetailId`, `productDetailId`, `entryIntoForceDate`.
- No error messages in response.

**References:** `ProductContractController.create`, `ProductContractService.createProductContract`.

---

### TC-BE-2 (Positive): Create product contract with POD linkage – billing group created

**Description:** Verify that when a product contract is created with a POD in the request, the system creates `contract_pods` and an initial `contract_billing_groups` record. This ensures downstream billing runs can reference the contract.

**Preconditions:**

1. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`). Store `customerId`, `customerDetailId`.
2. Create a POD via `POST /pod` (type: `ELECTRICITY`, status: `ACTIVE`, activationDate: today − 30 days, gridOperatorId: valid, consumptionType: `NON_HOUSEHOLD`, meteringType: valid, voltageLevel: valid). Store `podId`, `podDetailId`.
3. Create terms via `POST /terms` (termType: `PERIOD`, value: `100`, periodType: `DAY_DAYS`). Store `termId`.
4. Create a price component via `POST /price-components` (type: `ELECTRICITY`, vatRateId: from `envVariables`). Store `priceComponentId`.
5. Create a product via `POST /products` (termId from step 3, priceComponentIds from step 4, status: `ACTIVE`, availableForSale: `true`, contractTypes: `[SUPPLY_ONLY]`, paymentGuarantees: `[NO]`, gridOperatorId: same as step 2). Store `productId`, `productDetailId`.

**Test steps:**

1. Build `ProductContractCreateRequest` payload with:
   - `customerDetailId`: from precondition step 1
   - `productDetailId`: from precondition step 5
   - `entryIntoForceDate`: today
   - `contractStatus`: `ACTIVE`
   - `versionStatus`: `SIGNED`
   - `contractType`: `SUPPLY_ONLY`
   - `pods`: array containing `{ podDetailId: <from step 2>, activationDate: today }`
2. Send `POST /product-contract` with the payload.
3. Capture response status and body; store `contractId` and `contractDetailId`.
4. Query contract PODs: `GET /product-contract/{contractId}/pods` (or equivalent endpoint).
5. Query billing groups: `GET /billing-group?contractId={contractId}` (or read from response if included).

**Expected test case results:**

- HTTP 201 Created.
- Response body contains `id`, `contractNumber`, `contractStatus`: `ACTIVE`.
- Contract PODs response includes the POD from step 2 with `activationDate` = today.
- At least one billing group exists for the contract (created by `BillingGroupService.createInitial`).
- No validation errors.

**References:** `ProductContractPodService.addPodsToContract`, `BillingGroupService.createInitial`.

---

### TC-BE-3 (Negative): Create product contract without customer – validation error

**Description:** Verify that attempting to create a product contract without a valid `customerDetailId` returns a validation error. The system should reject the request and not create any contract record.

**Preconditions:**

1. Create terms via `POST /terms` (termType: `PERIOD`, value: `100`, periodType: `DAY_DAYS`). Store `termId`.
2. Create a price component via `POST /price-components` (type: `ELECTRICITY`, vatRateId: from `envVariables`). Store `priceComponentId`.
3. Create a product via `POST /products` (termId from step 1, priceComponentIds from step 2, status: `ACTIVE`, availableForSale: `true`, contractTypes: `[SUPPLY_ONLY]`, paymentGuarantees: `[NO]`). Store `productId`, `productDetailId`.
4. Do NOT create a customer — `customerDetailId` will be `null` or omitted.

**Test steps:**

1. Build `ProductContractCreateRequest` payload with:
   - `customerDetailId`: `null` (or omit the field)
   - `productDetailId`: from precondition step 3
   - `entryIntoForceDate`: today
   - `contractStatus`: `ACTIVE`
   - `versionStatus`: `SIGNED`
   - `contractType`: `SUPPLY_ONLY`
2. Send `POST /product-contract` with the payload.
3. Capture response status and body.

**Expected test case results:**

- HTTP 400 Bad Request.
- Response body contains a validation error message indicating `customerDetailId` is required (e.g., "Customer detail ID must not be null", "customerDetailId is required", or equivalent).
- No contract record is created in the database.
- No orphan records in `product_contract.contracts` or `contract_details`.

**References:** `ProductContractValidatorService`, `@Valid` annotation on `ProductContractCreateRequest`.

---

### TC-BE-4 (Negative): Create product contract with incompatible POD – grid operator mismatch

**Description:** Verify that when a POD's grid operator is incompatible with the product's grid operator, the system rejects the contract creation with a clear validation error. This tests the POD ↔ product matrix validation.

**Preconditions:**

1. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`). Store `customerId`, `customerDetailId`.
2. Create a POD via `POST /pod` (type: `ELECTRICITY`, status: `ACTIVE`, activationDate: today − 30 days, gridOperatorId: **Grid Operator A**, consumptionType: `NON_HOUSEHOLD`). Store `podId`, `podDetailId`.
3. Create terms via `POST /terms` (termType: `PERIOD`, value: `100`, periodType: `DAY_DAYS`). Store `termId`.
4. Create a price component via `POST /price-components` (type: `ELECTRICITY`, vatRateId: from `envVariables`). Store `priceComponentId`.
5. Create a product via `POST /products` (termId from step 3, priceComponentIds from step 4, status: `ACTIVE`, availableForSale: `true`, contractTypes: `[SUPPLY_ONLY]`, paymentGuarantees: `[NO]`, gridOperatorId: **Grid Operator B** — different from step 2). Store `productId`, `productDetailId`.
6. Ensure Grid Operator A and Grid Operator B are distinct and the product's rules do not allow Grid Operator A.

**Test steps:**

1. Build `ProductContractCreateRequest` payload with:
   - `customerDetailId`: from precondition step 1
   - `productDetailId`: from precondition step 5
   - `entryIntoForceDate`: today
   - `contractStatus`: `ACTIVE`
   - `versionStatus`: `SIGNED`
   - `contractType`: `SUPPLY_ONLY`
   - `pods`: array containing `{ podDetailId: <from step 2>, activationDate: today }`
2. Send `POST /product-contract` with the payload.
3. Capture response status and body.

**Expected test case results:**

- HTTP 400 Bad Request (or HTTP 422 Unprocessable Entity, depending on API design).
- Response body contains an error message indicating POD/product incompatibility, such as "POD grid operator is not compatible with product", "Grid operator mismatch", or similar.
- No contract record is created.
- No `contract_pods` or `contract_billing_groups` records are created.

**References:** `ProductContractPodService.addPodsToContract`, POD ↔ product matrix validation in `ProductContractValidatorService`.

---

### TC-BE-5 (Negative): Create product contract with inactive product – validation error

**Description:** Verify that attempting to create a product contract with an `INACTIVE` product is rejected. Only `ACTIVE` products with `availableForSale: true` should be eligible for new contracts.

**Preconditions:**

1. Create a customer via `POST /customer` (type: `PRIVATE`, status: `ACTIVE`). Store `customerId`, `customerDetailId`.
2. Create a POD via `POST /pod` (type: `ELECTRICITY`, status: `ACTIVE`, activationDate: today − 30 days, gridOperatorId: valid). Store `podId`, `podDetailId`.
3. Create terms via `POST /terms` (termType: `PERIOD`, value: `100`, periodType: `DAY_DAYS`). Store `termId`.
4. Create a price component via `POST /price-components` (type: `ELECTRICITY`, vatRateId: from `envVariables`). Store `priceComponentId`.
5. Create a product via `POST /products` (termId from step 3, priceComponentIds from step 4, status: **`INACTIVE`**, availableForSale: `false`, contractTypes: `[SUPPLY_ONLY]`, paymentGuarantees: `[NO]`, gridOperatorId: same as step 2). Store `productId`, `productDetailId`.

**Test steps:**

1. Build `ProductContractCreateRequest` payload with:
   - `customerDetailId`: from precondition step 1
   - `productDetailId`: from precondition step 5 (INACTIVE product)
   - `entryIntoForceDate`: today
   - `contractStatus`: `ACTIVE`
   - `versionStatus`: `SIGNED`
   - `contractType`: `SUPPLY_ONLY`
2. Send `POST /product-contract` with the payload.
3. Capture response status and body.

**Expected test case results:**

- HTTP 400 Bad Request.
- Response body contains an error message indicating the product is not eligible for contract creation, such as "Product is not active", "Product not available for sale", or "Cannot create contract with inactive product".
- No contract record is created in `product_contract.contracts`.
- No orphan records.

**References:** `ProductRelatedEntitiesService.canCreateProductContractWithProductVersionAndCustomer`, product eligibility rules.

---

## References

- **Jira:** N/A (General test cases for Product Contract creation)
- **Entry point:** `POST /product-contract` → `ProductContractController.create`
- **Service:** `ProductContractService.createProductContract` (phoenix-core-lib)
- **Validators:** `ProductContractValidatorService`, `ProductContractPodService`
- **Database:** `product_contract.contracts`, `product_contract.contract_details`, `product_contract.contract_pods`, `product_contract.contract_billing_groups`
- **Related:** Billing groups, customer status events, deal events (when SIGNED)
