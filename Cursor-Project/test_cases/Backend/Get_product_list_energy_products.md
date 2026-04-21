# Get Product List (Energy Products) – Sales Portal product catalog (PHN-2178)

**Jira:** PHN-2178 (Phoenix Phase 2)
**Type:** Story
**Summary:** The `GET /product/list` endpoint returns all energy products available for sale via the Sales Portal, applying strict filtering and validation rules per the Confluence specification.

**Scope:** When a customer visits the Sales Portal, they should see only products that are fully configured, active, available for sale via "Portals" sales channel, have ALL areas and ALL segments, use fixed parameters only, and are not individual, deleted, or re-signing products. The endpoint must validate every sub-object (contract type, contract term, terms, payment guarantee, price components, entering into force, start of initial term, supply activation after resigning, advance payments, equal monthly installments) and exclude any product with ambiguous or incomplete configuration.

---

## Test data (preconditions)

Shared setup for this file (environment + entity creation chain). The endpoint under test is `GET /product/list` which returns products from the Phoenix product catalog. To test various scenarios, products must be pre-created with different configurations.

- **Environment:** Test

1. Create a product type (nomenclature) via `POST /product-types` (name: "Електроснабдяване", status: ACTIVE) — this represents the energy product type.
2. Create a sales channel nomenclature entry via the nomenclature endpoint (name: "Portals", status: ACTIVE).
3. Create a standard product via `POST /products` with:
   - Status: ACTIVE
   - Available for sale: true
   - Sales channels: include "Portals" (from step 2)
   - Areas: ALL
   - Segments: ALL
   - Product type: energy product type from step 1
   - Individual: false
   - Deleted: false
   - Period from: 2025-01-01, Period to: 2027-12-31 (current date within range)
   - Contract type: exactly one (e.g., SUPPLY_ONLY)
   - Contract term: one payment term, type: Period (not "Certain date"), value filled
   - Payment guarantee: one option selected (e.g., "Cash Deposit" with amount: 500, currency: BGN)
   - Entering into force: one value, type not "Exact day" or "Manual"
   - Start of initial term: one value, type not "Exact day" or "Manual"
   - Supply activation after contract resigning: one value, type not "Exact day"
   - Re-signing: not marked as re-signing
4. Create terms for the product via `POST /terms` (linked to product from step 3; one payment term with value filled).
5. Create price components via `POST /price-component` (linked to product from step 3; value filled, type: "Active electric energy" or "Fee"; price component added directly or via active group version).
6. Configure advance payments for the product (if applicable): obligatory, with value type "exact amount" and value filled, date of issue "Match the invoice date", payment term "Matches with the term of the standard invoice".
7. Configure equal monthly installments: checkbox NOT selected (default).

---

## Backend Test Cases

### TC-BE-1 (Positive): Happy path — return fully configured standard product available via Portals

**Description:** Verify that a standard product meeting all filtering requirements is returned in the `GET /product/list` response with correct attributes.

**Preconditions:**
1. Complete steps 1–7 from Test data above.
2. Product is ACTIVE, available for sale, has "Portals" in sales channels, ALL areas, ALL segments, fixed parameters, not individual, not deleted, not re-signing.

**Test steps:**
1. Send `GET /product/list` request.
2. Parse the response JSON array and locate the product created in preconditions (match by Product ID).

**Expected test case results:** Response returns HTTP 200. The product from preconditions appears in the list. Response contains all required attributes: Product ID, Product Version, PrintingName, Printing name (Transliterated), Text to show in invoices and templates, Text to show in invoices and templates (Transliteration), Short description, Type of points of delivery, Purpose of consumption, Metering type of the points of delivery, Voltage level, Provided capacity limit in kWh, Payment guarantee, Contract type, Contract term array, and Price components array.

**References:** PHN-2178, Confluence page "get product list" (779517953).

---

### TC-BE-2 (Positive): Verify contract term data structure in response

**Description:** Verify that the contract term section in the response contains all required sub-fields: Type of terms, Value, Type, Automatic renewal, Renewal value, Renewal type, Perpetuity clause.

**Preconditions:**
1. Complete steps 1–7 from Test data above.
2. Product has contract term configured with: type FIXED_TERM, value 12, type MONTH, automatic renewal true, renewal value 12, renewal type MONTH, perpetuity clause false.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response and inspect the "Contract term" array.

**Expected test case results:** Response returns HTTP 200. The "Contract term" array for the product contains exactly one entry with all fields populated: Type of terms = "FIXED_TERM", Value = 12, Type = "MONTH", Automatic renewal = true, Renewal value = 12, Renewal type = "MONTH", Perpetuity clause = false.

**References:** PHN-2178, Confluence "get product list" — Response Parameters.

---

### TC-BE-3 (Positive): Verify price components filtered by Price Type

**Description:** Verify that the response includes only price components where Price Type is "Активна електрическа енергия" (Active electric energy) OR "Такса" (Fee), with all sub-attributes populated.

**Preconditions:**
1. Complete steps 1–7 from Test data above.
2. Product has three price components: one with Price Type "Active electric energy", one with Price Type "Fee", and one with Price Type "Grid" (a type that should NOT appear in the response).
3. Each price component has: Name, Name to show in invoices and templates, Value type, Number type, formula X values (Description, Value), Conditions (Parameter, Value, Operator), Price with words, Conditions with words.
4. The "Active electric energy" price component additionally has an Application model with type "Price application model over time" including Period, Level, ApplicationType.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product and inspect the "Price components" array.

**Expected test case results:** Response returns HTTP 200. Only price components with Price Type "Active electric energy" and "Fee" are present in the response. The "Grid" type price component is excluded. Each returned price component contains: Name, Name to show in invoices and templates, Value type, Number type, X descriptions and values, Conditions with Parameter/Value/Operator, Price with words, Conditions with words. The "Active electric energy" component additionally has an Application model with Period, Level, and ApplicationType.

**References:** PHN-2178, Confluence "get product list" — Response Parameters, section 6.

---

### TC-BE-4 (Positive): Product with period from defined and current date after period from

**Description:** Verify that a product with only "Period from" defined (Period to empty) is returned when the current date is after Period from.

**Preconditions:**
1. Create a product per steps 1–7 from Test data, but set Period from = 2025-01-01, Period to = empty/null.
2. Current date is after 2025-01-01.

**Test steps:**
1. Send `GET /product/list` request.
2. Check whether the product appears in the response.

**Expected test case results:** Response returns HTTP 200. The product is included in the list because the current date is greater than Period from and Period to is not defined.

**References:** PHN-2178, Confluence "get product list" — section 4, status requirements.

---

### TC-BE-5 (Positive): Product with period to defined and current date before period to

**Description:** Verify that a product with only "Period to" defined (Period from empty) is returned when the current date is before Period to.

**Preconditions:**
1. Create a product per steps 1–7 from Test data, but set Period from = empty/null, Period to = 2027-12-31.
2. Current date is before 2027-12-31.

**Test steps:**
1. Send `GET /product/list` request.
2. Check whether the product appears in the response.

**Expected test case results:** Response returns HTTP 200. The product is included in the list because the current date is less than Period to and Period from is not defined.

**References:** PHN-2178, Confluence "get product list" — section 4, status requirements.

---

### TC-BE-6 (Positive): Product with empty period from/to

**Description:** Verify that a product with both Period from and Period to empty is returned regardless of the current date.

**Preconditions:**
1. Create a product per steps 1–7 from Test data, but set Period from = empty/null, Period to = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Check whether the product appears in the response.

**Expected test case results:** Response returns HTTP 200. The product is included in the list because no date restrictions are defined.

**References:** PHN-2178, Confluence "get product list" — section 4, status requirements.

---

### TC-BE-7 (Positive): Product with term assigned via group — no additional filtering

**Description:** Verify that when a term group is assigned to the product (rather than a term added directly), the product is returned without additional term validation.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Assign a term group to the product (group contains multiple terms with varying configurations).
3. No term is added directly to the product.

**Test steps:**
1. Send `GET /product/list` request.
2. Check whether the product appears in the response.

**Expected test case results:** Response returns HTTP 200. The product is included. When a term group is assigned, the system displays the group directly without applying the single-payment-term or value-filled validation.

**References:** PHN-2178, Confluence "get product list" — section 4, Term requirements.

---

### TC-BE-8 (Positive): Product with payment guarantee "Bank Guarantee" correctly configured

**Description:** Verify that a product with "Bank Guarantee" selected and bank guarantee amount + currency filled is returned.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Payment guarantee = "Bank Guarantee", bank guarantee amount = 1000, bank guarantee currency = BGN.
2. Only the "Bank Guarantee" checkbox is selected (no other payment guarantee checkboxes).

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included. The "Payment guarantee" field in the response shows "BANK_GUARANTEE".

**References:** PHN-2178, Confluence "get product list" — section 4, Payment guarantee.

---

### TC-BE-9 (Positive): Product with payment guarantee "Cash Deposit and Bank Guarantee" fully configured

**Description:** Verify that a product with "Cash Deposit and Bank Guarantee" selected and all amounts/currencies filled is returned.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Payment guarantee = "Cash Deposit and Bank Guarantee", cash deposit amount = 500, cash deposit currency = BGN, bank guarantee amount = 1000, bank guarantee currency = BGN.
2. Only the "Cash Deposit and Bank Guarantee" checkbox is selected.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included in the list with correct payment guarantee configuration.

**References:** PHN-2178, Confluence "get product list" — section 4, Payment guarantee.

---

### TC-BE-10 (Positive): Product with supply activation type "Manual" — returned directly

**Description:** Verify that a product with supply activation after contract resigning type set to "Manual" is returned without additional checks.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Supply activation after contract resigning type = "Manual".
2. Only one value is configured for supply activation.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included. Per the specification, when supply activation type is "Manual", the product is returned directly.

**References:** PHN-2178, Confluence "get product list" — section 4, Supply activation after contract resigning.

---

### TC-BE-11 (Positive): Product with supply activation "From first day of month" and single "Wait for old contract" value

**Description:** Verify that a product with supply activation type "From first day of the month following the date of signing" and exactly one value for "Wait for old contract term to expire" is returned.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Supply activation type = "From first day of the month following the date of signing of the contract", "Wait for old contract term to expire (for resigning)" has exactly one value selected.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included in the list.

**References:** PHN-2178, Confluence "get product list" — section 4, Supply activation after contract resigning.

---

### TC-BE-12 (Positive): Product with advance payment obligatory — all sub-validations pass

**Description:** Verify that a product with obligatory advance payments is returned when all advance payment sub-validations pass (value type with value filled, date of issue filled, payment term filled).

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add an advance payment to the product (directly or via currently active group): obligatory = true.
3. Value type = "exact amount", value = 100 BGN.
4. Date of issue = "Date of the month", value = 15.
5. Payment term value = 30 days.
6. No "at least one is selected" flag — all advance payments are obligatory with consistent settings.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included because all advance payment validations pass: value type has a defined value, date of issue has a defined value, payment term has a defined value.

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-13 (Positive): Product with advance payment value type "price component" and price component filled

**Description:** Verify that a product with advance payment value type "price component" is returned when the price component reference is filled.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add an advance payment: obligatory = true, value type = "price component", price component = reference to existing price component from step 5.
3. Date of issue = "Match the invoice date" (no additional check needed).
4. Payment term = "Matches with the term of the standard invoice" (no additional check needed).

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included because the advance payment uses value type "price component" with a filled price component reference.

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-14 (Positive): Product with equal monthly installments selected and values filled

**Description:** Verify that a product with equal monthly installments checkbox selected, number of installments filled, and amount value filled is returned.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Equal monthly installments = selected/true, number of installments = 12, amount value = 50 BGN.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included because equal monthly installments is selected and both number of installments and amount value are filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Equal monthly installments.

---

### TC-BE-15 (Positive): Product with equal monthly installments NOT selected — returned without additional checks

**Description:** Verify that a product with equal monthly installments checkbox NOT selected is returned without checking installment values.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Equal monthly installments = not selected/false, number of installments = empty, amount value = empty.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included in the list. The system does not check installment values when the checkbox is not selected.

**References:** PHN-2178, Confluence "get product list" — section 4, Equal monthly installments.

---

### TC-BE-16 (Positive): Product with price components from active group version — returned correctly

**Description:** Verify that price components added via a group of price components (current active version) are validated and the product is returned when all price component values are filled.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Instead of adding price components directly, create a price component group with an active version containing price components with values filled.
3. Link the group to the product.

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included. Price components from the currently active group version are considered during validation.

**References:** PHN-2178, Confluence "get product list" — section 4, Price component values.

---

### TC-BE-17 (Positive): Product with advance payment added via currently active group

**Description:** Verify that advance payments added via groups (currently active version) are validated correctly and the product is returned when valid.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Instead of adding advance payments directly, create an advance payment group (currently active version) linked to the product.
3. Advance payment in the group: obligatory, value type = "% from previous invoice amount", value filled, date of issue = "Periodical" (no additional check), payment term = "Matches with the term of the standard invoice".

**Test steps:**
1. Send `GET /product/list` request.
2. Locate the product in the response.

**Expected test case results:** Response returns HTTP 200. Product is included. Advance payments from the currently active group version pass all validation checks.

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-18 (Negative): Inactive product is excluded from the list

**Description:** Verify that a product with status NOT ACTIVE is excluded from the `GET /product/list` response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set status: INACTIVE (or DRAFT).
2. All other configurations are valid (available for sale, Portals channel, ALL areas/segments, etc.).

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The inactive product does NOT appear in the list.

**References:** PHN-2178, Confluence "get product list" — section 4, Status requirement.

---

### TC-BE-19 (Negative): Product not available for sale is excluded

**Description:** Verify that a product with "Available for sale" flag set to false is excluded from the response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set Available for sale = false.
2. Product is ACTIVE with all other configurations valid.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear in the list because it is not available for sale.

**References:** PHN-2178, Confluence "get product list" — section 4, "Available for sale" requirement.

---

### TC-BE-20 (Negative): Product without "Portals" sales channel is excluded

**Description:** Verify that a product that does not include "Portals" in its sales channels is excluded from the response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set sales channels to include only "Direct Sales" (not "Portals").
2. Product is ACTIVE, available for sale, ALL areas, ALL segments.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear in the list because "Portals" is not included in its sales channels.

**References:** PHN-2178, Confluence "get product list" — section 4, sales channel requirement.

---

### TC-BE-21 (Negative): Product without ALL areas is excluded

**Description:** Verify that a product where areas are restricted (not ALL) is excluded from the response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set areas to a specific region (not ALL).
2. Product is ACTIVE, available for sale, has "Portals" sales channel, ALL segments.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because ALL areas is required.

**References:** PHN-2178, Confluence "get product list" — section 4, "All areas" requirement.

---

### TC-BE-22 (Negative): Product without ALL segments is excluded

**Description:** Verify that a product where segments are restricted (not ALL) is excluded from the response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set segments to a specific segment (not ALL).
2. Product is ACTIVE, available for sale, has "Portals" sales channel, ALL areas.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because ALL segments is required.

**References:** PHN-2178, Confluence "get product list" — section 4, "All segments" requirement.

---

### TC-BE-23 (Negative): Individual product is excluded

**Description:** Verify that a product marked as "individual" is excluded from the response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set individual = true.
2. All other configurations are valid.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The individual product does NOT appear in the list.

**References:** PHN-2178, Confluence "get product list" — section 4, "Products are not individual" requirement.

---

### TC-BE-24 (Negative): Deleted product is excluded

**Description:** Verify that a deleted product is excluded from the response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Delete the product (set deleted flag or status to DELETED).

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The deleted product does NOT appear in the list.

**References:** PHN-2178, Confluence "get product list" — section 4, "not deleted" requirement.

---

### TC-BE-25 (Negative): Product with multiple contract types is excluded

**Description:** Verify that a product with more than one contract type selected is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but select two contract types (e.g., SUPPLY_ONLY and COMBINED).

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the spec requires "Only one option is selected" for contract type.

**References:** PHN-2178, Confluence "get product list" — section 4, Contract type.

---

### TC-BE-26 (Negative): Product with multiple payment terms is excluded

**Description:** Verify that a product with more than one payment term in its contract term is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but configure two payment terms in the contract term.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the spec requires the product to have only one payment term.

**References:** PHN-2178, Confluence "get product list" — section 4, Contract term.

---

### TC-BE-27 (Negative): Product with contract term type "Certain date" is excluded

**Description:** Verify that a product with contract term type "Certain date" is excluded from the response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set contract term type = "Certain date".

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the spec states "If type is certain date — do not return such product."

**References:** PHN-2178, Confluence "get product list" — section 4, Contract term.

---

### TC-BE-28 (Negative): Product with term added directly but no value filled is excluded

**Description:** Verify that a product with a term added directly (not via group) where the payment term value is not filled is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add a term directly to the product (not via group): one payment term, but value = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because when a term is added directly, the value must be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Term requirements.

---

### TC-BE-29 (Negative): Product with multiple payment guarantee checkboxes selected is excluded

**Description:** Verify that a product with more than one payment guarantee checkbox selected is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but select both "Cash Deposit" and "Bank Guarantee" checkboxes separately (not the combined option).

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the spec requires "Only one checkbox should be selected" for payment guarantee.

**References:** PHN-2178, Confluence "get product list" — section 4, Payment guarantee.

---

### TC-BE-30 (Negative): Product with "Cash Deposit" but amount/currency missing is excluded

**Description:** Verify that a product with "Cash Deposit" payment guarantee selected but cash deposit amount or currency not filled is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Payment guarantee = "Cash Deposit", cash deposit amount = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because "Cash Deposit" requires amount and currency to be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Payment guarantee.

---

### TC-BE-31 (Negative): Product with "Bank Guarantee" but amount/currency missing is excluded

**Description:** Verify that a product with "Bank Guarantee" payment guarantee selected but bank guarantee amount or currency not filled is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Payment guarantee = "Bank Guarantee", bank guarantee amount = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because "Bank Guarantee" requires amount and currency to be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Payment guarantee.

---

### TC-BE-32 (Negative): Product with price component missing value is excluded

**Description:** Verify that a product where any price component (directly added, via active group, or via advance payment) has no value filled is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add a price component directly to the product but leave its value empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because all price component values must be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Price component values.

---

### TC-BE-33 (Negative): Product with entering into force type "Exact day" is excluded

**Description:** Verify that a product with entering into force type set to "Exact day" is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set entering into force type = "Exact day".
2. Only one entering into force value.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the spec states "If type is 'Exact day' or 'Manual' — do not return such product" for entering into force.

**References:** PHN-2178, Confluence "get product list" — section 4, Entering into force.

---

### TC-BE-34 (Negative): Product with entering into force type "Manual" is excluded

**Description:** Verify that a product with entering into force type set to "Manual" is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set entering into force type = "Manual".

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear.

**References:** PHN-2178, Confluence "get product list" — section 4, Entering into force.

---

### TC-BE-35 (Negative): Product with multiple entering into force values is excluded

**Description:** Verify that a product with more than one entering into force value is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but configure two entering into force values.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the spec requires "Check if only one value is added" for entering into force.

**References:** PHN-2178, Confluence "get product list" — section 4, Entering into force.

---

### TC-BE-36 (Negative): Product with start of initial term type "Exact day" is excluded

**Description:** Verify that a product with start of initial term type set to "Exact day" is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set start of initial term type = "Exact day".

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear.

**References:** PHN-2178, Confluence "get product list" — section 4, Start of the initial term.

---

### TC-BE-37 (Negative): Product with start of initial term type "Manual" is excluded

**Description:** Verify that a product with start of initial term type set to "Manual" is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set start of initial term type = "Manual".

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear.

**References:** PHN-2178, Confluence "get product list" — section 4, Start of the initial term.

---

### TC-BE-38 (Negative): Product with supply activation type "Exact day" is excluded

**Description:** Verify that a product with supply activation after contract resigning type "Exact day" is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set supply activation type = "Exact day".

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear.

**References:** PHN-2178, Confluence "get product list" — section 4, Supply activation after contract resigning.

---

### TC-BE-39 (Negative): Product with multiple supply activation values is excluded

**Description:** Verify that a product with more than one supply activation after contract resigning value is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but configure two supply activation values.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because only one value is allowed for supply activation.

**References:** PHN-2178, Confluence "get product list" — section 4, Supply activation after contract resigning.

---

### TC-BE-40 (Negative): Product with "at least one is selected" advance payment flag is excluded

**Description:** Verify that a product where advance payment has "at least one is selected" is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add an advance payment to the product with "at least one is selected" flag.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the spec states "If 'at least one is selected' do not return such product."

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-41 (Negative): Product with advance payment obligatory but missing value

**Description:** Verify that a product with obligatory advance payment where value type is "exact amount" but value is empty is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add an advance payment: obligatory = true, value type = "exact amount", value = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because when value type is "exact amount" or "% from previous invoice amount", the value must be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-42 (Negative): Product with advance payment obligatory but missing date of issue value

**Description:** Verify that a product with obligatory advance payment where date of issue is "Date of the month" but value is empty is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add an advance payment: obligatory = true, value type = "exact amount" (value filled), date of issue = "Date of the month", date value = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because when date of issue is "Date of the month" or "Working days after invoice date", the value must be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-43 (Negative): Product with advance payment obligatory but missing payment term value

**Description:** Verify that a product with obligatory advance payment where a custom payment term is added but value is empty is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add an advance payment: obligatory = true, value type configured and filled, date of issue configured and filled, payment term added (not "Matches with standard"), payment term value = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because when a payment term is added (not matching standard), the value must be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-44 (Negative): Product with "Days after invoice date" advance payment but Value From/To not empty is excluded

**Description:** Verify that a product with advance payment "Days after invoice date" selected but Value From and/or Value To fields are NOT empty is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add an advance payment directly with "Days after invoice date" selected, Value = 15, Value From = 10, Value To = 20.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because when "Days after invoice date" is selected, the Value must be defined but Value From and Value To must be empty.

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-45 (Negative): Product with different advance payment settings is excluded

**Description:** Verify that a product where advance payments have different (inconsistent) settings is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add two advance payments with different settings (e.g., one obligatory with "exact amount", another obligatory with "% from previous invoice amount" using different date of issue types).

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the spec states "If interim and advance payment has different settings, then this product shouldn't be returned."

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-46 (Negative): Product with equal monthly installments selected but number of installments missing is excluded

**Description:** Verify that a product with equal monthly installments selected but number of installments not filled is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Equal monthly installments = selected/true, number of installments = empty/null, amount value = 50.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because number of installments must be filled when the checkbox is selected.

**References:** PHN-2178, Confluence "get product list" — section 4, Equal monthly installments.

---

### TC-BE-47 (Negative): Product with equal monthly installments selected but amount value missing is excluded

**Description:** Verify that a product with equal monthly installments selected but amount value not filled is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Equal monthly installments = selected/true, number of installments = 12, amount value = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because amount value must be filled when the checkbox is selected.

**References:** PHN-2178, Confluence "get product list" — section 4, Equal monthly installments.

---

### TC-BE-48 (Negative): Re-signing product is excluded

**Description:** Verify that a product marked as "re-signing" is excluded from the response.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but mark it as a re-signing product.
2. All other configurations are valid.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The re-signing product does NOT appear in the list. The spec explicitly states "No re-signing products."

**References:** PHN-2178, PHN-2614 ("Functionality for marking products as re-signing"), Confluence "get product list" — section 4.

---

### TC-BE-49 (Negative): Product with period from/to defined but current date outside range is excluded

**Description:** Verify that a product with Period from and Period to defined is excluded when the current date is outside the range.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set Period from = 2020-01-01, Period to = 2020-12-31 (both in the past).
2. Current date is outside this range.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the current date is not within the period from/to range.

**References:** PHN-2178, Confluence "get product list" — section 4, Status requirements.

---

### TC-BE-50 (Negative): Product with only period from defined and current date before period from is excluded

**Description:** Verify that a product with only Period from defined is excluded when the current date is less than Period from.

**Preconditions:**
1. Create a product per steps 1–7 from Test data but set Period from = 2028-01-01 (in the future), Period to = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because the current date is less than Period from.

**References:** PHN-2178, Confluence "get product list" — section 4, Status requirements.

---

### TC-BE-51 (Negative): Product with advance payment value type "price component" but price component empty is excluded

**Description:** Verify that a product with advance payment value type "price component" but no price component reference filled is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data.
2. Add an advance payment: obligatory = true, value type = "price component", price component = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because when value type is "price component", the price component reference must be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Interim and advance payment.

---

### TC-BE-52 (Negative): Product with "Cash Deposit and Bank Guarantee" but partial amounts missing is excluded

**Description:** Verify that a product with "Cash Deposit and Bank Guarantee" selected but only cash deposit amount filled (bank guarantee amount missing) is excluded.

**Preconditions:**
1. Create a product per steps 1–7 from Test data with: Payment guarantee = "Cash Deposit and Bank Guarantee", cash deposit amount = 500, cash deposit currency = BGN, bank guarantee amount = empty/null.

**Test steps:**
1. Send `GET /product/list` request.
2. Search for the product in the response.

**Expected test case results:** Response returns HTTP 200. The product does NOT appear because "Cash Deposit and Bank Guarantee" requires both cash deposit and bank guarantee amounts and currencies to be filled.

**References:** PHN-2178, Confluence "get product list" — section 4, Payment guarantee.

---

## References

- **Jira:** PHN-2178 – Get: product list (Energy products).
- **Parent Epic:** PHN-2243 – Sales portal - Product.
- **Subtask:** PHN-2179 – Backend Get: product list (Energy products).
- **Subtask:** PHN-2180 – [DB] Get: product list (Energy products).
- **Linked:** PHN-2614 – Functionality for marking products as "re-signing" (blocks PHN-2178).
- **Linked:** PHN-2187 – Get: List of products based on pod ID (blocked by PHN-2178).
- **Confluence:** [get product list](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/779517953/get+product+list) (page 779517953).
- **Codebase:** `ProductsController.java` (POST /products/list, /products/filter), `ProductService.java` (list, filter), `ProductRepository.java` (filter, searchForContract, searchForExpressContract), `ProductContractController.java` (GET /product-contract/list/products).
