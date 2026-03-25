# POD coordinate search – Request validation and backward compatibility (PHN-2197)

**Jira:** PHN-2197 (Phoenix)  
**Type:** Task  
**Summary:** Coordinate-based filtering must be supported on the existing POD search endpoint without breaking existing callers. Requests must be validated consistently, with clear errors for invalid coordinate inputs and combinations.

**Scope:** This document covers input and behaviour validation for extending **GET `/pod/list`** with coordinate-based search parameters for a map view (e.g. bounding box or point + radius), while preserving the existing search behaviour when coordinate parameters are not provided. It verifies backward compatibility, validation of coordinate formats and ranges, and deterministic handling when both coordinate filters and existing filters are used together.

---

## Test data (preconditions)

- **Environment:** Test (or as per ticket).
- **POD dataset (with coordinates):** At least 10 PODs exist with valid coordinates (latitude/longitude) spread across multiple geographic areas (e.g. two cities/regions far apart).
- **POD dataset (without coordinates):** At least 3 PODs exist with missing coordinates (null/empty) or coordinates not set.
- **POD dataset (edge coordinates):** At least 2 PODs exist near coordinate boundaries (e.g. near latitude 0, longitude 0, and near ±180 longitude) if the system supports such locations.
- **Caller identity / permissions:** A user or service account exists that can successfully call `GET /pod/list` through the API gateway with the same permissions used by existing POD list callers.
- **Baseline request:** A known-good baseline request exists that returns results using existing filters only (no coordinate filtering), so regression can be compared.

---

## TC-1 (Positive): Existing `/pod/list` request works unchanged when no coordinate parameters are provided

**Objective:** Verify backward compatibility: existing callers can keep using `GET /pod/list` without providing any coordinate parameters, and results/pagination are consistent with the pre-change behaviour.

**Preconditions:**
1. A baseline non-coordinate request is known to return results (e.g. using `prompt`, `podTypes`, pagination, sorting).
2. The caller has access to the endpoint through the API gateway.

**Steps:**
1. Call `GET /pod/list` using only existing query parameters used by current consumers (do not include any coordinate/bbox/radius parameters).
2. Repeat the same call at least twice.
3. Capture the response status, response body, and key response fields (e.g. number of items returned, total count if present, paging metadata if present).

**Expected result:** The request succeeds (e.g. HTTP 200) and returns results consistent with the baseline behaviour. No new mandatory parameters are introduced. The response schema remains compatible for existing clients (no breaking changes).

---

## TC-2 (Positive): Coordinate parameters are optional; adding them does not require changing existing non-coordinate filters

**Objective:** Verify that coordinate filtering can be added on top of existing filters without forcing changes to existing fields or making previously optional filters mandatory.

**Preconditions:**
1. At least one POD exists in a known geographic area A and at least one POD exists in area B far away.
2. A baseline request exists that can return PODs from both areas when no coordinate filter is applied.

**Steps:**
1. Call `GET /pod/list` with baseline filters only; record a subset of returned POD identifiers.
2. Call `GET /pod/list` with the same baseline filters plus a coordinate filter (e.g. `bbox=...` or `lat=...&lon=...` with a reasonable radius if supported) that targets area A.
3. Compare the returned POD identifiers between step 1 and step 2.

**Expected result:** Step 2 returns a subset filtered to the selected geographic area, while still respecting the baseline filters. No unexpected validation errors occur due to adding coordinate parameters.

---

## TC-3 (Negative): Invalid coordinate format is rejected (non-numeric values)

**Objective:** Ensure the backend validates coordinate inputs and rejects malformed values (e.g. letters, mixed tokens) with a clear client error.

**Preconditions:**
1. The caller can reach `GET /pod/list` through the API gateway.

**Steps:**
1. Call `GET /pod/list` with a coordinate-based filter but provide a non-numeric latitude (e.g. `lat=abc`) while other required/typical parameters are valid.
2. Call `GET /pod/list` with a non-numeric longitude (e.g. `lon=xyz`) while other parameters are valid.
3. If `bbox` is supported, call `GET /pod/list?bbox=not-a-bbox` (or an equivalent malformed bbox representation).

**Expected result:** The system returns a client error (e.g. HTTP 400) with a clear validation message identifying which parameter is invalid and why. No results are returned and no server error occurs.

---

## TC-4 (Negative): Out-of-range latitude/longitude values are rejected

**Objective:** Verify strict validation of coordinate ranges to prevent incorrect map queries.

**Preconditions:**
1. The caller can reach `GET /pod/list` through the API gateway.

**Steps:**
1. Call `GET /pod/list` with `lat=90.0001` (just above max) and a valid longitude.
2. Call `GET /pod/list` with `lat=-90.0001` (just below min) and a valid longitude.
3. Call `GET /pod/list` with `lon=180.0001` and a valid latitude.
4. Call `GET /pod/list` with `lon=-180.0001` and a valid latitude.

**Expected result:** Each request is rejected with a client validation error (e.g. HTTP 400). The error message clearly states the allowed range (latitude \([-90, 90]\), longitude \([-180, 180]\) or the system’s documented range).

---

## TC-5 (Positive): Boundary latitude/longitude values are accepted (range edges)

**Objective:** Confirm that valid boundary values are accepted and handled deterministically.

**Preconditions:**
1. The caller can reach `GET /pod/list` through the API gateway.

**Steps:**
1. Call `GET /pod/list` with `lat=90` and a valid `lon` value (e.g. 0) using the point-based filter variant (if supported).
2. Call `GET /pod/list` with `lat=-90` and a valid `lon` value.
3. Call `GET /pod/list` with `lon=180` and a valid `lat` value.
4. Call `GET /pod/list` with `lon=-180` and a valid `lat` value.

**Expected result:** The requests are accepted (e.g. HTTP 200). The response is returned successfully and does not error due to boundary inputs. Results may be empty depending on dataset, but behaviour is correct and stable.

---

## TC-6 (Negative): Partial coordinate input is rejected (missing one required parameter)

**Objective:** Ensure coordinate filters are not applied with incomplete parameters (avoiding ambiguous interpretation).

**Preconditions:**
1. Point-based filtering exists in at least one supported form (e.g. `lat` + `lon`, optionally with `radius`).

**Steps:**
1. Call `GET /pod/list` with `lat=<valid>` but omit `lon`.
2. Call `GET /pod/list` with `lon=<valid>` but omit `lat`.
3. If radius-based filtering is supported, call with `lat` + `lon` but omit `radius` (only if radius is required for that variant).

**Expected result:** The system rejects incomplete coordinate filters with a client error (e.g. HTTP 400) and clearly states which parameter(s) are missing.

---

## TC-7 (Positive): Empty result set is returned cleanly when the coordinate filter matches no PODs

**Objective:** Verify the system returns a valid “no results” response rather than errors when searching a geographic area with no PODs.

**Preconditions:**
1. There is a geographic region outside the dataset coverage (no PODs expected).

**Steps:**
1. Call `GET /pod/list` with a coordinate filter targeting a region that should contain no PODs (e.g. a bbox in an empty region).
2. Inspect the response and any paging metadata.

**Expected result:** The system returns success (e.g. HTTP 200) with an empty list and correct paging metadata (e.g. total = 0, page content empty), without server errors.

---

## TC-8 (Positive): PODs without coordinates are handled deterministically when coordinate filtering is used

**Objective:** Ensure predictable behaviour for PODs missing coordinates when coordinate-based search is requested.

**Preconditions:**
1. At least 3 PODs exist without coordinates.
2. At least 1 POD exists within the coordinate region used for the test.

**Steps:**
1. Call `GET /pod/list` with a coordinate filter that matches at least one POD with valid coordinates.
2. Check whether any POD without coordinates appears in the response.
3. Repeat step 1 using a different coordinate region, if feasible.

**Expected result:** PODs without coordinates are not incorrectly included in a coordinate-filtered result set. The system behaves consistently (either explicitly excludes such PODs from coordinate-filtered queries, or uses a documented fallback), and does not error due to missing coordinate data.

---

## TC-9 (Negative): Conflicting coordinate filter modes are rejected (bbox plus point/radius together)

**Objective:** Avoid ambiguous behaviour when multiple coordinate filtering modes are provided simultaneously.

**Preconditions:**
1. The endpoint supports at least two coordinate filtering modes (e.g. bbox and point-based search).

**Steps:**
1. Call `GET /pod/list` providing both a `bbox` parameter and a point-based parameter set (e.g. `lat` and `lon` and/or `radius`), with otherwise valid parameters.

**Expected result:** The system rejects the request with a client error (e.g. HTTP 400) indicating that only one coordinate filter mode can be used at a time, or it applies a deterministic documented precedence rule and returns a correct result accordingly. No server error occurs.

---

## TC-10 (Positive): Existing non-coordinate filters still work when coordinate filtering is enabled

**Objective:** Ensure combination of coordinate search and existing filters (e.g. POD types, voltage levels, measurement types, consumption purposes) works as an intersection, not a union.

**Preconditions:**
1. At least one POD exists inside the target region with POD type X and at least one POD inside the region with POD type Y.
2. The endpoint supports filtering by POD type (or another existing filter).

**Steps:**
1. Call `GET /pod/list` with a coordinate filter targeting the region, without specifying POD type; record returned PODs.
2. Call `GET /pod/list` with the same coordinate filter plus `podTypes=X`.
3. Compare the results.

**Expected result:** Step 2 returns only PODs that satisfy both the coordinate filter and the additional filter(s). No unexpected inclusions appear.

---

## TC-11 (Negative): Invalid pagination parameters are rejected when coordinate filtering is used

**Objective:** Validate request parameters consistently in coordinate mode, including paging.

**Preconditions:**
1. The endpoint supports pagination via `page` and `size` (or equivalent).

**Steps:**
1. Call `GET /pod/list` with a valid coordinate filter but set `size=0`.
2. Call `GET /pod/list` with `size=-1`.
3. Call `GET /pod/list` with `page=-1`.
4. Call `GET /pod/list` with very large `size` (e.g. above any documented max) while coordinate filter is present.

**Expected result:** Invalid pagination values are rejected with a clear client error (e.g. HTTP 400). If a maximum size exists, the system either rejects the request or caps the size deterministically and documents it in the response behaviour.

---

## TC-12 (Positive): Sorting can be applied together with coordinate filtering without errors

**Objective:** Ensure sorting parameters do not break coordinate-filtered queries.

**Preconditions:**
1. The endpoint supports sorting via `sortBy` and `sortDirection`.
2. The coordinate region returns at least 5 PODs (to observe ordering).

**Steps:**
1. Call `GET /pod/list` with a coordinate filter returning multiple results and include `sortBy=<a supported field>` and `sortDirection=ASC`.
2. Call the same request with `sortDirection=DESC`.
3. Compare ordering across the two responses.

**Expected result:** Both requests succeed and ordering changes as expected. No server errors occur.

---

## TC-13 (Negative): Unsupported sort field is rejected (coordinate mode)

**Objective:** Ensure invalid sorting parameters are validated and do not cause server failures.

**Preconditions:**
1. The endpoint supports sorting and performs validation on `sortBy`.

**Steps:**
1. Call `GET /pod/list` with a valid coordinate filter and set `sortBy` to an unsupported value (e.g. `sortBy=__invalid__`), with otherwise valid parameters.

**Expected result:** The system returns a client error (e.g. HTTP 400) indicating an invalid sort field, and does not return a server error.

---

## TC-14 (Positive): Coordinate filtering works consistently via API gateway path used by frontend map view

**Objective:** Validate the integration point “Frontend map view → API gateway → controller” for coordinate filtering, not just the controller in isolation.

**Preconditions:**
1. The API gateway route used by the frontend exists (e.g. `/api/pod/list` or equivalent mapping to `/pod/list`).
2. The caller can authenticate/authorize through the gateway.

**Steps:**
1. Call the API gateway route with a coordinate filter (bbox or point-based) and valid baseline parameters.
2. Verify the response content matches the same query executed directly on the internal route (if direct access is available in the test environment).

**Expected result:** The API gateway forwards parameters correctly, the response is successful, and the filtered results are correct. No parameter loss or transformation issues occur.

---

## TC-15 (Negative): Coordinate parameters with extra whitespace or locale-specific decimal separators are rejected or normalized consistently

**Objective:** Ensure robust parsing for real-world clients that may send slightly messy values.

**Preconditions:**
1. The endpoint parses numeric inputs from query parameters.

**Steps:**
1. Call `GET /pod/list` with `lat= 45.123 ` (leading/trailing whitespace) and a valid longitude; observe behaviour.
2. Call `GET /pod/list` with `lat=45,123` (comma decimal separator) and a valid longitude; observe behaviour.
3. Call `GET /pod/list` with `bbox` values containing spaces after commas (if bbox uses comma-separated values).

**Expected result:** The system behaves consistently according to documented parsing rules: either trims whitespace and accepts valid numeric values, or rejects them with a clear validation error. Locale-specific commas must not be silently misinterpreted (e.g. `45,123` must not become `45123`).

---

## References

- **Jira:** PHN-2197 – Backend GET: POD list - Search of pods by Coordinates on Map view.
- **Endpoint (from scope):** GET `/pod/list` (existing search endpoint to be extended).
- **Integration points (from cross-dependency data):** Frontend map view → API gateway → point-of-delivery-controller; existing `/pod/list` callers; contract flows selecting POD.

