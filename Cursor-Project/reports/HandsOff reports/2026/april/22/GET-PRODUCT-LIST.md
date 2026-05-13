# GET-PRODUCT-LIST – Playwright test results

**Jira:** GET-PRODUCT-LIST (Task provided directly)  
**Title:** Get Product List – Backend API validation for product catalog  
**Date:** 2026-04-22  
**Assignee:** N/A / Tester: N/A (no Jira ticket)

**Total:** 0 passed, 1 failed, 14 skipped, 55 did not run.

-------------------------

## Playwright test results:

### Test 1: [GET-PRODUCT-LIST] TC-BE-1 – Happy path – Valid product appears in product list

**Test description:**
Verify that a fully configured standard product meeting all requirements is returned by `POST /products/list`. This test creates terms, price component, and product, then calls the list endpoint to verify the product appears.

**Expected result:**
HTTP 200. Response contains a paginated list. The created product appears in the list with all expected attributes (Product ID, Product Version, PrintingName, etc.).

**Actual result:**
HTTP 400. Product creation failed during precondition setup.
Error: `priceSettings.priceComponentIds[0]-can't find price component with id: null;`
The price component creation step succeeded but returned a null ID, causing the product creation to fail.

**Test result:**
Failed

---

### Tests 2-58: Backend tests (TC-BE-2 through TC-BE-58)

**Test description:**
Backend API tests covering: product status validation, availability dates, sales channels, areas, segments, contract types, payment terms, payment guarantees, price components, entering into force, start of initial term, supply activation, interim payments, equal monthly installments, and response attribute validation.

**Expected result:**
Various validations per test case – some products should appear in list (positive), others should be excluded (negative).

**Actual result:**
Did not run – dependent on TC-BE-1 which failed in beforeAll preconditions.

**Test result:**
Not run (55 tests)

---

### Tests 59-70: Frontend tests (TC-FE-1 through TC-FE-12)

**Test description:**
Frontend (UI) test cases for Sales Portal product list page: page loading, attribute display, contract terms, price components, payment guarantee, Cyrillic rendering, pagination, error handling, empty state.

**Expected result:**
Various UI behaviors for product list display.

**Actual result:**
Skipped – UI tests are not automated in EnergoTS API suite.

**Test result:**
Skipped (14 tests)

---

## Root Cause Analysis

The test failure occurred in the `test.beforeAll` precondition setup:

1. **Terms creation:** SUCCESS (termId: 18928)
2. **Price component creation:** The API call returned HTTP 200, but the response body did not contain a valid `id` field, resulting in `basePriceComponentId = null`
3. **Product creation:** FAILED because `priceComponentIds: [null]` is invalid

**Investigation needed:**
- Verify the price component creation endpoint (`POST /price-components`) response structure
- Check if the `electricity()` payload generator returns the correct response format
- Confirm the API returns `{ id: number }` or a different structure

---

## Spec file location

`Cursor-Project/EnergoTS/tests/cursor/GET-PRODUCT-LIST-product-list.spec.ts`

**To run:**
```bash
cd Cursor-Project/EnergoTS
npx playwright test --grep "GET-PRODUCT-LIST"
```

---

## Test cases coverage

- **Backend:** 58 test cases (TC-BE-1 to TC-BE-58) in `test_cases/Backend/Get_product_list_energy_products.md`
- **Frontend:** 12 test cases (TC-FE-1 to TC-FE-12) in `test_cases/Frontend/Get_product_list_energy_products.md`
- **Playwright spec:** 70 test blocks (1:1 mapping)

---

## Validation issues from playwright-test-validator

1. Title format uses `[GET-PRODUCT-LIST]` instead of Jira key format (acceptable – no real Jira ticket)
2. Some `test.skip` calls missing explicit reason strings
3. Minor alignment issues in TC-BE-49, 50, 52, 53 (response attribute validation depth)
4. `Responses.*[0]` indexing in group tests may reference wrong entities under serial accumulation

---

## Agents involved

HandsOff (orchestrator), CrossDependencyFinderAgent, TestCaseGeneratorAgent (parent), EnergoTSTestAgent, PlaywrightTestValidatorAgent, EnergoTS Playwright Test Runner
