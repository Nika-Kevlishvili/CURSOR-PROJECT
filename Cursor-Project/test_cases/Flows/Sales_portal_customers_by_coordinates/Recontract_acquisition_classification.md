# Sales Portal customers by coordinates – Recontract/acquisition classification (PHN-2529)

**Jira:** PHN-2529 (Phoenix)  
**Type:** Task  
**Summary:** The response must include recontract/acquisition classification for each customer (or in the projection) so that the Sales Portal can distinguish customer types.

**Scope:** This document covers the classification aspect of GET /sales-portal/customers/by-coordinates. The backend implements recontract and acquisition classification in CustomerDetailsRepository; the API response (SalesPortalCustomerByCoordinatesProjection or response model) must expose this so that clients can use it correctly.

---

## Test data (preconditions)

- **Environment:** Test (or as per ticket).
- **Valid JWT:** Sales Portal JWT.
- **Customers with known classification:** Where possible, test data includes customers that are classified as recontract and others as acquisition (or equivalent values as per business rules).

---

## TC-7 (Positive): Response includes recontract/acquisition classification field

**Objective:** Verify that each customer item in the response (or projection) includes a field that indicates recontract vs acquisition (or the defined classification values).

**Preconditions:**
1. Valid JWT.
2. At least one customer is returned for the given coordinate request.

**Steps:**
1. Call GET /sales-portal/customers/by-coordinates with coordinates that return at least one customer.
2. Inspect the response schema: each element in the list must have a classification-related field (e.g. recontract/acquisition or a single enum/string).
3. Verify the field is present and has a value (e.g. "RECONTRACT", "ACQUISITION" or as defined in the API).

**Expected result:** Response body includes the classification field for each customer. Values are consistent with the backend logic (CustomerDetailsRepository native query and projection). No missing or null classification when the business logic assigns one.

**References:** SalesPortalCustomerByCoordinatesProjection; CustomerDetailsRepository; recontract/acquisition classification.

---

## TC-8 (Positive): Classification values are consistent with backend logic

**Objective:** Ensure that when test data has known recontract vs acquisition customers, the API returns the correct classification for each.

**Preconditions:**
1. Test data includes at least one customer known to be recontract and one known to be acquisition (or equivalent), or documentation defines how classification is derived.
2. Valid JWT and coordinates that return these customers.

**Steps:**
1. Call GET /sales-portal/customers/by-coordinates so that the result set includes customers with known classification.
2. For each returned customer, compare the classification field value to the expected value (from test data or business rules).
3. If the repository logic is documented (e.g. by contract status or similar), verify at least one recontract and one acquisition example match.

**Expected result:** Classification values in the response match the backend classification logic. No incorrect or swapped values for known test data.

**References:** CustomerDetailsRepository; business rules for recontract vs acquisition.

---

## References

- **Jira:** PHN-2529 – Get customer list by sales agent coordinates.
- **Endpoint:** GET /sales-portal/customers/by-coordinates.
- **Related:** SalesPortalCustomerByCoordinatesProjection; CustomerDetailsRepository; recontract/acquisition classification.
