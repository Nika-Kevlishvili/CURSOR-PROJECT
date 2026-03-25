# Sales Portal customers by coordinates – Request validation and auth (PHN-2529)

**Jira:** PHN-2529 (Phoenix)  
**Type:** Task  
**Summary:** The new endpoint GET /sales-portal/customers/by-coordinates must accept valid coordinate and pagination parameters and must reject invalid inputs and unauthorized requests with clear errors.

**Scope:** This document covers request validation and authentication for **GET /sales-portal/customers/by-coordinates**. Valid requests with coordinates (e.g. lat/lon and radius or bbox) and pagination must succeed when the caller is authenticated and authorized. Invalid coordinate format, missing required parameters, and missing or invalid authentication must be rejected with appropriate HTTP status and message.

---

## Test data (preconditions)

- **Environment:** Test (or as per ticket).
- **Sales Portal API:** The Sales Portal base URL is available and the endpoint path is `/sales-portal/customers/by-coordinates`.
- **Valid JWT:** A valid Sales Portal JWT exists for an authenticated sales agent (or test user) that is allowed to call this endpoint.
- **Invalid/expired token:** An expired or malformed JWT is available for negative auth tests.
- **Customers with coordinates:** At least a few customer records exist that can be returned when querying by coordinates (for positive tests).

---

## TC-1 (Positive): Valid request with coordinates and pagination returns success

**Objective:** Verify that a valid request to GET /sales-portal/customers/by-coordinates with correct coordinate parameters (e.g. latitude, longitude, radius or bbox) and pagination (e.g. page, size) returns HTTP 200 and a well-formed JSON response.

**Preconditions:**
1. The caller has a valid Sales Portal JWT.
2. The request body or query includes valid coordinate parameters as defined by SalesPortalCustomerByCoordinatesRequest (e.g. lat, lon, radius or bbox).
3. Pagination parameters (e.g. page, size) are valid and within allowed range.

**Steps:**
1. Obtain a valid JWT for the Sales Portal (e.g. via login or test token).
2. Call GET /sales-portal/customers/by-coordinates with valid coordinates (e.g. lat=41.7, lon=44.8, radius in metres if required) and pagination (e.g. page=0, size=10).
3. Assert response status is 200 and body is JSON.
4. Assert response structure matches SalesPortalCustomerByCoordinatesResponse (e.g. list of customers and/or pagination metadata).

**Expected result:** Request succeeds (HTTP 200). Response body is valid JSON and matches the expected schema (e.g. content array, total count, page/size if applicable). No server error or 401/403.

**References:** PHN-2529; SalesPortalCustomerByCoordinatesRequest; SalesPortalCustomerByCoordinatesResponse.

---

## TC-2 (Negative): Invalid or missing coordinate parameters are rejected

**Objective:** Ensure the backend validates coordinate inputs and returns a client error (e.g. 400) when coordinates are missing, malformed, or out of range.

**Preconditions:**
1. The caller has a valid Sales Portal JWT.

**Steps:**
1. Call GET /sales-portal/customers/by-coordinates with missing required coordinate parameters (e.g. no lat/lon or no bbox).
2. Call with non-numeric lat or lon (e.g. lat=abc).
3. Call with out-of-range values (e.g. lat=100, lon=200) if the API defines range validation.

**Expected result:** The system returns a client error (e.g. HTTP 400) with a clear validation message. No results are returned and no 5xx server error occurs.

**References:** Request validation; SalesPortalCustomerByCoordinatesRequest.

---

## TC-3 (Negative): Missing or invalid authentication is rejected

**Objective:** Verify that requests without a valid Sales Portal JWT (or with expired/invalid token) are rejected with 401 Unauthorized or 403 Forbidden.

**Preconditions:**
1. No valid JWT is sent, or an expired/malformed token is used.

**Steps:**
1. Call GET /sales-portal/customers/by-coordinates without Authorization header (or with an invalid/expired token).
2. Optionally call with a token that does not have permission for the Sales Portal customers-by-coordinates resource.

**Expected result:** The system returns 401 Unauthorized or 403 Forbidden. Response must not return customer data. Message may indicate invalid or expired token or insufficient permissions.

**References:** Sales Portal auth; JWT.

---

## References

- **Jira:** PHN-2529 – Get customer list by sales agent coordinates.
- **Endpoint:** GET /sales-portal/customers/by-coordinates.
- **Related:** SalesPortalCustomerController; SalesPortalCustomerService; CustomerDetailsRepository.
