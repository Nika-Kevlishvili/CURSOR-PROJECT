# Get Product List (Energy Products) – Backend API Tests (GET-PRODUCT-LIST)

**Jira:** GET-PRODUCT-LIST (Task description provided)  
**Type:** Task  
**Summary:** Backend API tests for `POST /products/list` endpoint that returns all standard products available for sale to customers, applying strict filtering rules per business requirements.

**Scope:** When `POST /products/list` is called, Phoenix should return only products that meet ALL requirements: Active, Available for sale, "Portals" sales channel, ALL areas, ALL segments, fixed parameters only, not individual/deleted, single contract type, valid terms/payment guarantee/price components/interim payments/equal installments, and excluding re-signing products. Products failing any validation must NOT appear in the response.

---

## Test data (preconditions)

Shared setup for this file (environment + entity creation chain).

- **Environment:** Test
1. Create terms via `POST /terms` (type: PERIOD, value: 12, periodType: MONTH, status: ACTIVE).
2. Create a price component via `POST /price-components` (type: ELECTRICITY, valueType: FIXED, value: 0.235, status: ACTIVE, with resolved formula variables).
3. Create a product via `POST /products` with the following attributes:
   - `productStatus`: ACTIVE
   - `availableForSale`: true
   - `availableFrom`: null or past date
   - `availableTo`: null or future date
   - `globalSalesChannel`: true (ALL channels including "Portals")
   - `globalSalesArea`: true (ALL areas)
   - `globalSegment`: true (ALL segments)
   - `isIndividual`: false
   - `contractTypes`: single value (e.g., ["SUPPLY_ONLY"])
   - `productTerms`: single term with type PERIOD (NOT CERTAIN_DATE)
   - `paymentGuarantees`: single value (e.g., ["NO"] or ["CASH_DEPOSIT"] with amount/currency)
   - `termId`: linked to terms from step 1
   - `priceComponentIds`: linked to price component from step 2 (with value filled, NOT value from/to)
   - `enteringIntoForce`: single value, type NOT 'Exact day' or 'Manual'
   - `startOfInitialTerm`: single value, type NOT 'Exact day' or 'Manual'
   - `supplyActivationAfterResigning`: type NOT 'Exact day'
   - `interimAdvancePayments`: if present, NOT "at least one is selected"; if Obligatory, all validations pass
   - `equalMonthlyInstallmentsActivation`: false (or true with installmentNumber and amount filled)
4. Verify product is created successfully (HTTP 200/201, response contains `id`).

---

## Backend Test Cases

### TC-BE-1 (Positive): Happy path – Valid product appears in product list

**Description:** Verify that a fully configured standard product meeting all requirements is returned by `POST /products/list`.

**Preconditions:**
1. Complete steps 1–4 from Test data above.
2. Product status is ACTIVE, availableForSale is true.
3. Product has globalSalesChannel, globalSalesArea, globalSegment all set to true.
4. Product is NOT individual (isIndividual: false).

**Test steps:**
1. Call `POST /products/list` with empty filter body `{}`.
2. Parse the response JSON.
3. Search for the created product by ID in the response content.

**Expected test case results:** HTTP 200. Response contains a paginated list. The created product appears in the list with all expected attributes (Product ID, Product Version, PrintingName, etc.). The response structure matches the documented JSON format.

---

### TC-BE-2 (Positive): Product with "Portals" sales channel via globalSalesChannel=true is included

**Description:** Verify that products with `globalSalesChannel: true` (which includes "Portals") appear in the list.

**Preconditions:**
1. Create terms via `POST /terms`.
2. Create price component via `POST /price-components`.
3. Create product with `globalSalesChannel: true` and all other required attributes.

**Test steps:**
1. Call `POST /products/list` with empty body.
2. Verify the product appears in the response.

**Expected test case results:** HTTP 200. Product with globalSalesChannel=true is included in the list because "Portals" channel is part of ALL channels.

---

### TC-BE-3 (Negative): Inactive product is excluded from product list

**Description:** Verify that products with `productStatus: INACTIVE` are NOT returned by the list.

**Preconditions:**
1. Create terms and price component.
2. Create product with `productStatus: INACTIVE`, all other attributes valid.

**Test steps:**
1. Call `POST /products/list` with empty body.
2. Search for the created product by ID in response.

**Expected test case results:** HTTP 200. The inactive product does NOT appear in the response list.

---

### TC-BE-4 (Negative): Product with availableForSale=false is excluded

**Description:** Verify that products not available for sale are excluded from the list.

**Preconditions:**
1. Create terms and price component.
2. Create product with `availableForSale: false`, productStatus: ACTIVE.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product with availableForSale=false does NOT appear in the list.

---

### TC-BE-5 (Negative): Product outside availability period (current date before availableFrom) is excluded

**Description:** Verify that products with `availableFrom` in the future are excluded.

**Preconditions:**
1. Create product with `availableFrom`: future date (e.g., 2027-01-01), `availableTo`: null.
2. Product is ACTIVE and availableForSale: true.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because current date is before availableFrom.

---

### TC-BE-6 (Negative): Product outside availability period (current date after availableTo) is excluded

**Description:** Verify that products with `availableTo` in the past are excluded.

**Preconditions:**
1. Create product with `availableFrom`: null, `availableTo`: past date (e.g., 2025-01-01).
2. Product is ACTIVE and availableForSale: true.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because current date is after availableTo.

---

### TC-BE-7 (Positive): Product within availability period is included

**Description:** Verify that products with current date within availableFrom/availableTo range are included.

**Preconditions:**
1. Create product with `availableFrom`: past date (2024-01-01), `availableTo`: future date (2030-12-31).
2. Product is ACTIVE, availableForSale: true, all other validations pass.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-8 (Positive): Product with both availableFrom and availableTo null is included

**Description:** Verify that products with no date restrictions (both null) are included.

**Preconditions:**
1. Create product with `availableFrom`: null, `availableTo`: null.
2. Product is ACTIVE, availableForSale: true, all other validations pass.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list (no date restriction applies).

---

### TC-BE-9 (Negative): Individual product (isIndividual=true) is excluded

**Description:** Verify that individual products are excluded from the standard list.

**Preconditions:**
1. Create product with `isIndividual: true`, all other attributes valid.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Individual product does NOT appear in the list.

---

### TC-BE-10 (Negative): Deleted product is excluded

**Description:** Verify that deleted products (status or flag indicating deletion) are excluded.

**Preconditions:**
1. Create a valid product.
2. Delete the product via `DELETE /products/{id}` or set deletion flag.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the deleted product by ID.

**Expected test case results:** HTTP 200. Deleted product does NOT appear in the list.

---

### TC-BE-11 (Negative): Product without globalSalesChannel and missing "Portals" channel is excluded

**Description:** Verify that products without "Portals" in their sales channels are excluded.

**Preconditions:**
1. Create product with `globalSalesChannel: false`, `salesChannelIds`: [some_non_portals_channel_id].
2. All other attributes valid.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product does NOT appear because "Portals" channel is required.

---

### TC-BE-12 (Negative): Product without ALL areas (globalSalesArea=false with specific areas) is excluded

**Description:** Verify that products restricted to specific areas (not ALL) are excluded.

**Preconditions:**
1. Create product with `globalSalesArea: false`, `salesAreasIds`: [specific_area_id].

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because ALL areas are required.

---

### TC-BE-13 (Negative): Product without ALL segments (globalSegment=false) is excluded

**Description:** Verify that products with specific segments (not ALL) are excluded.

**Preconditions:**
1. Create product with `globalSegment: false`, `segmentIds`: [specific_segment_id].

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because ALL segments are required.

---

### TC-BE-14 (Negative): Product with multiple contract types is excluded

**Description:** Verify that products with more than one contract type are excluded.

**Preconditions:**
1. Create product with `contractTypes`: ["COMBINED", "SUPPLY_ONLY"].

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because only single contract type is allowed.

---

### TC-BE-15 (Positive): Product with single contract type is included

**Description:** Verify that products with exactly one contract type are included.

**Preconditions:**
1. Create product with `contractTypes`: ["SUPPLY_ONLY"].
2. All other validations pass.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-16 (Negative): Product with contract term type CERTAIN_DATE is excluded

**Description:** Verify that products with contract term type "CERTAIN_DATE" are excluded.

**Preconditions:**
1. Create product with productTerms containing `typeOfTerms`: "CERTAIN_DATE".

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because CERTAIN_DATE type is not allowed.

---

### TC-BE-17 (Positive): Product with contract term type PERIOD is included

**Description:** Verify that products with contract term type "PERIOD" are included.

**Preconditions:**
1. Create product with productTerms containing `typeOfTerms`: "PERIOD".
2. All other validations pass.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-18 (Positive): Product with contract term type WITHOUT_TERM is included

**Description:** Verify that products with type "without term" are included.

**Preconditions:**
1. Create product with productTerms containing `typeOfTerms`: "WITHOUT_TERM" (or equivalent).

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-19 (Negative): Product with multiple payment terms is excluded

**Description:** Verify that products with more than one payment term are excluded.

**Preconditions:**
1. Create product with multiple entries in `productTerms` array.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because only one payment term is allowed.

---

### TC-BE-20 (Positive): Product with single payment term is included

**Description:** Verify that products with exactly one payment term are included.

**Preconditions:**
1. Create product with single entry in `productTerms`.
2. Term has value filled.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-21 (Negative): Product with multiple payment guarantees selected is excluded

**Description:** Verify that products with more than one payment guarantee checkbox are excluded.

**Preconditions:**
1. Create product with `paymentGuarantees`: ["CASH_DEPOSIT", "BANK"].

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because only one payment guarantee option should be selected.

---

### TC-BE-22 (Positive): Product with single payment guarantee "NO" is included

**Description:** Verify that products with payment guarantee "NO" (no deposit required) are included.

**Preconditions:**
1. Create product with `paymentGuarantees`: ["NO"].

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-23 (Positive): Product with CASH_DEPOSIT and valid amount/currency is included

**Description:** Verify that products with "Cash Deposit" selected and amount+currency filled are included.

**Preconditions:**
1. Create product with `paymentGuarantees`: ["CASH_DEPOSIT"], `cashDepositAmount`: 500, `cashDepositCurrencyId`: valid_currency_id.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list with payment guarantee information.

---

### TC-BE-24 (Negative): Product with CASH_DEPOSIT but missing amount is excluded

**Description:** Verify that products with "Cash Deposit" but no amount are excluded.

**Preconditions:**
1. Create product with `paymentGuarantees`: ["CASH_DEPOSIT"], `cashDepositAmount`: null.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because cash deposit amount is required when CASH_DEPOSIT is selected.

---

### TC-BE-25 (Positive): Product with BANK_GUARANTEE and valid amount/currency is included

**Description:** Verify that products with "Bank Guarantee" and filled amount/currency are included.

**Preconditions:**
1. Create product with `paymentGuarantees`: ["BANK"], `bankGuaranteeAmount`: 1000, `bankGuaranteeCurrencyId`: valid_currency_id.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-26 (Negative): Product with BANK_GUARANTEE but missing amount is excluded

**Description:** Verify that products with "Bank Guarantee" but no amount are excluded.

**Preconditions:**
1. Create product with `paymentGuarantees`: ["BANK"], `bankGuaranteeAmount`: null.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-27 (Positive): Product with CASH_DEPOSIT_AND_BANK and both amounts filled is included

**Description:** Verify that products with both deposit types and all amounts/currencies filled are included.

**Preconditions:**
1. Create product with `paymentGuarantees`: ["CASH_DEPOSIT_AND_BANK"], cashDepositAmount, cashDepositCurrencyId, bankGuaranteeAmount, bankGuaranteeCurrencyId all filled.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-28 (Negative): Product with CASH_DEPOSIT_AND_BANK but missing one amount is excluded

**Description:** Verify that partial amounts with CASH_DEPOSIT_AND_BANK exclude the product.

**Preconditions:**
1. Create product with `paymentGuarantees`: ["CASH_DEPOSIT_AND_BANK"], `cashDepositAmount`: 500, `bankGuaranteeAmount`: null.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-29 (Positive): Product with price component having fixed value is included

**Description:** Verify that products with price components having Value (not value from/to) are included.

**Preconditions:**
1. Create price component with fixed value (e.g., value: 0.235).
2. Create product linked to this price component.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears with price component details.

---

### TC-BE-30 (Negative): Product with price component missing value is excluded

**Description:** Verify that products with price components without value are excluded.

**Preconditions:**
1. Create price component with value: null (or unresolved variable).
2. Create product linked to this price component.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because price component value is required.

---

### TC-BE-31 (Negative): Product with entering into force type 'Exact day' is excluded

**Description:** Verify that products with entering into force type "Exact day" are excluded.

**Preconditions:**
1. Create product with enteringIntoForce configuration having type: "EXACT_DAY".

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-32 (Negative): Product with entering into force type 'Manual' is excluded

**Description:** Verify that products with entering into force type "Manual" are excluded.

**Preconditions:**
1. Create product with enteringIntoForce type: "MANUAL".

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-33 (Positive): Product with entering into force type other than Exact day/Manual is included

**Description:** Verify valid entering into force types allow product inclusion.

**Preconditions:**
1. Create product with enteringIntoForce type: "FIRST_DAY_OF_MONTH" (or similar valid type).

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-34 (Negative): Product with multiple entering into force values is excluded

**Description:** Verify that only single value is allowed for entering into force.

**Preconditions:**
1. Create product with multiple enteringIntoForce entries.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-35 (Negative): Product with start of initial term type 'Exact day' is excluded

**Description:** Verify that products with start of initial term type "Exact day" are excluded.

**Preconditions:**
1. Create product with startOfInitialTerm type: "EXACT_DAY".

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-36 (Negative): Product with start of initial term type 'Manual' is excluded

**Description:** Verify that products with start of initial term type "Manual" are excluded.

**Preconditions:**
1. Create product with startOfInitialTerm type: "MANUAL".

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-37 (Negative): Product with supply activation type 'Exact day' is excluded

**Description:** Verify that products with supply activation after resigning type "Exact day" are excluded.

**Preconditions:**
1. Create product with supplyActivationAfterResigning type: "EXACT_DAY".

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-38 (Positive): Product with supply activation type 'Manual' is included

**Description:** Verify that type "Manual" for supply activation is allowed (per business rules).

**Preconditions:**
1. Create product with supplyActivationAfterResigning type: "MANUAL".

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-39 (Negative): Product with interim payment "at least one selected" is excluded

**Description:** Verify that "at least one is selected" for interim payments excludes the product.

**Preconditions:**
1. Create product with interimAdvancePayments having selection type: "AT_LEAST_ONE".

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-40 (Positive): Product with obligatory interim payment and valid configuration is included

**Description:** Verify that obligatory interim payments with all validations passing are included.

**Preconditions:**
1. Create interim advance payment with: obligatory: true, valueType: "EXACT_AMOUNT", value: 100.
2. Create product linked to this interim payment.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-41 (Negative): Product with obligatory interim payment but missing value is excluded

**Description:** Verify that obligatory interim payments with missing value exclude the product.

**Preconditions:**
1. Create interim payment with valueType: "EXACT_AMOUNT", value: null.
2. Create product linked to this interim payment.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-42 (Negative): Product with interim payment having "Days after invoice date" but no value is excluded

**Description:** Verify payment term "Days after invoice date" requires value.

**Preconditions:**
1. Create interim payment with paymentTerm: "DAYS_AFTER_INVOICE_DATE", value: null.
2. Create product linked to this interim payment.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-43 (Negative): Product with interim payments having different settings is excluded

**Description:** Verify that inconsistent interim payment configurations exclude the product.

**Preconditions:**
1. Create two interim payments with different settings (different value types).
2. Create product linked to both.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded because interim payments have different settings.

---

### TC-BE-44 (Positive): Product without equal monthly installments is included

**Description:** Verify that products with equalMonthlyInstallmentsActivation=false are included without additional checks.

**Preconditions:**
1. Create product with `equalMonthlyInstallmentsActivation`: false.
2. installmentNumber and amount can be null.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-45 (Positive): Product with equal monthly installments enabled and all values filled is included

**Description:** Verify that enabled installments with filled values are included.

**Preconditions:**
1. Create product with `equalMonthlyInstallmentsActivation`: true, `installmentNumber`: 12, `amount`: 100.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-46 (Negative): Product with equal monthly installments enabled but missing installmentNumber is excluded

**Description:** Verify that missing installment number excludes the product.

**Preconditions:**
1. Create product with `equalMonthlyInstallmentsActivation`: true, `installmentNumber`: null.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-47 (Negative): Product with equal monthly installments enabled but missing amount is excluded

**Description:** Verify that missing amount excludes the product.

**Preconditions:**
1. Create product with `equalMonthlyInstallmentsActivation`: true, `installmentNumber`: 12, `amount`: null.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product is excluded.

---

### TC-BE-48 (Negative): Re-signing product is excluded

**Description:** Verify that products marked for re-signing are excluded.

**Preconditions:**
1. Create product with re-signing flag/configuration enabled.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Re-signing product does NOT appear in the list.

---

### TC-BE-49 (Positive): Response contains all required product attributes

**Description:** Verify that the response JSON includes all documented attributes.

**Preconditions:**
1. Create a valid product meeting all requirements.

**Test steps:**
1. Call `POST /products/list`.
2. Find the product in response.
3. Validate presence of: Product ID, Product Version, PrintingName, PrintingName (Transliterated), Text for invoices, Short description, Type of POD, Purpose of consumption, Metering type, Voltage level, Capacity limit, Payment guarantee, Contract type, Contract term details, Price components.

**Expected test case results:** HTTP 200. All required attributes are present in the response for the product. No field is null when value exists.

---

### TC-BE-50 (Positive): Price components filtered by "Active electric energy" or "Fee" type are returned

**Description:** Verify that only price components with Price Type "Active electric energy" OR "Fee" are included in the response.

**Preconditions:**
1. Create multiple price components with different types.
2. Create product linked to all price components.

**Test steps:**
1. Call `POST /products/list`.
2. Find the product and inspect price components array.

**Expected test case results:** HTTP 200. Only price components with type "Active electric energy" or "Fee" appear in the response. Other types are excluded.

---

### TC-BE-51 (Positive): Contract term with automatic renewal details is returned correctly

**Description:** Verify that automatic renewal information is included in contract term response.

**Preconditions:**
1. Create product with productTerms having automaticRenewal: true, renewalPeriodValue: 12, renewalPeriodType: "MONTH".

**Test steps:**
1. Call `POST /products/list`.
2. Find the product and check Contract term section.

**Expected test case results:** HTTP 200. Contract term includes: automaticRenewal=true, renewalValue, renewalType, perpetuityClause value.

---

### TC-BE-52 (Positive): Application model for price components is returned when type is "Price application model over time"

**Description:** Verify application model details are returned for applicable price components.

**Preconditions:**
1. Create price component with application model type: "PRICE_APPLICATION_MODEL_OVER_TIME", period: "MONTHLY", level: "CONTRACT".
2. Create product linked to this price component.

**Test steps:**
1. Call `POST /products/list`.
2. Find the product and inspect price component's Application model.

**Expected test case results:** HTTP 200. Application model includes: ApplicationModel.Period, ApplicationModel.Level, ApplicationModel.ApplicationType.

---

### TC-BE-53 (Positive): Product list returns paginated results

**Description:** Verify that the endpoint supports pagination.

**Preconditions:**
1. Create multiple valid products (e.g., 25 products).

**Test steps:**
1. Call `POST /products/list` with pagination parameters (e.g., page=0, size=10).
2. Call again with page=1.

**Expected test case results:** HTTP 200. First call returns 10 products. Second call returns next set. Response includes totalElements, totalPages, page number.

---

### TC-BE-54 (Negative): Empty list when no products meet criteria

**Description:** Verify empty list is returned when no products qualify.

**Preconditions:**
1. Ensure all products in the system are either INACTIVE, individual, or fail other criteria.

**Test steps:**
1. Call `POST /products/list`.

**Expected test case results:** HTTP 200. Response contains empty content array. totalElements is 0.

---

### TC-BE-55 (Positive): Product with term group (not direct term) is included if group passes validation

**Description:** Verify that products using term groups (termGroupId) instead of direct term are included if the group is valid.

**Preconditions:**
1. Create a term group via `POST /terms-group` containing valid terms.
2. Create product with `termGroupId`: group_id (termId: null).

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list. Term details from the group are displayed.

---

### TC-BE-56 (Positive): Product with price component group is included if group passes validation

**Description:** Verify that products using price component groups are included.

**Preconditions:**
1. Create price component group with valid price components (current version, values filled).
2. Create product with `priceComponentGroupIds`: [group_id].

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list with price components from the group.

---

### TC-BE-57 (Positive): Product with advance payment group is included if group passes validation

**Description:** Verify that products using advance payment groups are included.

**Preconditions:**
1. Create advance payment group with valid configuration (currently active version).
2. Create product with `interimAdvancePaymentGroups`: [group_id].

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product appears in the list.

---

### TC-BE-58 (Positive): NEW status product with valid configuration is handled

**Description:** Verify handling of products with status NEW (may or may not be included based on business rules).

**Preconditions:**
1. Create product with `productStatus`: "NEW", all other attributes valid.

**Test steps:**
1. Call `POST /products/list`.
2. Search for the product in response.

**Expected test case results:** HTTP 200. Product handling matches business requirement (likely excluded as only ACTIVE should appear).

---

---

## References

- **Jira:** GET-PRODUCT-LIST – Get Product List API test cases.
- **Related:** ProductService, ProductRepository (searchForExpressContract, filter).
- **Confluence:** [get product list](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/779517953/get+product+list).
- **Frontend test cases:** `Frontend/Get_product_list_energy_products.md` (TC-FE-1 through TC-FE-12).
