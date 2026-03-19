# PHN-2529 - Input Validation and Invalid Parameters

**Scope:** Validate request parameter constraints and error contract for invalid input.

## Preconditions

- Endpoint validation is enabled.
- Error response format is defined and stable.

## TC-1: Latitude/longitude valid range acceptance

**Steps:**
1. Call endpoint with valid edge values (`latitude=-90/90`, `longitude=-180/180`).
2. Observe response.

**Expected result:**
- Valid range boundaries are accepted.

## TC-2: Latitude out of range is rejected

**Steps:**
1. Call endpoint with `latitude < -90` and `latitude > 90`.
2. Capture response code and validation message.

**Expected result:**
- Request is rejected with validation error.
- Error points to invalid latitude parameter.

## TC-3: Longitude out of range is rejected

**Steps:**
1. Call endpoint with `longitude < -180` and `longitude > 180`.
2. Capture response.

**Expected result:**
- Request is rejected with validation error referencing longitude.

## TC-4: Missing required coordinate parameter is rejected

**Steps:**
1. Omit `latitude`, then omit `longitude`.
2. Validate error response payload.

**Expected result:**
- Each request fails with clear required-parameter error.

## TC-5: Malformed numeric formats are rejected

**Steps:**
1. Send non-numeric values, scientific notation edge cases, and locale-formatted decimals.
2. Validate response behavior.

**Expected result:**
- Invalid numeric formats are rejected consistently.
- No implicit locale-dependent parsing issues.

## TC-6: Invalid pagination and sort parameters are rejected

**Steps:**
1. Send invalid `page`, `size`, and unsupported sort keys/directions.
2. Validate status and error contract.

**Expected result:**
- Invalid paging/sort input is rejected.
- Error payload is consistent and actionable.

## TC-7: Unknown query parameters do not break contract

**Steps:**
1. Add unknown query parameters.
2. Observe whether they are ignored or rejected per API policy.

**Expected result:**
- Behavior matches documented API policy.
- No server error due to unknown parameters.
# Input validation and invalid parameters

## TC-VAL-01 - Invalid latitude format
**Rationale:** Covers input validation edge cases for coordinates.

**Preconditions**
- Valid auth token is available.

**Steps**
1. Call endpoint with non-numeric latitude (e.g., `abc`) and valid longitude.

**Expected result**
- Request is rejected with `400 Bad Request` (or project-standard validation code).
- Error payload identifies invalid parameter.

## TC-VAL-02 - Latitude out of range
**Rationale:** Ensures boundary validation (`-90` to `90`) is enforced.

**Preconditions**
- Valid auth token is available.

**Steps**
1. Call endpoint with latitude `91` and valid longitude.

**Expected result**
- Validation error response is returned.
- No repository query is executed for invalid request path.

## TC-VAL-03 - Longitude out of range
**Rationale:** Ensures boundary validation (`-180` to `180`) is enforced.

**Preconditions**
- Valid auth token is available.

**Steps**
1. Call endpoint with valid latitude and longitude `181`.

**Expected result**
- Validation error response is returned.
- Error structure follows standard API validation format.

## TC-VAL-04 - Invalid pagination params
**Rationale:** Prevents runtime failures from negative page/size and malformed params.

**Preconditions**
- Valid auth token is available.

**Steps**
1. Call endpoint with `page=-1`.
2. Call endpoint with `size=0` (if not allowed).
3. Call endpoint with non-numeric `page`/`size`.

**Expected result**
- Invalid params are rejected consistently.
- API does not return `500` for client-side input mistakes.

## TC-VAL-05 - Missing required parameters
**Rationale:** Ensures request contract validation before service/repository execution.

**Preconditions**
- Valid auth token is available.

**Steps**
1. Call endpoint without latitude.
2. Call endpoint without longitude.
3. Call endpoint without radius (if radius is mandatory).

**Expected result**
- Each request fails with validation error.
- Error message identifies missing required parameter(s).
- No internal error or stack trace is returned.

## TC-VAL-06 - Coordinate boundary values are accepted
**Rationale:** Validates inclusive bounds handling and avoids false negatives at limits.

**Preconditions**
- Valid auth token is available.

**Steps**
1. Call endpoint with latitude `-90`, longitude `-180`, valid radius.
2. Call endpoint with latitude `90`, longitude `180`, valid radius.

**Expected result**
- Requests are accepted by validation layer.
- Endpoint returns contract-compliant response (`200` or empty content, depending on data).

## TC-VAL-07 - Radius boundary and invalid radius values
**Rationale:** Covers high-risk radius validation and default handling from `what_could_break`.

**Preconditions**
- Valid auth token is available.
- Allowed min/max radius are known from API contract.

**Steps**
1. Call endpoint with minimum allowed radius.
2. Call endpoint with maximum allowed radius.
3. Call endpoint with radius `0` and negative radius.
4. Call endpoint with radius above maximum.

**Expected result**
- Min/max values are accepted when in range.
- Invalid radius values are rejected with clear validation errors.
- API never returns `500` for invalid client input.

## TC-VAL-08 - Invalid sort parameter and direction
**Rationale:** Ensures that unsupported sort keys or directions are validated consistently.

**Preconditions**
- Valid auth token is available.
- Documented list of allowed sort keys and directions exists (e.g., `asc`/`desc`).

**Steps**
1. Call endpoint with an unsupported sort key (e.g., `sort=unknownProperty,asc`).
2. Call endpoint with an invalid sort direction (e.g., `sort=classification,invalidDirection`).
3. Call endpoint with multiple sort parameters where one is invalid.

**Expected result**
- Requests with invalid sort parameters are rejected with a clear validation error.
- Error payload identifies the offending sort parameter when possible.
- No server-side error or fallback to an undocumented sort order occurs.
