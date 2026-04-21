# Get Product List (Energy Products) – Sales Portal product catalog (PHN-2178)

**Jira:** PHN-2178 (Phoenix Phase 2)
**Type:** Story
**Summary:** The Sales Portal product list page displays all energy products available for sale, allowing customers to assess their options and potentially sign a contract with Energo-Pro.

**Scope:** When a customer visits the Sales Portal product list page, they should see a list of fully configured, active energy products available via the "Portals" sales channel. Each product displays its attributes: printing name, description, type of POD (Point of Delivery), purpose of consumption, metering type, voltage level, capacity limit, payment guarantee, contract type, contract term details, and price components. The UI must handle empty states, loading, and errors gracefully.

---

## Test data (preconditions)

Shared setup for this file (environment + entity creation chain).

- **Environment:** Test
1. Log into the Sales Portal with a customer account (or demo account with view permissions for the product catalog).
2. Ensure at least one product is available via `GET /product/list` (i.e., a standard product exists that is ACTIVE, available for sale, with "Portals" sales channel, ALL areas, ALL segments, fixed parameters, not individual, not deleted, not re-signing — created via backend preconditions described in `Backend/Get_product_list_energy_products.md`).
3. Ensure the backend `GET /product/list` endpoint is deployed and accessible from the Sales Portal frontend.

---

## Frontend Test Cases

### TC-FE-1 (Positive): Product list page loads and displays available energy products

**Description:** Verify that the Sales Portal product list page loads successfully and displays the list of energy products returned by the backend.

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. At least one valid product exists in the backend (per backend test data setup).

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Wait for the page to fully load (loading indicator disappears).
3. Observe the product list.

**Expected test case results:** The page loads without errors. A list of products is displayed. Each product entry is visible with at least its name (PrintingName) shown. The number of products matches the count returned by `GET /product/list`.

**References:** PHN-2178.

---

### TC-FE-2 (Positive): Product details display all required attributes

**Description:** Verify that each product in the list displays all required attributes from the response: printing name, transliterated name, short description, type of POD, purpose of consumption, metering type, voltage level, capacity limit, payment guarantee, and contract type.

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. At least one product is displayed in the list.

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Locate a product entry in the list.
3. Inspect the displayed attributes (expand the product card/details if the UI uses a collapsed view).

**Expected test case results:** The product displays: PrintingName, Printing name (Transliterated), Short description, Type of points of delivery (e.g., "CONSUMPTION"), Purpose of consumption (e.g., "NON_HOUSEHOLD"), Metering type, Voltage level, Provided capacity limit in kWh, Payment guarantee (e.g., "BANK_GUARANTEE"), and Contract type (e.g., "SUPPLY_ONLY"). All values are human-readable and properly formatted (not raw API codes unless designed that way).

**References:** PHN-2178, Confluence "get product list" — Response Parameters.

---

### TC-FE-3 (Positive): Contract term details displayed correctly

**Description:** Verify that the contract term section for a product shows all sub-fields: type of terms, value, type (unit), automatic renewal, renewal value, renewal type, perpetuity clause.

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. A product with a fully configured contract term is displayed.

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Locate a product and view its contract term details (expand section if collapsed).

**Expected test case results:** The contract term section displays: Type of terms (e.g., "Fixed term"), Value (e.g., 12), Type (e.g., "Month"), Automatic renewal (Yes/No), Renewal value, Renewal type, Perpetuity clause (Yes/No). Values match what the backend returns.

**References:** PHN-2178, Confluence "get product list" — Contract term response attributes.

---

### TC-FE-4 (Positive): Price components displayed with correct structure

**Description:** Verify that price components (filtered by Price Type "Active electric energy" or "Fee") are displayed with their sub-attributes: name, value type, number type, formula X values, conditions, price with words, and application model (when applicable).

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. A product with at least one price component of type "Active electric energy" is displayed.

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Locate a product and view its price components section.

**Expected test case results:** Price components are displayed with: Name, Name to show in invoices, Value type, Number type, formula variables (X descriptions and values), Conditions (parameter, value, operator), Price with words, and Conditions with words. If an application model exists (type "Price application model over time"), its Period, Level, and ApplicationType are shown.

**References:** PHN-2178, Confluence "get product list" — Price components response attributes.

---

### TC-FE-5 (Positive): Payment guarantee information displayed correctly

**Description:** Verify that the payment guarantee type is displayed for each product (e.g., "Cash Deposit", "Bank Guarantee", or "Cash Deposit and Bank Guarantee").

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. Products with different payment guarantee types exist.

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Locate products and observe the payment guarantee field.

**Expected test case results:** Each product shows its payment guarantee type in a human-readable format. The display matches the backend response value.

**References:** PHN-2178, Confluence "get product list" — Payment guarantee.

---

### TC-FE-6 (Positive): Text to show in invoices and templates displayed

**Description:** Verify that the "Text to show in invoices and templates" (and its transliterated version) are displayed for each product.

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. Products with invoice text configured are displayed.

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Locate a product and check for invoice template text.

**Expected test case results:** Both "Text to show in invoices and templates" and its transliterated version are visible in the product details. Text renders correctly (no encoding issues, Cyrillic characters display properly).

**References:** PHN-2178, Confluence "get product list" — Response Parameters.

---

### TC-FE-7 (Positive): Product list page handles multiple products

**Description:** Verify that the page correctly displays multiple products when more than one valid product exists.

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. Create at least 3 valid products via the backend (different configurations but all meeting filter criteria).

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Count the number of product entries displayed.
3. Scroll through the list if pagination or infinite scroll is used.

**Expected test case results:** All products returned by `GET /product/list` are displayed. If pagination is implemented, navigation between pages works correctly. Products are visually distinct and do not overlap or duplicate.

**References:** PHN-2178.

---

### TC-FE-8 (Negative): Empty state when no products are available

**Description:** Verify that the product list page displays a user-friendly empty state message when `GET /product/list` returns an empty list.

**Preconditions:**
1. Log into the Sales Portal.
2. Ensure no products meet the filter criteria (e.g., all products are deactivated or removed from "Portals" sales channel in the backend).

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Wait for the page to load.

**Expected test case results:** The page loads without errors. Instead of an empty list, a user-friendly message is displayed (e.g., "No products available at this time" or similar). No broken layout or JavaScript errors appear. The page remains navigable.

**References:** PHN-2178.

---

### TC-FE-9 (Negative): Error handling when backend API returns an error

**Description:** Verify that the product list page handles backend API errors gracefully (e.g., HTTP 500 or network timeout).

**Preconditions:**
1. Log into the Sales Portal.
2. Simulate a backend failure (e.g., backend service is down or returns HTTP 500 for `GET /product/list`).

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Wait for the page to attempt loading.

**Expected test case results:** The page does not crash or display a blank white screen. A user-friendly error message is shown (e.g., "Unable to load products. Please try again later."). A retry option or button may be available. No sensitive technical details (stack traces, internal URLs) are exposed to the user.

**References:** PHN-2178.

---

### TC-FE-10 (Negative): Product list handles slow API response gracefully

**Description:** Verify that the product list page shows a loading indicator while waiting for the API response and does not time out prematurely.

**Preconditions:**
1. Log into the Sales Portal.
2. Backend `GET /product/list` has a delayed response (e.g., due to large dataset or slow database — simulated or natural).

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Observe the page during loading.

**Expected test case results:** A loading indicator (spinner, skeleton, or progress bar) is displayed while the API call is in progress. The page does not freeze or become unresponsive. Once data arrives, the loading indicator disappears and products are displayed. If the response exceeds a reasonable timeout (e.g., 30 seconds), a timeout error message is shown.

**References:** PHN-2178.

---

### TC-FE-11 (Positive): Cyrillic and transliterated text render correctly

**Description:** Verify that product names, descriptions, and invoice texts in Cyrillic (Bulgarian) script render correctly alongside their transliterated (Latin) versions.

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. A product exists with Cyrillic PrintingName (e.g., "Бизнес Електро Плюс") and transliterated name (e.g., "Biznes Elektro Plyus").

**Test steps:**
1. Navigate to the Sales Portal product list page.
2. Locate the product and inspect the name rendering.

**Expected test case results:** Cyrillic characters display correctly without mojibake or encoding issues. The transliterated (Latin) version displays alongside or below the Cyrillic name. Both are legible and properly formatted.

**References:** PHN-2178, Confluence "get product list" — JSON sample.

---

### TC-FE-12 (Positive): Page refresh reloads product list from API

**Description:** Verify that refreshing the product list page fetches fresh data from `GET /product/list`.

**Preconditions:**
1. Complete steps 1–3 from Test data above.
2. Products are displayed in the list.

**Test steps:**
1. Navigate to the Sales Portal product list page and observe the initial product list.
2. Refresh the page (browser refresh or pull-to-refresh on mobile).
3. Observe the list after refresh.

**Expected test case results:** The page reloads and fetches fresh data from the API. The product list is displayed again. If a product was added or removed in the backend between loads, the updated list reflects the change.

**References:** PHN-2178.

---

## References

- **Jira:** PHN-2178 – Get: product list (Energy products).
- **Parent Epic:** PHN-2243 – Sales portal - Product.
- **Confluence:** [get product list](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/779517953/get+product+list) (page 779517953).
- **Backend test cases:** `Backend/Get_product_list_energy_products.md` (TC-BE-1 through TC-BE-52).
