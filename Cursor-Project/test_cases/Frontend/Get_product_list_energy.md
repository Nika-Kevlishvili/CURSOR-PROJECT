# Get Product List (Energy Products) – Sales Portal UI (PHN-2178)

**Jira:** PHN-2178 (Phoenix)
**Type:** Task (Story)
**Summary:** The Sales Portal UI must consume the `GET /sales-portal/product/list` API and correctly render the Energy product catalog available for portal sales. This file verifies UI rendering, attribute display, empty and error states, transliteration, and price-component label localization.

**Scope:** Frontend (UI) tests for the Sales Portal product catalog view. The portal user navigates to the product selection screen (during a new contract creation or product browsing flow). The UI fetches the product list from `GET /sales-portal/product/list` and renders each eligible product with its required display attributes (Printing name, price components, contract term, payment guarantee). Tests cover: correct product list rendering, required attribute display, transliteration fields, price component label display (including Cyrillic type names), empty state when no products qualify, loading state while the API call is in progress, error state when the API call fails or returns 401/500, search/filter interactions (if implemented), and accessibility of product information.

---

## Test data (preconditions)

Shared setup for all frontend tests.

- **Environment:** Test
1. Obtain a valid Sales Portal OAuth2 bearer token via `POST /sales-portal/oauth2/token` (grant_type: client_credentials, CLIENT_ID, CLIENT_SECRET from environment variables).
2. Create two or more fully qualifying **Energy products** via the backend Phoenix API (using the full data creation chain as described in the Backend test data — Terms → Price Components → Product with ACTIVE status, availableForSale: true, globalSalesChannel: true, globalSalesArea: true, globalSegment: true, customerIdentifier: null, Cash Deposit payment guarantee, valid contract term, price components of types "Активна електрическа енергия" AND "Такса"). Store the product IDs and their `printingName` values for assertion in frontend tests.
3. Create one additional product that does NOT qualify for the portal (e.g. `productStatus: INACTIVE`). This is used to verify that non-qualifying products are not shown in the UI.
4. Log into the Sales Portal application using a portal user account (valid username and password with access to the product catalog / new contract creation flow).
5. Navigate to the product selection / catalog screen within the Sales Portal.

---

## Frontend Test Cases

### TC-FE-1 (Positive): Product list is rendered with all qualifying products displayed

**Description:** Verify that after the portal UI loads the product catalog screen, all qualifying Energy products are rendered as list or card elements, and no qualifying product is missing from the display.

**Preconditions:**
1. Complete steps 1–5 from Test data above. At least two fully qualifying products exist.
2. User is logged into the Sales Portal and on the product catalog / selection screen.

**Test steps:**
1. Observe the product catalog page after navigation completes.
2. Count the product cards or rows visible on screen.
3. Confirm the qualifying product from step 2 of Test data (product with the known `printingName`) is present in the UI.
4. Confirm the second qualifying product (also from step 2) is also visible.
5. Confirm the non-qualifying (inactive) product from step 3 of Test data is NOT shown.

**Expected test case results:** The product catalog displays all qualifying products. Each qualifying product appears as a card or row element. The inactive product is completely absent from the view. The list is not empty (at least two items are shown).

**References:** PHN-2178; product catalog UI rendering.

---

### TC-FE-2 (Positive): Required product attributes are displayed for each product

**Description:** Verify that each product card/row in the UI displays the required product attributes: Printing Name, Short Description (when present), Type of Points of Delivery, Purpose of Consumption, Metering Type, Voltage Level, Payment Guarantee type, Contract Type, and Contract Term summary.

**Preconditions:**
1. Complete steps 1–5 from Test data.
2. The qualifying product has all mandatory attributes filled: `printingName`, `typeOfPointsOfDelivery`, `purposeOfConsumption`, `meteringType`, `voltageLevel`, `paymentGuarantee`, `contractType`, and `contractTerm`.

**Test steps:**
1. On the product catalog screen, locate the qualifying product card/row.
2. Verify the following fields are visible:
   - Printing Name matches the expected value from backend data.
   - Type of Points of Delivery is shown.
   - Purpose of Consumption is shown.
   - Metering Type is shown.
   - Voltage Level is shown.
   - Payment Guarantee type is displayed (e.g. "Cash Deposit").
   - Contract Type is displayed (e.g. "STANDARD").
   - Contract Term summary is shown (e.g. "12 months").

**Expected test case results:** All required product attributes are visible on the product card/row. None of the mandatory fields is blank, truncated incorrectly, or missing from the UI. The values match what was set during product creation.

**References:** PHN-2178; `ProductListForSalesChannelProjection` fields — UI rendering.

---

### TC-FE-3 (Positive): Price components displayed with correct labels (Активна електрическа енергия and Такса)

**Description:** Verify that the UI displays the price components for each product, using the correct Bulgarian-language labels "Активна електрическа енергия" and "Такса" (or their appropriate UI translations), and shows the relevant values (name, invoice name, value).

**Preconditions:**
1. Complete steps 1–5 from Test data.
2. The qualifying product has price components of both types: "Активна електрическа енергия" and "Такса", each with `name`, `invoiceName`, and value configured.

**Test steps:**
1. On the product catalog screen, locate the qualifying product.
2. Expand or view the price components section (if collapsed by default, click to expand).
3. Verify that the "Активна електрическа енергия" price component is listed with its name and value.
4. Verify that the "Такса" price component is listed with its name and value.
5. Verify that no other price type (e.g. "Мрежова такса") appears for this product, even if it is linked in the backend.

**Expected test case results:** The price components section shows exactly the two allowed types. Both type labels are displayed correctly (Cyrillic characters render without corruption). Price component names and values are legible and accurate.

**References:** PHN-2178; price component UI display; Cyrillic label rendering.

---

### TC-FE-4 (Positive): Printing Name transliteration displayed alongside Cyrillic name

**Description:** Verify that when a product has a `printingNameTransliteration` value, the portal UI displays both the Cyrillic printing name and its Latin transliteration (as a subtitle, secondary label, or tooltip — depending on UI implementation).

**Preconditions:**
1. Complete steps 1–5 from Test data.
2. The qualifying product has `printingName` = "Тест Продукт 1" and `printingNameTransliteration` = "Test Product 1" (both filled).

**Test steps:**
1. On the product catalog screen, locate the qualifying product.
2. Verify the Cyrillic printing name "Тест Продукт 1" is shown as the primary product name.
3. Verify the Latin transliteration "Test Product 1" is shown alongside (either as secondary text, a label, or in the same field depending on UI design).

**Expected test case results:** Both the Cyrillic and Latin names are visible. Cyrillic characters render correctly. No character corruption or encoding issues in either field.

**References:** PHN-2178; `printingName` + `printingNameTransliteration` — dual display.

---

### TC-FE-5 (Positive): Transliteration fields null — UI handles gracefully (no empty label shown)

**Description:** Verify that when `printingNameTransliteration` is null for a product, the UI does not display an empty or broken transliteration label (e.g. an empty span, "null" text, or broken layout).

**Preconditions:**
1. Complete steps 1–2 (auth + product creation) from Test data, but for one product set `printingNameTransliteration`: null.
2. Complete steps 4–5 (login and navigate to catalog).

**Test steps:**
1. On the product catalog screen, locate this product.
2. Inspect the transliteration area of the product card.
3. Verify that no empty label, "null" string, or broken layout element is visible in the transliteration position.

**Expected test case results:** The UI gracefully hides or omits the transliteration label when the value is null. The product card layout is intact and undisturbed. No "null" text or empty placeholder is visible.

**References:** PHN-2178; null transliteration — graceful UI handling.

---

### TC-FE-6 (Positive): Contract term details (perpetuity clause, renewal) shown correctly

**Description:** Verify that the UI correctly displays contract term details including the perpetuity clause indicator and automatic renewal settings when these are configured on the product.

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Create a product as in step 2 of Test data but with `automaticRenewal: true`, `renewalValue: 12`, `renewalType: Month`, `perpetuityClause: true`.
3. Complete steps 4–5 (login and navigate to catalog).

**Test steps:**
1. On the product catalog screen, locate this product.
2. View the contract term section of the product card.
3. Verify that the automatic renewal indicator is visible and shows "12 months" or equivalent.
4. Verify that the perpetuity clause is shown (e.g. a checkbox, badge, or label indicating it is enabled).

**Expected test case results:** Contract term section displays renewal period (12 months) and perpetuity clause indicator. Values are accurate and match the backend product configuration.

**References:** PHN-2178; contract term — renewal and perpetuity clause UI display.

---

### TC-FE-7 (Positive): Payment guarantee type and amounts displayed correctly

**Description:** Verify that the UI displays the payment guarantee type (Cash Deposit, Bank Guarantee, or Both) and the associated amounts and currencies correctly for each product.

**Preconditions:**
1. Complete steps 1–5 from Test data. The baseline qualifying product has Cash Deposit payment guarantee with amount 100 BGN.

**Test steps:**
1. On the product catalog screen, locate the qualifying product.
2. View the payment guarantee section of the product card.
3. Verify the payment guarantee type is shown as "Cash Deposit" (or the appropriate localized label).
4. Verify the amount (100) and currency (BGN) are displayed.

**Expected test case results:** The payment guarantee section correctly shows "Cash Deposit" with the configured amount and currency. No empty or placeholder values in this section.

**References:** PHN-2178; payment guarantee — UI display.

---

### TC-FE-8 (Positive): Empty state displayed correctly when no products qualify

**Description:** Verify that when the `GET /sales-portal/product/list` API returns an empty array (no qualifying products), the UI renders a proper **empty state** message or illustration — not a blank/broken page, and not an error state.

**Preconditions:**
1. Complete steps 4–5 (login and navigate) from Test data.
2. Arrange the environment so no qualifying products exist at the time of the test (all products are inactive or unavailable for sale). This may require a dedicated isolated environment or a state where all products have been deactivated.

**Test steps:**
1. Navigate to the product catalog screen in the Sales Portal.
2. Wait for the API response to complete.
3. Observe the product list area.

**Expected test case results:** The UI displays a meaningful empty state: an informational message (e.g. "No products available" or equivalent in Bulgarian) and/or an illustration. The page is not blank, not frozen, and shows no JavaScript errors in the console. The user can navigate away without issues.

**References:** PHN-2178; empty list — UI empty state.

---

### TC-FE-9 (Positive): Loading state displayed while API call is in progress

**Description:** Verify that the product catalog screen shows a **loading indicator** (spinner, skeleton loader, or progress bar) while the `GET /sales-portal/product/list` API call is pending, and the indicator is dismissed once the data arrives.

**Preconditions:**
1. Complete steps 4–5 (login and navigate) from Test data.
2. (Optional) Use browser developer tools or network throttling to slow the API response and make the loading state observable.

**Test steps:**
1. Navigate to the product catalog screen.
2. Observe the UI immediately after navigation, before the API response arrives.
3. Confirm a loading indicator (spinner, skeleton, or similar) is visible.
4. Wait for the API to respond and the product list to render.
5. Confirm the loading indicator disappears after data loads.

**Expected test case results:** A loading indicator appears promptly while the API call is in progress. It disappears cleanly when the data has loaded and the product list is rendered. No persistent spinner or flicker after data arrives.

**References:** PHN-2178; loading state — UX.

---

### TC-FE-10 (Negative): Error state displayed when API returns 401 Unauthorized

**Description:** Verify that when the Sales Portal API returns HTTP 401 (e.g. due to an expired or missing OAuth2 token), the UI renders an appropriate **error state** or redirects the user to the login page, rather than showing a blank page or unhandled exception.

**Preconditions:**
1. Log into the Sales Portal (step 4 from Test data).
2. Expire or invalidate the OAuth2 token on the client side (e.g. by manipulating localStorage/sessionStorage in the browser to remove or corrupt the token), then navigate to the product catalog screen.

**Test steps:**
1. Navigate to the product catalog screen with an invalid/missing token.
2. Wait for the API call to complete.
3. Observe the UI response.

**Expected test case results:** The UI either (a) redirects the user to the login/authentication page automatically, or (b) displays an error message indicating the session has expired and the user must log in again. The page does not show a blank white screen or an unhandled JavaScript exception. No product data from a previous session bleeds through.

**References:** PHN-2178; 401 error handling — UI error state / session expiry.

---

### TC-FE-11 (Negative): Error state displayed when API returns 500 or network failure

**Description:** Verify that when the `GET /sales-portal/product/list` API returns HTTP 500 (server error) or when the network request fails entirely, the UI renders a user-friendly **error message** (not a stack trace or blank screen).

**Preconditions:**
1. Log into the Sales Portal (step 4 from Test data).
2. Use a proxy tool (e.g. Fiddler, Charles, browser dev tools) to intercept the `GET /sales-portal/product/list` request and return a 500 response or a network error (connection refused).

**Test steps:**
1. Navigate to the product catalog screen with the interception active.
2. Wait for the API call to fail.
3. Observe the UI response.

**Expected test case results:** The UI displays a user-friendly error message (e.g. "Unable to load products. Please try again." or equivalent). No stack trace, raw error object, or blank page is shown. A retry button or navigation option is available if the design supports it.

**References:** PHN-2178; 500/network error — UI error handling.

---

### TC-FE-12 (Negative): Non-qualifying products are NOT rendered in the product list UI

**Description:** Verify that products excluded by any of the backend eligibility rules (inactive, not available for sale, individual, deleted, etc.) are never displayed in the Sales Portal product catalog UI. The frontend must trust the API filtering and not perform any client-side re-inclusion.

**Preconditions:**
1. Complete steps 1–5 from Test data. Step 3 created a non-qualifying (inactive) product.
2. The non-qualifying product has a known, unique `printingName` (e.g. "EXCLUDED Product").

**Test steps:**
1. On the product catalog screen, scan all visible products.
2. Search for (or visually scan for) a product named "EXCLUDED Product" or with the known ID of the inactive product.

**Expected test case results:** The non-qualifying product is not present anywhere in the product catalog UI. The UI list perfectly mirrors what the backend API returns. The frontend does not show any products beyond what the API provided.

**References:** PHN-2178; client-side trust of backend filtering.

---

### TC-FE-13 (Positive): Large product list — UI handles many products without performance degradation

**Description:** Verify that when the product list contains a large number of qualifying products (e.g. 50+), the UI renders the list without freezing, crashing, or unacceptable load times. Pagination or virtual scrolling (if implemented) functions correctly.

**Preconditions:**
1. Complete step 4–5 from Test data.
2. Arrange the environment to have at least 20–50 qualifying products visible via `GET /sales-portal/product/list` (create multiple products if necessary).

**Test steps:**
1. Navigate to the product catalog screen.
2. Wait for the full list to render.
3. Measure (or observe) page load time and scroll performance.
4. Scroll through the entire list.
5. If pagination is implemented: navigate between pages and confirm products on each page are correct.
6. If virtual scrolling is implemented: scroll to the bottom and confirm all products load.

**Expected test case results:** The product catalog renders within an acceptable time (under 5 seconds for 50 products). No browser freeze, scroll jank, or UI crash occurs. All products are accessible. Pagination or virtual scrolling (if present) works correctly without skipping or duplicating products.

**References:** PHN-2178; product list — large data set performance.

---

### TC-FE-14 (Positive): Price component "Price application model over time" — ApplicationModel Period, Level, ApplicationType shown in UI

**Description:** Verify that when a price component has an ApplicationModel of type "Price application model over time", the UI renders the `applicationModel.period`, `applicationModel.level`, and `applicationModel.applicationType` values alongside the price component entry in the product catalog.

**Preconditions:**
1. Complete steps 1–2 (auth) from Test data.
2. Create a qualifying product whose "Активна електрическа енергия" price component has `applicationModel.period` = "Peak", `applicationModel.level` = "HV", and `applicationModel.applicationType` = "Time-based".
3. Complete steps 4–5 (login and navigate).

**Test steps:**
1. On the product catalog screen, locate this product.
2. View the price components section and find the "Активна електрическа енергия" entry.
3. Verify that the ApplicationModel sub-details are displayed: period "Peak", level "HV", applicationType "Time-based" (or the localized equivalents).

**Expected test case results:** The ApplicationModel fields are visible within the price component breakdown. Values match the backend configuration. When ApplicationModel is absent on other products, the section is cleanly omitted without leaving blank space.

**References:** PHN-2178; ApplicationModel "over time" — UI display of Period/Level/ApplicationType.

---

## References

- **Jira:** PHN-2178 – Get product list (Energy products) — Sales Portal UI.
- **Endpoint consumed by UI:** `GET /sales-portal/product/list`.
- **Auth:** `POST /sales-portal/oauth2/token` — OAuth2 client credentials.
- **Related:** TC backend counterpart `test_cases/Backend/Get_product_list_energy.md`; PHN-2187 (Sales Portal POD list); Sales Portal portal application.
