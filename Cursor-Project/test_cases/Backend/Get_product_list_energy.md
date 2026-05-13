# Get Product List (Energy Products) – Sales Portal API (PHN-2178)

**Jira:** PHN-2178 (Phoenix)
**Type:** Task (Story)
**Summary:** The Sales Portal exposes `GET /sales-portal/product/list` to return Energy products eligible for portal sales. This file verifies that the endpoint applies all filtering rules correctly, returns the expected response schema, enforces authentication, and excludes every product that fails even one eligibility condition.

**Scope:** Backend (API) tests for `GET /sales-portal/product/list`. The endpoint requires an OAuth2 bearer token (obtained via `POST /sales-portal/oauth2/token`) and returns `List<ProductListForSalesChannelProjection>`. A product appears only when it simultaneously satisfies Active status, Available for sale, availability window, Portals sales channel (channel id=1 or global), all areas, all segments, not individual, not deleted, not re-signable, exactly one contract type, valid contract term, valid payment guarantee, complete price components, valid Entering into force / Start of initial term / Supply activation, valid interim/advance payment configuration, and valid equal monthly installments. Tests verify all positive inclusions, all negative exclusions, edge/boundary cases, auth guards, response schema, and regression scenarios.

---

## Test data (preconditions)

Shared setup for this file — the baseline "happy-path" product used across positive tests. Tests that need a different state create their own product inline (see individual TC preconditions).

- **Environment:** Test
1. Authenticate with the main Phoenix API using `POST /auth/token` (admin credentials) to obtain a JWT for product/term/price-component management.
2. Authenticate with the Sales Portal using `POST /sales-portal/oauth2/token` (grant_type: client_credentials, CLIENT_ID, CLIENT_SECRET from environment) to obtain the OAuth2 bearer token. Store this token — all test calls to `GET /sales-portal/product/list` use it.
3. Create a **Terms** record via `POST /terms` (payment term type: Period, value: 12, period type: Month, no specific date). Store the terms ID.
4. Create a **Price Component** of type "Активна електрическа енергия" via `POST /price-component` (value type: Fixed, numeric value: 0.15 BGN/kWh, all formula variables filled, linked to the terms from step 3). Store the price component ID.
5. Create a second **Price Component** of type "Такса" via `POST /price-component` (value type: Fixed, numeric value: 5.00 BGN/month, all formula variables filled). Store the second price component ID.
6. Create a third **Price Component** of a type that is **NOT** "Активна електрическа енергия" or "Такса" (e.g. "Мрежова такса") via `POST /price-component`. Store for use in TC-BE-33.
7. Create a **Product** via `POST /product` with the following key attributes:
   - `productStatus`: ACTIVE
   - `availableForSale`: true
   - `globalSalesChannel`: true (makes it available to all channels including Portals)
   - `globalSalesArea`: true (all areas included)
   - `globalSegment`: true (all segments included)
   - `customerIdentifier`: null (not individual)
   - `deleted`: false
   - `isResignable`: false (not re-signable)
   - `contractType`: exactly one option selected (e.g. STANDARD)
   - `contractTermPeriodType`: Period (not Certain date)
   - Payment guarantee: exactly one checkbox — Cash Deposit; `cashDepositAmount`: 100.00; `cashDepositCurrency`: BGN
   - `enteringIntoForce`: exactly one value; type NOT 'Exact day' or 'Manual'
   - `startOfInitialTerm`: exactly one value; type NOT 'Exact day' or 'Manual'
   - `supplyActivation`: exactly one value; type NOT 'Exact day'
   - `availabilityPeriodFrom`: null; `availabilityPeriodTo`: null (both empty → always available)
   - No interim/advance payment obligations
   - Equal monthly installments: not selected
   - Price components: linked to both PC from step 4 and PC from step 5
   - PrintingName: "Тест Продукт 1"; PrintingNameTransliteration: "Test Product 1"
   - InvoiceName: "ТП1"; InvoiceNameTransliteration: "TP1"
   Store the product ID as `baseProductId`.

---

## Backend Test Cases

### TC-BE-1 (Positive): Happy path — fully qualifying product appears in the list

**Description:** Verify that a product satisfying ALL eligibility conditions is returned by `GET /sales-portal/product/list` with the correct response structure and all required field values populated.

**Preconditions:**
1. Complete steps 1–7 from Test data above. The baseline product (`baseProductId`) is active, available for sale, no availability window, global sales channel, all areas, all segments, not individual, not deleted, not re-signable, one contract type, valid contract term, Cash Deposit payment guarantee with amount and currency, both required price component types linked, valid entering-into-force / start-of-initial-term / supply-activation, no interim/advance payments, equal installments not selected.
2. The Sales Portal OAuth2 token from step 2 is valid and unexpired.

**Test steps:**
1. Send `GET /sales-portal/product/list` with header `Authorization: Bearer <token from step 2>`.
2. Inspect the HTTP response status code.
3. Parse the response body as JSON array.
4. Find the element whose `productId` matches `baseProductId`.
5. Assert all mandatory fields are present: `productId`, `productVersion`, `printingName`, `printingNameTransliteration`, `invoiceName`, `invoiceNameTransliteration`, `shortDescription` (may be null), `typeOfPointsOfDelivery`, `purposeOfConsumption`, `meteringType`, `voltageLevel`, `providedCapacityLimit`, `paymentGuarantee`, `contractType`, `contractTerm`, `priceComponents`.
6. Assert `priceComponents` is a valid JSON structure containing at least one entry of type "Активна електрическа енергия" and at least one entry of type "Такса".

**Expected test case results:** HTTP 200 OK. The response is a JSON array. The element for `baseProductId` is present and contains all required fields with non-null values where applicable. `priceComponents` contains entries for both allowed price types and the fields `name`, `invoiceName`, `valueType`, `numberType`, `xs`, `conditions`, `priceWithWords`, `conditionsWithWords` are present for each price component entry.

**References:** PHN-2178; `SalesPortalProductController.getProductListForSalesChannel()`.

---

### TC-BE-2 (Positive): Product with group-based term is included

**Description:** Verify that a product whose contract term is defined via a **term group** (rather than a direct term) passes the term validation rules and appears in the list, provided the group has exactly one active payment term and a filled value.

**Preconditions:**
1. Complete steps 1–2 from Test data (authenticate both APIs).
2. Create a **Term Group** via the terms-group management endpoint (name: "Standard Group", status: ACTIVE, containing exactly one payment term entry with period type: Period, value: 12, Type: Month).
3. Create price components as in steps 3–5 of Test data.
4. Create a product via `POST /product` (ACTIVE, availableForSale: true, globalSalesChannel: true, globalSalesArea: true, globalSegment: true, customerIdentifier: null, contractTermType: GROUP, linked term group: group from step 2; one contract type, valid payment guarantee, valid entering-into-force / start-of-initial-term / supply-activation; price components from steps 3–5 linked).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Parse the response body.
3. Confirm the product from step 4 is present in the list.
4. Confirm `contractTerm` in the response reflects the group term parameters (Type, Value, Renewal, etc.).

**Expected test case results:** HTTP 200. The product with the group term is included in the response array. The `contractTerm` object correctly exposes the group's term details (type of terms, value, type, automatic renewal, renewal value, renewal type, perpetuity clause).

**References:** PHN-2178; term group filtering logic.

---

### TC-BE-3 (Positive): Product with direct term (single payment term, value filled) is included

**Description:** Verify that a product using a **direct term** (not a term group) is included when exactly one payment term is configured and the term value is filled.

**Preconditions:**
1. Complete steps 1–2 from Test data (authenticate).
2. Create Terms via `POST /terms` (direct term, payment term type: Period, value: 24, period type: Month, value filled).
3. Create price components as in steps 4–5 of Test data.
4. Create a product via `POST /product` (ACTIVE, availableForSale: true, globalSalesChannel: true, globalSalesArea: true, globalSegment: true, customerIdentifier: null, contractTermType: DIRECT, linked directly to terms from step 2; exactly one payment term; value = 24 filled; one contract type; valid payment guarantee; valid entering-into-force / start / supply-activation; price components linked).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate the product in the response array.
3. Confirm `contractTerm.value` = 24 and `contractTerm.type` reflects Period.

**Expected test case results:** HTTP 200. Product with direct term appears in the list. The `contractTerm` sub-object correctly represents the direct term configuration.

**References:** PHN-2178; direct term validation.

---

### TC-BE-4 (Positive): Payment guarantee — Cash Deposit with amount and currency

**Description:** Verify that a product configured with the **Cash Deposit** payment guarantee option (exactly one checkbox, `cashDepositAmount` and `cashDepositCurrency` both filled) is included in the list.

**Preconditions:**
1. Complete steps 1–7 from Test data. The baseline product uses Cash Deposit with amount 100 BGN. Confirm no other guarantee checkbox is checked.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId` in the response.
3. Assert `paymentGuarantee` object reflects Cash Deposit type with amount and currency values.

**Expected test case results:** HTTP 200. Product is in the list. `paymentGuarantee` shows Cash Deposit selected, with `cashDepositAmount` = 100 and `cashDepositCurrency` = BGN (or equivalent representation).

**References:** PHN-2178; payment guarantee validation — Cash Deposit.

---

### TC-BE-5 (Positive): Payment guarantee — Bank Guarantee with amount and currency

**Description:** Verify that a product with **Bank Guarantee** (exactly one checkbox, `bankGuaranteeAmount` and `bankGuaranteeCurrency` both filled) is included in the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, availableForSale: true, globalSalesChannel: true, all areas, all segments, not individual, not deleted, not re-signable, one contract type, valid contract term, Payment guarantee: Bank Guarantee ONLY — `bankGuaranteeAmount`: 500.00, `bankGuaranteeCurrency`: BGN, valid entering-into-force / start / supply-activation, price components linked).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product in the response.
3. Assert `paymentGuarantee` reflects Bank Guarantee option with amount and currency.

**Expected test case results:** HTTP 200. Product with Bank Guarantee appears in the list. `paymentGuarantee` shows only Bank Guarantee, with `bankGuaranteeAmount` = 500 and `bankGuaranteeCurrency` = BGN.

**References:** PHN-2178; payment guarantee validation — Bank Guarantee.

---

### TC-BE-6 (Positive): Payment guarantee — Both (Cash Deposit AND Bank Guarantee) with all four fields filled

**Description:** Verify that selecting the **Both** payment guarantee option (Cash Deposit + Bank Guarantee simultaneously) with all four fields (`cashDepositAmount`, `cashDepositCurrency`, `bankGuaranteeAmount`, `bankGuaranteeCurrency`) filled results in the product being included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, availableForSale: true, global channel/area/segment, not individual/deleted/re-signable, one contract type, valid contract term, Payment guarantee: BOTH — `cashDepositAmount`: 200 BGN, `cashDepositCurrency`: BGN, `bankGuaranteeAmount`: 1000 BGN, `bankGuaranteeCurrency`: BGN, valid entering-into-force / start / supply-activation, price components linked).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate the product in the response.
3. Assert all four payment guarantee values are present.

**Expected test case results:** HTTP 200. Product with Both guarantee appears in the list. All four amounts/currencies are returned in `paymentGuarantee`.

**References:** PHN-2178; payment guarantee — Both option, four-field completeness.

---

### TC-BE-7 (Positive): Sales channel — Portals explicitly linked (channel id=1)

**Description:** Verify that a product that is NOT `global_sales_channel` but has the **Portals channel (id=1) explicitly included** in its sales channel list passes the channel filter and appears in the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, availableForSale: true, `globalSalesChannel`: false, sales channels list: [id=1 (Portals)], globalSalesArea: true, globalSegment: true, not individual/deleted/re-signable, one contract type, valid contract term, Cash Deposit payment guarantee, valid entering-into-force / start / supply-activation, price components linked).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears in the response.

**Expected test case results:** HTTP 200. The product with explicit Portals channel linkage (channel id=1) is returned in the list despite `globalSalesChannel` being false.

**References:** PHN-2178; sales channel filter — hardcoded Portals id=1.

---

### TC-BE-8 (Positive): Sales channel — product with global_sales_channel flag

**Description:** Verify that a product with `global_sales_channel = true` is treated as available to all channels including Portals and appears in the list.

**Preconditions:**
1. Complete steps 1–7 from Test data. The baseline product uses `globalSalesChannel: true`.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm `baseProductId` appears in the response.

**Expected test case results:** HTTP 200. Product with `globalSalesChannel = true` is returned. This is the complement test to TC-BE-7, confirming both channel linkage paths work.

**References:** PHN-2178; global sales channel flag.

---

### TC-BE-9 (Positive): Availability window — both from and to defined, current date is within range

**Description:** Verify that a product with both `availabilityPeriodFrom` and `availabilityPeriodTo` set is included when the current date falls strictly between the two dates.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set:
   - `availabilityPeriodFrom`: 2020-01-01 (well in the past)
   - `availabilityPeriodTo`: 2099-12-31 (well in the future)
   (The current date is guaranteed to be between these dates.)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears in the response.

**Expected test case results:** HTTP 200. Product is included — the current date falls within the defined availability window.

**References:** PHN-2178; availability window — both from/to defined and current date in range.

---

### TC-BE-10 (Positive): Availability window — only from defined, current date is after from

**Description:** Verify that a product with only `availabilityPeriodFrom` set (no to) is included when the current date is after the from date.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set:
   - `availabilityPeriodFrom`: 2020-01-01
   - `availabilityPeriodTo`: null

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears in the response.

**Expected test case results:** HTTP 200. Product included — only `from` is set and current date is past the from date.

**References:** PHN-2178; availability window — only from set.

---

### TC-BE-11 (Positive): Availability window — only to defined, current date is before to

**Description:** Verify that a product with only `availabilityPeriodTo` set (no from) is included when the current date is before the to date.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set:
   - `availabilityPeriodFrom`: null
   - `availabilityPeriodTo`: 2099-12-31

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears in the response.

**Expected test case results:** HTTP 200. Product included — only `to` is set and current date is before the to date.

**References:** PHN-2178; availability window — only to set.

---

### TC-BE-12 (Positive): Availability window — both from and to are null (always available)

**Description:** Verify that a product with no availability window defined (both `availabilityPeriodFrom` and `availabilityPeriodTo` are null) is always included, regardless of the current date.

**Preconditions:**
1. Complete steps 1–7 from Test data. The baseline product has both null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm `baseProductId` is in the response.

**Expected test case results:** HTTP 200. Product with no availability window restriction is always returned.

**References:** PHN-2178; availability window — both null (no restriction).

---

### TC-BE-13 (Positive): Contract term with perpetuity clause enabled

**Description:** Verify that a product whose contract term includes a **perpetuity clause** (automatic indefinite renewal with no end date) passes all term validations and appears in the list. The `perpetuityClause` flag must be returned in the `contractTerm` response object.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` configured as in step 7 of Test data, but with contract term:
   - `contractTermPeriodType`: Period
   - `automaticRenewal`: true
   - `perpetuityClause`: true
   - One payment term with filled value
3. Confirm all other eligibility conditions are met.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product in the response.
3. Inspect `contractTerm.perpetuityClause`.

**Expected test case results:** HTTP 200. Product is included. `contractTerm.perpetuityClause` = true in the response.

**References:** PHN-2178; contract term perpetuity clause.

---

### TC-BE-14 (Positive): Both required price component types ("Активна електрическа енергия" AND "Такса") returned in priceComponents

**Description:** Verify that when a product has price components of both allowed types, the `priceComponents` field in the response contains exactly those two types and excludes all other types.

**Preconditions:**
1. Complete steps 1–7 from Test data. The baseline product has price components of type "Активна електрическа енергия" (step 4) and "Такса" (step 5). A third price component of type "Мрежова такса" (step 6) is also linked to `baseProductId`.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId` in the response.
3. Extract `priceComponents` array.
4. Assert the array contains exactly entries for types "Активна електрическа енергия" and "Такса".
5. Assert the array does NOT contain any entry of type "Мрежова такса" or any other non-allowed type.

**Expected test case results:** HTTP 200. `priceComponents` for `baseProductId` contains entries with types "Активна електрическа енергия" and "Такса" only. Non-allowed price component types are filtered out even if linked to the product.

**References:** PHN-2178; price type filter — "Активна електрическа енергия" and "Такса" only.

---

### TC-BE-15 (Positive): Price components assigned directly to the product are returned

**Description:** Verify that price components **directly assigned** to a product (not via group or advance payment) are included in the `priceComponents` response field when they have all required values filled.

**Preconditions:**
1. Complete steps 1–7 from Test data. The baseline product has price components directly assigned.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId`. Assert `priceComponents` is non-empty and each entry has `name`, `invoiceName`, `valueType`, `numberType` populated.

**Expected test case results:** HTTP 200. Directly-assigned price components appear in `priceComponents`.

**References:** PHN-2178; direct price component assignment.

---

### TC-BE-16 (Positive): Price components via active price-component group are returned

**Description:** Verify that price components assigned to a product **through a currently active price-component group** are included in `priceComponents`, provided all group row values are filled.

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Create an active **price-component group** via the price-component group management endpoint (status: ACTIVE, containing one row of type "Активна електрическа енергия" with value filled, and one row of type "Такса" with value filled).
3. Create a product via `POST /product` (all eligibility conditions met; price components linked via the active group from step 2; no direct price component assignment).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product. Confirm `priceComponents` lists the group's rows.

**Expected test case results:** HTTP 200. Product is included and `priceComponents` contains the group's active price component rows.

**References:** PHN-2178; price component group rows included.

---

### TC-BE-17 (Positive): Price components via advance payments are returned

**Description:** Verify that when price components are provided through **advance payment** configuration, they are included in `priceComponents` when all values are filled.

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Create Terms and Price Components of the allowed types with values filled via advance payment routes.
3. Create a product via `POST /product` (all eligibility conditions met; price components configured via advance payment mechanism; advance payments mode is Obligatory, not "at least one"; all value/date/term sub-fields filled).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product. Confirm `priceComponents` is non-empty.

**Expected test case results:** HTTP 200. Product is included and `priceComponents` contains the advance-payment-sourced price component entries.

**References:** PHN-2178; advance payment price components.

---

### TC-BE-18 (Positive): Interim and advance payment with same settings, all Obligatory — product included

**Description:** Verify that a product with both **interim** and **advance** payments configured in Obligatory mode, and where both have identical settings (matching Value, Date of issue, Payment term), is included in the list. Differing settings would exclude the product; identical settings are valid.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all standard eligibility conditions met) with:
   - Interim payment: Obligatory; value type: exact amount; value: 50 BGN; Date of issue: match invoice date; Payment term: Matches with term of standard invoice.
   - Advance payment: Obligatory; value type: exact amount; value: 50 BGN; Date of issue: match invoice date; Payment term: Matches with term of standard invoice.
   (Interim and advance settings are identical.)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears in the response.

**Expected test case results:** HTTP 200. Product with matching interim/advance Obligatory settings is included.

**References:** PHN-2178; interim/advance payment — same settings rule.

---

### TC-BE-19 (Positive): Obligatory value type "% from previous invoice amount" with value filled — included

**Description:** Verify that when interim or advance payment mode is Obligatory and the value type is **"% from previous invoice amount"**, having the percentage value filled causes the product to be included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: "% from previous invoice amount"; value: 10 (percent); Date of issue: match invoice date; Payment term: Matches with term of standard invoice.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — Obligatory with "%" and value filled.

**References:** PHN-2178; advance payment Obligatory "%" branch.

---

### TC-BE-20 (Positive): Obligatory value type "exact amount" with value filled — included

**Description:** Verify that Obligatory advance/interim payment with value type **"exact amount"** and the amount value filled results in the product being included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: exact amount; value: 200 BGN; Date of issue: match invoice date; Payment term: Matches with term of standard invoice.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — Obligatory exact amount with value filled.

**References:** PHN-2178; advance payment Obligatory exact-amount branch.

---

### TC-BE-21 (Positive): Obligatory value type "price component" with price component linked — included

**Description:** Verify that Obligatory advance/interim payment with value type **"price component"** and a price component actually linked results in the product being included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: price component; price component: linked to price component from step 4 of Test data (non-null); Date of issue: match invoice date; Payment term: Matches with term of standard invoice.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — Obligatory price-component type with PC linked.

**References:** PHN-2178; advance payment Obligatory price-component branch.

---

### TC-BE-22 (Positive): Date of issue "match invoice date" — included

**Description:** Verify that Obligatory advance/interim payment with Date of issue = **"match invoice date"** is a valid configuration that does not require an additional value, and the product is included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: exact amount; value: 100 BGN; Date of issue: match invoice date; Payment term: Matches with term of standard invoice.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — "match invoice date" requires no extra value and is valid.

**References:** PHN-2178; Date of issue "match invoice date" — no extra value required.

---

### TC-BE-23 (Positive): Date of issue "Periodical" — included

**Description:** Verify that **Periodical** date of issue is a valid option that does not require an additional numeric value, and the product is included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: exact amount; value: 100 BGN; Date of issue: Periodical; Payment term: Matches with term of standard invoice.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — "Periodical" date of issue is valid.

**References:** PHN-2178; Date of issue "Periodical".

---

### TC-BE-24 (Positive): Payment term "Matches with term of standard invoice" — no separate value required, included

**Description:** Verify that when the payment term is set to **"Matches with term of standard invoice"**, no additional payment term value is required and the product is included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: exact amount; value: 100 BGN; Date of issue: match invoice date; Payment term: Matches with term of standard invoice (no extra value field required).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — payment term "Matches" is valid without additional value.

**References:** PHN-2178; payment term "Matches with standard invoice".

---

### TC-BE-25 (Positive): "Days after invoice date" advance payment directly — Value defined, Value From/To empty — included

**Description:** Verify the specific rule: when Date of issue is "Days after invoice date" and advance payment is configured directly (not via a group), the product is included only when `Value` is defined and `ValueFrom` and `ValueTo` are both empty/null.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: exact amount; value: 100 BGN; Date of issue: "Days after invoice date"; `dateOfIssueValue` (days): 5; `dateOfIssueValueFrom`: null; `dateOfIssueValueTo`: null; Payment term: Matches with term of standard invoice.
   - Advance payment is directly assigned (not via group).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — "Days after invoice date" with defined Value and empty From/To is valid.

**References:** PHN-2178; "Days after invoice date" — Value defined, From/To empty rule.

---

### TC-BE-26 (Positive): Equal monthly installments — Number of installments AND Amount both filled — included

**Description:** Verify that when the **Equal monthly installments** option is selected, having both `numberOfInstallments` and `installmentAmount` filled results in the product being included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - `equalMonthlyInstallments`: selected/true
   - `numberOfInstallments`: 12
   - `installmentAmount`: 100.00 BGN

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product with fully configured equal monthly installments is included.

**References:** PHN-2178; equal monthly installments — fully filled.

---

### TC-BE-27 (Positive): Automatic renewal enabled on contract term — field returned in response

**Description:** Verify that a product with `automaticRenewal = true` on the contract term is included in the list and the `contractTerm` response object correctly reflects the renewal configuration (renewal value, renewal type).

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Contract term: `automaticRenewal`: true; `renewalValue`: 12; `renewalType`: Month.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product. Inspect `contractTerm.automaticRenewal`, `contractTerm.renewalValue`, `contractTerm.renewalType`.

**Expected test case results:** HTTP 200. Product is included. `contractTerm.automaticRenewal = true`, `renewalValue = 12`, `renewalType = Month`.

**References:** PHN-2178; contract term renewal fields.

---

### TC-BE-28 (Positive): Automatic renewal disabled — renewalValue and renewalType absent or null

**Description:** Verify that a product with `automaticRenewal = false` is included and the renewal-specific fields are null or absent in the response.

**Preconditions:**
1. Complete steps 1–7 from Test data. The baseline product has `automaticRenewal: false` (default).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId`. Inspect `contractTerm.automaticRenewal` and related renewal fields.

**Expected test case results:** HTTP 200. Product is included. `contractTerm.automaticRenewal = false`; `renewalValue` and `renewalType` are null or absent.

**References:** PHN-2178; contract term — renewal disabled.

---

### TC-BE-29 (Positive): ApplicationModel "over time" present — Period, Level, ApplicationType returned in priceComponents

**Description:** Verify that when a price component is configured with **Application model "over time"**, the response includes the `applicationModel` sub-object with `period`, `level`, and `applicationType` fields.

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Create a price component of type "Активна електрическа енергия" with ApplicationModel type set to "Price application model over time" and `period`, `level`, `applicationType` all filled.
3. Create a product via `POST /product` (all eligibility conditions met; this price component linked; also link a "Такса" price component).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product. In `priceComponents`, find the "Активна електрическа енергия" entry.
3. Assert `applicationModel.period`, `applicationModel.level`, `applicationModel.applicationType` are present and non-null.

**Expected test case results:** HTTP 200. Product is included. The price component entry for "Активна електрическа енергия" contains an `applicationModel` sub-object with all three fields populated.

**References:** PHN-2178; ApplicationModel "Price application model over time" — Period/Level/ApplicationType.

---

### TC-BE-30 (Positive): Multiple qualifying products — all returned in the list

**Description:** Verify that when multiple products satisfy all eligibility conditions simultaneously, the endpoint returns all of them in the response array. Confirm no qualifying product is missing.

**Preconditions:**
1. Complete steps 1–7 from Test data (baseline product `baseProductId`).
2. Create two additional fully qualifying products (products P2 and P3) using the same approach as step 7, with different `printingName` values. Store their product IDs.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Parse the response JSON array.
3. Assert `baseProductId`, P2 ID, and P3 ID are all present in the array.
4. Assert the array length includes at least three elements.

**Expected test case results:** HTTP 200. All three qualifying products are present in the list. No qualifying product is missing from the response.

**References:** PHN-2178; multi-product list response.

---

### TC-BE-31 (Positive): Empty response — no qualifying products exist

**Description:** Verify that when no products satisfy all eligibility conditions (e.g. all products are inactive or none have the Portals channel), the endpoint returns an empty JSON array (not an error).

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Confirm (or arrange via a dedicated environment) that no active, available-for-sale product with Portals channel exists in the environment at the time of the test — or create only products that fail the eligibility check on purpose. (This TC may be run in an isolated environment or after ensuring all qualifying products are inactive.)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Parse the response body.

**Expected test case results:** HTTP 200. The response body is an empty JSON array `[]`. No error occurs — an empty list is a valid and expected outcome when no products qualify.

**References:** PHN-2178; empty-list edge case.

---

### TC-BE-32 (Positive): Response schema — all expected fields present with correct data types

**Description:** Verify that the response object for each product element contains every field documented in the `ProductListForSalesChannelProjection` specification, with the correct data types (string, number, boolean, object, array).

**Preconditions:**
1. Complete steps 1–7 from Test data. `baseProductId` fully qualifies.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId` in the response.
3. For each of the following fields, assert presence and data type:
   - `productId` (integer/number)
   - `productVersion` (integer/number)
   - `printingName` (string)
   - `printingNameTransliteration` (string or null)
   - `invoiceName` (string)
   - `invoiceNameTransliteration` (string or null)
   - `shortDescription` (string or null)
   - `typeOfPointsOfDelivery` (string or enum)
   - `purposeOfConsumption` (string or enum)
   - `meteringType` (string or enum)
   - `voltageLevel` (string or enum)
   - `providedCapacityLimit` (number or null)
   - `paymentGuarantee` (object)
   - `contractType` (string or object)
   - `contractTerm` (object with `typeOfTerms`, `value`, `type`, `automaticRenewal`, `renewalValue`, `renewalType`, `perpetuityClause`)
   - `priceComponents` (array)
4. Within each `priceComponents` entry assert: `name`, `invoiceName`, `valueType`, `numberType`, `xs` (array), `conditions` (array), `priceWithWords`, `conditionsWithWords`.

**Expected test case results:** HTTP 200. All listed fields are present. Data types match specification. No unexpected null where a value is required. `priceComponents` is a valid JSON array (not a string).

**References:** PHN-2178; `ProductListForSalesChannelProjection` schema specification.

---

### TC-BE-33 (Positive): priceComponents array contains ONLY the two allowed price types

**Description:** Verify that even when a product has price components of types other than "Активна електрическа енергия" and "Такса", only those two types appear in the `priceComponents` field of the response. Non-allowed types must be filtered out at the SQL/query level.

**Preconditions:**
1. Complete steps 1–7 from Test data. `baseProductId` has three price components: "Активна електрическа енергия" (step 4), "Такса" (step 5), and "Мрежова такса" (step 6).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId`. Extract `priceComponents`.
3. Assert that for every entry in `priceComponents`, the price type is either "Активна електрическа енергия" or "Такса".
4. Assert no entry with type "Мрежова такса" exists in `priceComponents`.
5. Assert exactly two type-distinct entries are in the array (one per allowed type).

**Expected test case results:** HTTP 200. `priceComponents` contains only entries for the two allowed types. The "Мрежова такса" component is absent from the response even though it is linked to the product.

**References:** PHN-2178; price type filter in SQL query.

---

### TC-BE-34 (Edge): Boundary — current date equals availabilityPeriodFrom (inclusive lower bound)

**Description:** Verify the boundary case where `availabilityPeriodFrom` equals today's date exactly. The rule states: if only `from` is defined, `current > from` — this means the product is included only if current is strictly greater than `from`. If the rule uses `>=` (current >= from), the product is included on the boundary. Confirm the actual behavior.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set:
   - `availabilityPeriodFrom`: today's date (e.g. 2026-04-23)
   - `availabilityPeriodTo`: null
3. Note today's date at test runtime.

**Test steps:**
1. Send `GET /sales-portal/product/list` on the same day as `availabilityPeriodFrom`.
2. Check if this product appears in the response.

**Expected test case results:** The product behavior on the boundary date is documented. Based on the filtering rule ("current > from"), the product is expected to be **excluded** on the exact `from` date if the comparison is strict, or **included** if `>=` is used. The actual HTTP 200 response is inspected and the boundary behavior is recorded. (This test confirms the exact boundary semantics of the SQL query.)

**References:** PHN-2178; availability window boundary — from date equals current date.

---

### TC-BE-35 (Edge): Boundary — current date equals availabilityPeriodTo (inclusive upper bound)

**Description:** Verify the boundary case where `availabilityPeriodTo` equals today's date exactly. Rule: "current < to" — confirm behavior on the boundary day.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set:
   - `availabilityPeriodFrom`: null
   - `availabilityPeriodTo`: today's date (e.g. 2026-04-23)

**Test steps:**
1. Send `GET /sales-portal/product/list` on the same day as `availabilityPeriodTo`.
2. Check if this product appears in the response.

**Expected test case results:** The product behavior on the upper boundary date is documented. Based on "current < to" (strict), the product is expected to be **excluded** on the exact `to` date. If `<=` is used it is included. The actual result confirms the SQL operator semantics.

**References:** PHN-2178; availability window boundary — to date equals current date.

---

### TC-BE-36 (Edge): Single qualifying product — list has exactly one element

**Description:** Verify that when only one product qualifies at the time of the call, the response is a single-element JSON array (not an object, not a two-element array).

**Preconditions:**
1. Arrange an environment (or create a product in an isolated state) so that `baseProductId` is the only qualifying product at the time of the call. All other products in the environment are inactive or unavailable for sale.
2. Obtain a valid Sales Portal bearer token.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Parse the response.
3. Assert the response is a JSON array with exactly one element.
4. Assert the element is the qualifying product.

**Expected test case results:** HTTP 200. Response is `[{...}]` — a single-element array. The product object is complete and valid.

**References:** PHN-2178; single-element list edge case.

---

### TC-BE-37 (Edge): Unicode characters in PrintingName and PrintingNameTransliteration — returned correctly

**Description:** Verify that a product whose `PrintingName` contains Cyrillic characters (e.g. "Тест Продукт Кирилица") and whose `PrintingNameTransliteration` contains the Latin transliteration are serialized correctly in the JSON response without corruption or escaping errors.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set:
   - `printingName`: "Тест Продукт — Кирилица 100%"
   - `printingNameTransliteration`: "Test Product - Kirilitsa 100%"
   - `invoiceName`: "ТП-Кирилица"
   - `invoiceNameTransliteration`: "TP-Kirilitsa"

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product.
3. Assert `printingName` = "Тест Продукт — Кирилица 100%" (byte-for-byte correct).
4. Assert `printingNameTransliteration` = "Test Product - Kirilitsa 100%".

**Expected test case results:** HTTP 200. All Cyrillic characters are returned intact. Transliteration is correct. No JSON encoding issues.

**References:** PHN-2178; Unicode/Cyrillic character handling.

---

### TC-BE-38 (Edge): Transliteration fields null — returned as null in response

**Description:** Verify that when transliteration fields (`printingNameTransliteration`, `invoiceNameTransliteration`) are null/not set on the product, they are returned as `null` in the JSON response (not absent or empty string).

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set `printingNameTransliteration`: null and `invoiceNameTransliteration`: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product.
3. Assert `printingNameTransliteration` is present in the JSON and its value is `null`.
4. Assert `invoiceNameTransliteration` is present in the JSON and its value is `null`.

**Expected test case results:** HTTP 200. Transliteration fields are present in the response with `null` values.

**References:** PHN-2178; null optional fields — transliteration.

---

### TC-BE-39 (Edge): Price component with multiple Xs entries — all returned in priceComponents

**Description:** Verify that a price component configured with multiple **Xs** (description + value pairs) returns all Xs entries in the `xs` array of the `priceComponents` field.

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Create a price component of type "Активна електрическа енергия" with three Xs entries: (Description: "Zone 1", Value: 0.10), (Description: "Zone 2", Value: 0.12), (Description: "Zone 3", Value: 0.15).
3. Create a product via `POST /product` (all eligibility conditions met; this price component linked; also link a "Такса" price component).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product. Find the "Активна електрическа енергия" entry in `priceComponents`.
3. Assert `xs` is an array of length 3. Assert each entry has a `description` and `value`.

**Expected test case results:** HTTP 200. All three Xs entries are returned in the `xs` array. No Xs entry is truncated or missing.

**References:** PHN-2178; Xs (description + value) — multiple entries.

---

### TC-BE-40 (Edge): Price component Conditions with multiple operators (=, <>, IN) — all returned

**Description:** Verify that when a price component has multiple Conditions, each with a different operator (equals `=`, not-equals `<>`, and `IN`), all conditions and their operators are returned in the `conditions` array of the response.

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Create a price component of type "Активна електрическа енергия" with three Conditions:
   - (Parameter: "Voltage", Value: "0.4", Operator: "=")
   - (Parameter: "Region", Value: "Sofia", Operator: "<>")
   - (Parameter: "Zone", Value: "A,B,C", Operator: "IN")
3. Create a product via `POST /product` (all eligibility conditions met; this PC linked; "Такса" PC also linked).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product. Find the "Активна електрическа енергия" entry in `priceComponents`.
3. Assert `conditions` is an array of length 3.
4. Assert each condition has `parameter`, `value`, and `operator` fields with correct values.

**Expected test case results:** HTTP 200. All three conditions with their respective operators (`=`, `<>`, `IN`) are returned correctly.

**References:** PHN-2178; Conditions — multiple operators.

---

### TC-BE-41 (Edge): ApplicationModel absent — field not included in priceComponents entry

**Description:** Verify that when a price component does NOT have an ApplicationModel configured, the `applicationModel` field is either absent or null in the `priceComponents` entry (no error and no empty object returned).

**Preconditions:**
1. Complete steps 1–7 from Test data. The baseline product's price components do not have ApplicationModel configured.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId`. Inspect the `priceComponents` entries.
3. Assert that `applicationModel` is either null or absent in each entry (not an empty object `{}`).

**Expected test case results:** HTTP 200. Price component entries without ApplicationModel either omit the `applicationModel` field entirely or set it to `null`. No malformed structure.

**References:** PHN-2178; ApplicationModel optional field — absent case.

---

### TC-BE-42 (Edge): Short description null — field is null in response

**Description:** Verify that `shortDescription` is returned as `null` in the response when a product has no short description configured.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set `shortDescription`: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product. Assert `shortDescription` is present and equals `null`.

**Expected test case results:** HTTP 200. `shortDescription` = `null` in the response element.

**References:** PHN-2178; optional null fields.

---

### TC-BE-43 (Edge): Provided capacity null — field is null in response

**Description:** Verify that `providedCapacityLimit` is returned as `null` when not configured on the product.

**Preconditions:**
1. Complete steps 1–7 from Test data. The baseline product has `providedCapacityLimit`: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId`. Assert `providedCapacityLimit` is `null`.

**Expected test case results:** HTTP 200. `providedCapacityLimit = null` in the response.

**References:** PHN-2178; providedCapacityLimit — null case.

---

### TC-BE-44 (Edge): Provided capacity = 0 — returned as 0, product not excluded

**Description:** Verify that a product with `providedCapacityLimit = 0` is included in the list (zero is a valid numeric value, not a disqualifying null) and the value 0 is returned in the response.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set `providedCapacityLimit`: 0.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate this product. Assert `providedCapacityLimit` = 0.

**Expected test case results:** HTTP 200. Product is included. `providedCapacityLimit = 0` in the response.

**References:** PHN-2178; providedCapacityLimit = 0 boundary.

---

### TC-BE-45 (Edge): JSON special characters in PrintingName — correctly escaped in response

**Description:** Verify that when `PrintingName` contains special JSON characters (double quotes, backslashes, control characters), the JSON response is properly escaped and parseable.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set:
   - `printingName`: `Тест "Продукт" — 100% / Special`

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm the response is valid parseable JSON.
3. Locate this product. Assert `printingName` contains the embedded double quotes correctly.

**Expected test case results:** HTTP 200. Response JSON is valid and parseable. `printingName` contains the exact original string including special characters.

**References:** PHN-2178; JSON escaping of special characters.

---

### TC-BE-46 (Edge): Date of issue "Date of the month" with value filled — included

**Description:** Verify that advance/interim payment with Date of issue = **"Date of the month"** and the day-of-month value filled (e.g. 15) results in the product being included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: exact amount; value: 100 BGN; Date of issue: "Date of the month"; `dateOfIssueValue`: 15; Payment term: Matches with term of standard invoice.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — "Date of the month" with value 15 filled is valid.

**References:** PHN-2178; Date of issue "Date of the month" with value.

---

### TC-BE-47 (Edge): Date of issue "Working days after invoice date" with value filled — included

**Description:** Verify that advance/interim payment with Date of issue = **"Working days after invoice date"** and the number of working days filled results in the product being included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - Advance payment: Obligatory; value type: exact amount; value: 100 BGN; Date of issue: "Working days after invoice date"; `dateOfIssueValue`: 3; Payment term: Matches with term of standard invoice.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — "Working days after invoice date" with value 3 filled is valid.

**References:** PHN-2178; Date of issue "Working days after invoice date".

---

### TC-BE-48 (Edge): Supply activation "First day of month following date of signing" with single "Wait for old contract term to expire" value — included

**Description:** Verify the specific rule: when Supply activation type is **"First day of month following date of signing"**, the **"Wait for old contract term to expire"** field must have exactly one value configured. When this is correctly set, the product is included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all eligibility conditions met) with:
   - `supplyActivationType`: "First day of month following date of signing"
   - `waitForOldContractTermToExpire`: exactly one value configured (e.g. true)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Confirm this product appears.

**Expected test case results:** HTTP 200. Product included — "First day of month following date of signing" with exactly one "Wait for old contract term" value is valid.

**References:** PHN-2178; supply activation "First day of month" + wait-for-old-contract rule.

---

### TC-BE-49 (Negative): Missing Authorization header — 401 Unauthorized

**Description:** Verify that calling `GET /sales-portal/product/list` **without** any `Authorization` header results in a 401 Unauthorized response. No product data is returned.

**Preconditions:**
1. The Sales Portal endpoint is reachable.
2. No Authorization header is prepared for this test.

**Test steps:**
1. Send `GET /sales-portal/product/list` with NO `Authorization` header.
2. Inspect the HTTP response status code and response body.

**Expected test case results:** HTTP 401 Unauthorized. The response body contains an error message indicating missing or invalid credentials. No product list is returned.

**References:** PHN-2178; OAuth2 bearer token — missing header guard.

---

### TC-BE-50 (Negative): Invalid/malformed JWT token — 401 Unauthorized

**Description:** Verify that an invalid or malformed bearer token (e.g. a random string that is not a valid JWT) results in a 401 Unauthorized response.

**Preconditions:**
1. Prepare an invalid bearer token string (e.g. "Bearer INVALID_TOKEN_ABCDEF123").

**Test steps:**
1. Send `GET /sales-portal/product/list` with header `Authorization: Bearer INVALID_TOKEN_ABCDEF123`.
2. Inspect the HTTP response status code.

**Expected test case results:** HTTP 401 Unauthorized. No product data is returned.

**References:** PHN-2178; OAuth2 bearer token — invalid token guard.

---

### TC-BE-51 (Negative): Expired JWT token — 401 Unauthorized

**Description:** Verify that an expired bearer token (one that was valid at some point but whose expiry timestamp has passed) results in a 401 Unauthorized response.

**Preconditions:**
1. Obtain a valid Sales Portal bearer token via `POST /sales-portal/oauth2/token`.
2. Wait for the token to expire (or obtain a token with a very short TTL, or use a pre-expired token from a known past session).

**Test steps:**
1. Send `GET /sales-portal/product/list` with the expired token in the `Authorization` header.
2. Inspect the HTTP response status code.

**Expected test case results:** HTTP 401 Unauthorized. The response body indicates the token is expired or no longer valid. No product data is returned.

**References:** PHN-2178; OAuth2 bearer token — expired token guard.

---

### TC-BE-52 (Negative): Valid token but incorrect OAuth2 client scope — 403 Forbidden

**Description:** Verify that a token obtained via a client that does not have permission to access the Sales Portal product list endpoint results in a 403 Forbidden response (or 401, depending on scope enforcement).

**Preconditions:**
1. Obtain an OAuth2 token using a client with a different scope (e.g. a client intended for a different portal or service), if available in the environment.
2. Alternatively, modify the token's scope claim and re-sign (if the environment allows this for testing).

**Test steps:**
1. Send `GET /sales-portal/product/list` with the wrong-scope token.
2. Inspect the HTTP response status code.

**Expected test case results:** HTTP 403 Forbidden (or 401, depending on how the authorization server enforces scope). No product data is returned.

**References:** PHN-2178; OAuth2 scope enforcement.

---

### TC-BE-53 (Negative): Inactive product — excluded from the list

**Description:** Verify that a product with `productStatus = INACTIVE` does not appear in the `GET /sales-portal/product/list` response, even if all other conditions are satisfied.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` with all eligibility conditions met except:
   - `productStatus`: INACTIVE
3. Store this product's ID as `inactiveProductId`.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Parse the response array.
3. Search for an element where `productId` = `inactiveProductId`.

**Expected test case results:** HTTP 200. The response does NOT contain `inactiveProductId`. Inactive product is excluded.

**References:** PHN-2178; Active status filter — INACTIVE product excluded.

---

### TC-BE-54 (Negative): Product not available for sale — excluded

**Description:** Verify that a product with `availableForSale = false` does not appear in the list, even if it is ACTIVE and satisfies all other conditions.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, all other conditions met) with:
   - `availableForSale`: false

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response array.

**Expected test case results:** HTTP 200. Product with `availableForSale = false` is absent from the list.

**References:** PHN-2178; Available for sale filter.

---

### TC-BE-55 (Negative): Availability window — current date is outside the from-to range — excluded

**Description:** Verify that a product with a defined availability window where the current date falls outside the range (before `from` or after `to`) is excluded from the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product as in step 7 of Test data but set:
   - `availabilityPeriodFrom`: 2099-01-01 (far future — current date is before this)
   - `availabilityPeriodTo`: 2099-12-31

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with availability window starting in the far future is absent from the list (current date is before `from`).

**References:** PHN-2178; availability window outside range.

---

### TC-BE-56 (Negative): No Portals channel and not global_sales_channel — excluded

**Description:** Verify that a product that has `globalSalesChannel = false` and does NOT have the Portals channel (id=1) in its sales channel list is excluded from the response.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, availableForSale: true, all other conditions met) with:
   - `globalSalesChannel`: false
   - `salesChannels`: [] (empty) or a list containing only a non-Portals channel (e.g. id=2).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product without Portals channel access is absent from the list.

**References:** PHN-2178; sales channel filter — no Portals channel.

---

### TC-BE-57 (Negative): Areas NOT ALL (partial area assignment) — excluded

**Description:** Verify that a product configured for specific geographic areas (not all areas) is excluded from the list because the rule requires ALL areas to be included.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, availableForSale: true, global channel, all segments, all other conditions met) with:
   - `globalSalesArea`: false
   - `salesAreas`: [specific area ID only] (not all areas)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product limited to specific areas is absent from the list.

**References:** PHN-2178; areas filter — ALL required.

---

### TC-BE-58 (Negative): Segments NOT ALL (partial segment assignment) — excluded

**Description:** Verify that a product assigned to only specific customer segments (not all segments) is excluded from the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, availableForSale: true, global channel, global area, all other conditions met) with:
   - `globalSegment`: false
   - `segments`: [one specific segment ID] (not all segments)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product limited to specific segments is absent from the list.

**References:** PHN-2178; segments filter — ALL required.

---

### TC-BE-59 (Negative): Individual product (customer_identifier not null) — excluded

**Description:** Verify that a product with `customerIdentifier` set to a specific customer ID (making it an individual/custom product) is excluded from the general product list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a customer via `POST /customer` (type: PRIVATE, status: ACTIVE). Store `customerId`.
3. Create a product via `POST /product` (ACTIVE, availableForSale: true, global channel/area/segment, all other conditions met) with:
   - `customerIdentifier`: `customerId` (not null — this makes it individual)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Individual product (customer_identifier IS NOT NULL) is absent from the list.

**References:** PHN-2178; individual product filter — customer_identifier IS NULL required.

---

### TC-BE-60 (Negative): Deleted product — excluded

**Description:** Verify that a product marked as deleted (`deleted = true`) does not appear in the list, even if all other conditions would qualify it.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, availableForSale: true, global channel/area/segment, all other conditions met).
3. Delete the product via the appropriate delete endpoint (or set `deleted = true` via the product update endpoint). Confirm deletion.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert the deleted product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Deleted product is absent from the list.

**References:** PHN-2178; deleted product filter — not deleted required.

---

### TC-BE-61 (Negative): Re-signable flag true — excluded

**Description:** Verify that a product with `isResignable = true` (or equivalent re-signing flag set) is excluded from the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (ACTIVE, availableForSale: true, global channel/area/segment, all other conditions met) with:
   - `isResignable`: true

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Re-signable product is absent from the list.

**References:** PHN-2178; re-signing flag filter.

---

### TC-BE-62 (Negative): Contract type — two options selected — excluded

**Description:** Verify that a product with **more than one contract type option selected** does not appear in the list. The rule requires exactly one contract type to be selected.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `contractTypes`: [STANDARD, FIXED] (two contract type options selected simultaneously)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with two contract types is absent from the list.

**References:** PHN-2178; contract type — exactly one option required.

---

### TC-BE-63 (Negative): Contract term — more than one payment term configured — excluded

**Description:** Verify that a product whose contract term has **more than one payment term** (e.g. two concurrent term options) is excluded from the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Contract term: two separate payment terms configured simultaneously (e.g. Period 12 months AND Period 24 months both selected).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with multiple payment terms is absent from the list.

**References:** PHN-2178; contract term — exactly one payment term required.

---

### TC-BE-64 (Negative): Contract term period type "Certain date" — excluded

**Description:** Verify that a product with `contractTermPeriodType = "Certain date"` is excluded. The rule requires `contract_term_period_type` to be one of (Period, Without term, Other) — not "Certain date".

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `contractTermPeriodType`: "Certain date"

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with contract term period type "Certain date" is absent from the list.

**References:** PHN-2178; contract term period type filter — Certain date excluded.

---

### TC-BE-65 (Negative): Direct term — value (term length) missing — excluded

**Description:** Verify that a product with a direct term where the term **Value** field is not filled (null or zero) is excluded from the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Direct term; one payment term; `value`: null (or not filled).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with unfilled direct term value is absent from the list.

**References:** PHN-2178; direct term — value required.

---

### TC-BE-66 (Negative): Payment guarantee — no checkbox selected — excluded

**Description:** Verify that a product with zero payment guarantee options selected (none of Cash Deposit, Bank Guarantee, or Both is checked) is excluded from the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `paymentGuarantee`: no option selected / null / all false.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with no payment guarantee option is absent from the list.

**References:** PHN-2178; payment guarantee — exactly one checkbox required.

---

### TC-BE-67 (Negative): Cash Deposit selected but amount missing — excluded

**Description:** Verify that a product with Cash Deposit as the payment guarantee but `cashDepositAmount` missing (null) is excluded from the list.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Payment guarantee: Cash Deposit; `cashDepositAmount`: null; `cashDepositCurrency`: BGN.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Cash Deposit product with missing amount is absent.

**References:** PHN-2178; Cash Deposit — amount required.

---

### TC-BE-68 (Negative): Cash Deposit selected but currency missing — excluded

**Description:** Verify that Cash Deposit with amount filled but `cashDepositCurrency` missing (null) causes the product to be excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Payment guarantee: Cash Deposit; `cashDepositAmount`: 100; `cashDepositCurrency`: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Cash Deposit product with missing currency is absent.

**References:** PHN-2178; Cash Deposit — currency required.

---

### TC-BE-69 (Negative): Bank Guarantee selected but amount missing — excluded

**Description:** Verify that Bank Guarantee with `bankGuaranteeAmount` null causes the product to be excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Payment guarantee: Bank Guarantee; `bankGuaranteeAmount`: null; `bankGuaranteeCurrency`: BGN.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Bank Guarantee product with missing amount is absent.

**References:** PHN-2178; Bank Guarantee — amount required.

---

### TC-BE-70 (Negative): Bank Guarantee selected but currency missing — excluded

**Description:** Verify that Bank Guarantee with `bankGuaranteeCurrency` null causes the product to be excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Payment guarantee: Bank Guarantee; `bankGuaranteeAmount`: 500; `bankGuaranteeCurrency`: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Bank Guarantee product with missing currency is absent.

**References:** PHN-2178; Bank Guarantee — currency required.

---

### TC-BE-71 (Negative): Both (Cash+Bank) selected but one of the four fields missing — excluded

**Description:** Verify that when the **Both** payment guarantee option is selected, any one of the four required fields (`cashDepositAmount`, `cashDepositCurrency`, `bankGuaranteeAmount`, `bankGuaranteeCurrency`) being null causes the product to be excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Payment guarantee: Both; `cashDepositAmount`: null; `cashDepositCurrency`: BGN; `bankGuaranteeAmount`: 500; `bankGuaranteeCurrency`: BGN.
   (Cash amount is missing.)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Both-guarantee product with one missing field is absent.

**References:** PHN-2178; Both guarantee — all four fields required.

---

### TC-BE-72 (Negative): Price component with null formula variable value (direct assignment) — excluded

**Description:** Verify that a product is excluded when any directly assigned price component has at least one **formula variable value that is null** (the NOT EXISTS check on null formula variable values).

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Create a price component of type "Активна електрическа енергия" where one formula variable value is left null (the formula is defined but the variable value is not populated).
3. Create a product via `POST /product` (all other conditions met; this incomplete PC linked).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with incomplete price component (null formula variable) is absent.

**References:** PHN-2178; NOT EXISTS null formula variable values.

---

### TC-BE-73 (Negative): Entering into force — type "Exact day" — excluded

**Description:** Verify that a product with Entering into force type set to **"Exact day"** is excluded from the list. The rule explicitly forbids this type.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `enteringIntoForceType`: "Exact day"

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with entering-into-force type "Exact day" is absent.

**References:** PHN-2178; entering into force — "Exact day" forbidden.

---

### TC-BE-74 (Negative): Entering into force — type "Manual" — excluded

**Description:** Verify that a product with Entering into force type set to **"Manual"** is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `enteringIntoForceType`: "Manual"

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with entering-into-force type "Manual" is absent.

**References:** PHN-2178; entering into force — "Manual" forbidden.

---

### TC-BE-75 (Negative): Entering into force — more than one value configured — excluded

**Description:** Verify that a product with more than one entering-into-force value (e.g. two concurrent options configured) is excluded. The rule requires exactly one value.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `enteringIntoForce`: two values configured simultaneously.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with multiple entering-into-force values is absent.

**References:** PHN-2178; entering into force — exactly one value required.

---

### TC-BE-76 (Negative): Start of initial term — type "Exact day" — excluded

**Description:** Verify that a product with Start of initial term type "Exact day" is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `startOfInitialTermType`: "Exact day"

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with start-of-initial-term "Exact day" is absent.

**References:** PHN-2178; start of initial term — "Exact day" forbidden.

---

### TC-BE-77 (Negative): Start of initial term — type "Manual" — excluded

**Description:** Verify that a product with Start of initial term type "Manual" is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `startOfInitialTermType`: "Manual"

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with start-of-initial-term "Manual" is absent.

**References:** PHN-2178; start of initial term — "Manual" forbidden.

---

### TC-BE-78 (Negative): Start of initial term — more than one value configured — excluded

**Description:** Verify that a product with multiple start-of-initial-term values configured simultaneously is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with two start-of-initial-term values configured.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with multiple start-of-initial-term values is absent.

**References:** PHN-2178; start of initial term — exactly one value required.

---

### TC-BE-79 (Negative): Supply activation — type "Exact day" — excluded

**Description:** Verify that a product with Supply activation type "Exact day" is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `supplyActivationType`: "Exact day"

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with supply activation type "Exact day" is absent.

**References:** PHN-2178; supply activation — "Exact day" forbidden.

---

### TC-BE-80 (Negative): "First day of month following date of signing" without exactly one "Wait for old contract term to expire" value — excluded

**Description:** Verify that when Supply activation is **"First day of month following date of signing"** and the "Wait for old contract term to expire" field has zero values (not configured), the product is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `supplyActivationType`: "First day of month following date of signing"
   - `waitForOldContractTermToExpire`: null or not configured (zero values)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with "First day of month" supply activation but missing "wait" value is absent.

**References:** PHN-2178; supply activation "First day of month" — requires single "Wait" value.

---

### TC-BE-81 (Negative): Interim/advance payment — "at least one is selected" mode — excluded

**Description:** Verify that when the interim/advance payment configuration is set to **"at least one is selected"** (optional), the product is excluded. Only if all are Obligatory is the product eligible.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Advance payment mode: "at least one is selected" (optional/mixed mode, not Obligatory for all).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with "at least one" advance payment mode is absent.

**References:** PHN-2178; interim/advance — "at least one is selected" → exclude.

---

### TC-BE-82 (Negative): Obligatory with "% from previous invoice amount" but value missing — excluded

**Description:** Verify that when advance/interim payment is Obligatory with value type "% from previous invoice amount" but the **percentage value is null**, the product is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Advance payment: Obligatory; value type: "% from previous invoice amount"; value: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with Obligatory "%" but no value is absent.

**References:** PHN-2178; Obligatory "%" — value required.

---

### TC-BE-83 (Negative): Obligatory with "exact amount" but value missing — excluded

**Description:** Verify that Obligatory advance/interim payment with value type "exact amount" and value = null causes the product to be excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Advance payment: Obligatory; value type: exact amount; value: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with Obligatory exact amount and null value is absent.

**References:** PHN-2178; Obligatory exact amount — value required.

---

### TC-BE-84 (Negative): Obligatory with "price component" value type but no price component linked — excluded

**Description:** Verify that when value type is "price component" (Obligatory) but no price component is actually linked, the product is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Advance payment: Obligatory; value type: price component; linked price component: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with Obligatory price-component type and no PC is absent.

**References:** PHN-2178; Obligatory price-component — PC required.

---

### TC-BE-85 (Negative): Date of issue "Date of the month" but day value missing — excluded

**Description:** Verify that when Date of issue is "Date of the month" and the day-of-month value is null, the product is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Advance payment: Obligatory; Date of issue: "Date of the month"; `dateOfIssueValue`: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with "Date of the month" and no day value is absent.

**References:** PHN-2178; Date of issue "Date of the month" — value required.

---

### TC-BE-86 (Negative): Date of issue "Working days after invoice date" but value missing — excluded

**Description:** Verify that "Working days after invoice date" with `dateOfIssueValue` = null causes the product to be excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Advance payment: Obligatory; Date of issue: "Working days after invoice date"; `dateOfIssueValue`: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with "Working days" and null value is absent.

**References:** PHN-2178; Date of issue "Working days" — value required.

---

### TC-BE-87 (Negative): Payment term added but term value (days/period) missing — excluded

**Description:** Verify that when a payment term is configured for advance/interim payments but the term value (number of days or period length) is not filled, the product is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Advance payment: Obligatory; Date of issue: match invoice date; Payment term: configured (not "Matches with standard invoice") with term value: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with payment term set but value missing is absent.

**References:** PHN-2178; payment term — value required when term type is not "Matches".

---

### TC-BE-88 (Negative): "Days after invoice date" with ValueFrom and/or ValueTo set — excluded

**Description:** Verify the specific rule: "Days after invoice date" with advance payment configured directly — when `ValueFrom` or `ValueTo` is set (non-null), the product is excluded even if `Value` is also present.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Advance payment: Obligatory; Date of issue: "Days after invoice date"; `dateOfIssueValue`: 5; `dateOfIssueValueFrom`: 1; `dateOfIssueValueTo`: 10; direct (not via group).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with "Days after invoice date" and From/To values set is absent.

**References:** PHN-2178; "Days after invoice date" — ValueFrom/ValueTo must be empty.

---

### TC-BE-89 (Negative): Interim and advance payment have differing settings — excluded

**Description:** Verify that when both interim and advance payments are Obligatory but have **different settings** (e.g. different value types or different date of issue configurations), the product is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - Interim payment: Obligatory; value type: exact amount; value: 100; Date of issue: match invoice date.
   - Advance payment: Obligatory; value type: "% from previous invoice amount"; value: 10%; Date of issue: "Date of the month"; dayValue: 15.
   (Differing value types and date-of-issue settings.)

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with differing interim/advance settings is absent.

**References:** PHN-2178; interim vs advance — differing settings → exclude.

---

### TC-BE-90 (Negative): Equal monthly installments selected but number of installments missing — excluded

**Description:** Verify that when Equal monthly installments is enabled but `numberOfInstallments` is null, the product is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `equalMonthlyInstallments`: true; `numberOfInstallments`: null; `installmentAmount`: 100.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with missing number of installments is absent.

**References:** PHN-2178; equal monthly installments — number required.

---

### TC-BE-91 (Negative): Equal monthly installments selected but installment amount missing — excluded

**Description:** Verify that when Equal monthly installments is enabled but `installmentAmount` is null, the product is excluded.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product via `POST /product` (all other conditions met) with:
   - `equalMonthlyInstallments`: true; `numberOfInstallments`: 12; `installmentAmount`: null.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert this product's ID is NOT in the response.

**Expected test case results:** HTTP 200. Product with missing installment amount is absent.

**References:** PHN-2178; equal monthly installments — amount required.

---

### TC-BE-92 (Regression): Sales channel hardcoded id=1 — verify Portals channel identity

**Description:** Regression check that the Portals sales channel identifier used in the SQL filter is exactly **id=1** and has not been renumbered. If the Portals channel ID is ever changed in the nomenclature table, products linked to the old ID would incorrectly appear or disappear.

**Preconditions:**
1. Complete steps 1–2 (auth) and steps 3–5 (terms + price components) from Test data.
2. Create a product with `globalSalesChannel: false` and `salesChannels: [id=1]` (explicit Portals link). Store as `portalsLinkedProduct`.
3. Also verify (via database or admin query) that the sales channel with id=1 is named "Portals" in the system nomenclature.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Assert `portalsLinkedProduct`'s ID is in the response (it has the correct Portals channel id=1).
3. Create a separate product with `salesChannels: [id=2]` (a different channel, not Portals) and `globalSalesChannel: false`. Assert it is NOT in the response.

**Expected test case results:** HTTP 200. Only the product with the correct Portals channel (id=1) appears. The channel id mapping is stable and consistent with the system nomenclature.

**References:** PHN-2178; regression — Portals channel id=1 hardcoded in SQL filter.

---

### TC-BE-93 (Regression): Price type filter — verify exact nomenclature names match

**Description:** Regression check that the price component type filter uses the **exact string values** "Активна електрическа енергия" and "Такса". If these nomenclature names are ever renamed in the system, the filter would fail to include valid price components.

**Preconditions:**
1. Complete steps 1–7 from Test data. `baseProductId` has price components of both required types.
2. Verify (via admin query) that the price type nomenclature values are exactly "Активна електрическа енергия" and "Такса" (correct spelling, casing, and encoding).

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Locate `baseProductId`. Inspect the `priceComponents` array.
3. Assert at least one entry exists for each of the two allowed type names.
4. Assert no price component of a renamed/incorrect type appears.

**Expected test case results:** HTTP 200. The `priceComponents` filter works correctly with the exact nomenclature names. Both types are present in the response.

**References:** PHN-2178; regression — exact price type names "Активна електрическа енергия" / "Такса".

---

### TC-BE-94 (Regression): PHN-2187 alignment — Sales Portal product list consistent with POD list product data

**Description:** Regression check that the product data returned by `GET /sales-portal/product/list` (PHN-2178) is consistent with product data exposed via the Sales Portal POD-related endpoints (PHN-2187). The same product ID, version, and key attributes should be identical across both endpoints. This guards against divergence between the two Sales Portal views.

**Preconditions:**
1. Complete steps 1–7 from Test data. `baseProductId` is active and eligible.
2. Obtain valid Sales Portal token.

**Test steps:**
1. Send `GET /sales-portal/product/list` with valid bearer token.
2. Extract `productId`, `productVersion`, `printingName` for `baseProductId`.
3. Send `GET /sales-portal/pod/list` (or equivalent PHN-2187 endpoint) and retrieve product details for the same product.
4. Compare product fields between both endpoint responses.

**Expected test case results:** HTTP 200 on both calls. `productId`, `productVersion`, and `printingName` match between the product-list and POD-list responses. No discrepancy in shared product fields between the two endpoints.

**References:** PHN-2178; PHN-2187; regression — cross-endpoint product data alignment.

---

## References

- **Jira:** PHN-2178 – Get product list (Energy products) — Sales Portal API.
- **Endpoint:** `GET /sales-portal/product/list` — `SalesPortalProductController.getProductListForSalesChannel()`.
- **Auth:** `POST /sales-portal/oauth2/token` — OAuth2 client credentials.
- **Response type:** `List<ProductListForSalesChannelProjection>`.
- **Related:** PHN-2187 (Sales Portal POD list); Sales Portal product catalog; price component groups; payment guarantee validation.
- **Cross-dependency data:** Provided by CrossDependencyFinderAgent for PHN-2178.
