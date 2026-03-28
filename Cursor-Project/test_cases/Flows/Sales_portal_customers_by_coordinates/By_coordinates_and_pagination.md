# Sales Portal customers by coordinates – By coordinates and pagination (PHN-2529)

**Jira:** PHN-2529 (Phoenix)  
**Type:** Task  
**Summary:** The endpoint must return customers filtered by geographic coordinates and support pagination; empty result sets must be returned cleanly with valid metadata.

**Scope:** This document covers the success path for GET /sales-portal/customers/by-coordinates: returning customers that fall within the given coordinate filter (e.g. point + radius or bounding box), correct pagination (page, size, total), and deterministic behaviour when no customers match (empty list with valid structure).

---

## Test data (preconditions)

- **Environment:** Test (or as per ticket).
- **Valid JWT:** Sales Portal JWT for an authenticated user.
- **Customers in area:** At least some customers exist with address/coordinates in a known area A so that a query with coordinates covering A returns at least one result.
- **Area with no customers:** A coordinate range is known (or can be chosen) that contains no customers, for empty-result test.

---

## TC-4 (Positive): Customers returned when coordinates match existing data

**Objective:** Verify that when the request coordinates (e.g. lat, lon, radius) cover an area where customers exist, the response includes those customers in the result list, respecting pagination.

**Preconditions:**
1. Valid JWT and at least one customer in the database whose location falls within the requested coordinate range.
2. Request uses valid pagination (e.g. page=0, size=10).

**Steps:**
1. Call GET /sales-portal/customers/by-coordinates with coordinates that cover an area containing at least one customer.
2. Check response status 200 and that the content (or equivalent) array contains at least one item.
3. Optionally verify that returned customer identifiers or attributes are consistent with the coordinate filter.

**Expected result:** HTTP 200. Response contains a non-empty list of customers (or projection) and pagination metadata (e.g. totalElements, totalPages, size) when applicable. Data is consistent with the coordinate filter.

**References:** CustomerDetailsRepository; native query with coordinates.

---

## TC-5 (Positive): Pagination parameters control page and size correctly

**Objective:** Verify that pagination parameters (e.g. page, size) are applied: second page returns different items, and size limits the number of items per page.

**Preconditions:**
1. Enough customers exist in the requested area to fill more than one page (e.g. > 10 if size=10).
2. Valid JWT.

**Steps:**
1. Call GET /sales-portal/customers/by-coordinates with page=0, size=5; record the first page of results (e.g. customer IDs).
2. Call with page=1, size=5; record the second page.
3. Compare: second page must not duplicate first page items; total count or totalPages should be consistent.

**Expected result:** First and second page return different sets of customers. Number of items per page does not exceed the requested size. Pagination metadata (totalElements, totalPages, number, size) is correct and consistent.

**References:** CustomerDetailsRepository pagination; SalesPortalCustomerByCoordinatesResponse.

---

## TC-6 (Positive): Empty result set returned cleanly when no customers match

**Objective:** When the coordinate filter matches no customers, the API must return HTTP 200 with an empty list and valid structure (no server error).

**Preconditions:**
1. Valid JWT.
2. A coordinate range is used that is known to contain no customers (e.g. remote area or bbox with no data).

**Steps:**
1. Call GET /sales-portal/customers/by-coordinates with coordinates that match no customers.
2. Assert status 200 and response body is valid JSON.
3. Assert content/list is empty and any pagination fields (totalElements=0, totalPages=0 or 1) are present and correct.

**Expected result:** HTTP 200. Response has empty content array and valid pagination metadata. No 404 or 5xx.

**References:** Empty result handling; response schema.

---

## References

- **Jira:** PHN-2529 – Get customer list by sales agent coordinates.
- **Endpoint:** GET /sales-portal/customers/by-coordinates.
- **Related:** CustomerDetailsRepository; SalesPortalCustomerByCoordinatesResponse; pagination.
