# POD coordinate search – Bounding box (bbox) search scenarios (PHN-2197)

**Jira:** PHN-2197 (Phoenix)  
**Type:** Task  
**Summary:** The POD list endpoint must support searching PODs by a geographic bounding box (bbox) for map view use cases, returning only PODs whose coordinates fall inside the requested area, while preserving paging/sorting and existing filters.

**Scope:** This document covers the bounding-box variant of coordinate search for **GET `/pod/list`** as used by map view. Because the ticket description is empty, the tests include validation and alternative formats for bbox (e.g. `bbox=minLon,minLat,maxLon,maxLat` vs `bbox=minLat,minLon,maxLat,maxLon`) and explicitly verify whichever format is implemented. It also covers edge conditions (dateline crossing, poles, zero-area boxes), correctness of inclusion/exclusion at boundaries, and handling of PODs with missing coordinates.

---

## Test data (preconditions)

- **Environment:** Test (or as per ticket).
- **POD dataset (area A):** At least 5 PODs exist within a known small area A (e.g. within a 1–5 km rectangle) and their coordinates are known.
- **POD dataset (area B):** At least 5 PODs exist within a different known area B far away and their coordinates are known.
- **POD at boundary:** At least 2 PODs exist exactly on a bbox boundary line (one on min boundary, one on max boundary) if feasible.
- **POD without coordinates:** At least 3 PODs exist with missing coordinates.
- **Baseline filters:** At least one filter (e.g. `podTypes` or `voltageLevels`) is usable to further narrow results inside the bbox.

---

## TC-1 (Positive): Bbox search returns PODs inside the box and excludes PODs outside

**Objective:** Verify the core bbox behaviour for map view: only PODs inside the bbox are returned.

**Preconditions:**
1. Area A bbox coordinates are known and include at least 5 PODs.
2. Area B PODs are outside the area A bbox.

**Steps:**
1. Call `GET /pod/list` with a bbox that tightly covers area A (use the implemented bbox format).
2. Record returned POD identifiers.
3. Verify at least one known “area A” POD is present in results.
4. Verify no known “area B” POD is present in results.

**Expected result:** The response succeeds and includes PODs in area A while excluding PODs outside the bbox.

---

## TC-2 (Positive): PODs exactly on bbox boundaries are handled consistently (inclusive boundaries)

**Objective:** Ensure deterministic boundary inclusion rules, important for panning/tiling in map view.

**Preconditions:**
1. A POD exists exactly on the bbox min boundary line.
2. A POD exists exactly on the bbox max boundary line.

**Steps:**
1. Call `GET /pod/list` with a bbox whose boundary matches the boundary POD coordinates exactly.
2. Check whether boundary PODs are included.
3. Repeat the call with a slightly smaller bbox (shrinking by a tiny delta) to move the boundary just inside the boundary POD point.

**Expected result:** The system follows a consistent rule (preferably inclusive of boundary points). The boundary behaviour is stable and does not flip unpredictably.

---

## TC-3 (Negative): Bbox with min > max is rejected (invalid ordering)

**Objective:** Validate bbox ordering to prevent invalid map queries.

**Preconditions:**
1. Bbox search is implemented via a `bbox` parameter.

**Steps:**
1. Call `GET /pod/list` with bbox values where min latitude is greater than max latitude (e.g. `minLat=50`, `maxLat=49`), using the implemented parameter ordering.
2. Call `GET /pod/list` with bbox values where min longitude is greater than max longitude (e.g. `minLon=20`, `maxLon=19`) if the system does not support dateline-crossing representation.

**Expected result:** The request is rejected with a clear client error (e.g. HTTP 400) describing the invalid bbox ordering, unless dateline-crossing boxes are explicitly supported (in which case the behaviour must be documented and deterministic).

---

## TC-4 (Negative): Bbox with missing or wrong number of coordinates is rejected

**Objective:** Ensure strict input validation for bbox format.

**Preconditions:**
1. Bbox is provided as a single parameter (e.g. comma-separated list) or as multiple parameters (min/max).

**Steps:**
1. Call `GET /pod/list` with `bbox` containing fewer than required numbers (e.g. only 2 or 3 values).
2. Call `GET /pod/list` with `bbox` containing more than required numbers (e.g. 5 values).
3. Call `GET /pod/list` with empty bbox (e.g. `bbox=`).

**Expected result:** Each request is rejected with a client validation error (e.g. HTTP 400) clearly stating the expected bbox format.

---

## TC-5 (Positive): Alternative bbox ordering is either accepted or rejected explicitly (format discovery)

**Objective:** Because the requirement is inferred from title only, verify which bbox ordering is implemented and ensure the non-implemented ordering fails clearly (no silent wrong results).

**Preconditions:**
1. Area A bbox coordinates are known.

**Steps:**
1. Call `GET /pod/list` using bbox ordering candidate #1 (e.g. `minLon,minLat,maxLon,maxLat`) and record results.
2. Call `GET /pod/list` using bbox ordering candidate #2 (e.g. `minLat,minLon,maxLat,maxLon`) and record results.
3. Compare which call returns correct “area A” PODs and excludes “area B” PODs.

**Expected result:** Exactly one ordering produces correct filtering (or both if the API accepts both explicitly). If an ordering is not supported, the system rejects it with a clear validation error rather than returning misleading results.

---

## TC-6 (Positive): Bbox search combined with existing filters returns the intersection

**Objective:** Ensure bbox filtering works with existing filters commonly used by callers and map view.

**Preconditions:**
1. Within area A, at least one POD has type X and at least one has type Y (or another filterable attribute).
2. Filtering by that attribute is supported.

**Steps:**
1. Call `GET /pod/list` with bbox for area A without additional filters; record results.
2. Call `GET /pod/list` with the same bbox plus `podTypes=X` (or the chosen existing filter).
3. Compare results.

**Expected result:** Step 2 results are a subset of step 1, containing only PODs inside bbox that also match the additional filter.

---

## TC-7 (Positive): Zero-area bbox (min == max) behaves deterministically

**Objective:** Validate behaviour for a bbox that collapses to a point (or a line), which can occur due to UI bugs or extreme zoom.

**Preconditions:**
1. A POD exists at a known coordinate P (latP, lonP).

**Steps:**
1. Call `GET /pod/list` with bbox where min and max coordinates are equal to P (zero-area).
2. Observe whether the POD at P is returned.

**Expected result:** The system behaves deterministically: either treats it as a point query (returns POD(s) exactly at that point) or rejects it with a validation error stating bbox must have non-zero area. It must not return unrelated results.

---

## TC-8 (Negative): Bbox values outside allowed lat/lon ranges are rejected

**Objective:** Ensure range validation also applies to bbox endpoints.

**Preconditions:**
1. Bbox accepts numeric inputs.

**Steps:**
1. Call `GET /pod/list` with a bbox containing latitude above 90 or below -90.
2. Call `GET /pod/list` with a bbox containing longitude above 180 or below -180.

**Expected result:** The system returns a client validation error (e.g. HTTP 400) explaining invalid ranges.

---

## TC-9 (Positive): PODs without coordinates are excluded from bbox results

**Objective:** Ensure missing coordinate PODs do not pollute map results.

**Preconditions:**
1. At least 3 PODs exist without coordinates.
2. The bbox used for the test matches at least one POD with valid coordinates.

**Steps:**
1. Call `GET /pod/list` with a bbox that matches multiple PODs with coordinates.
2. Verify whether any returned POD has missing coordinates.

**Expected result:** PODs without coordinates are not returned in bbox-filtered results (unless the API explicitly documents a fallback). No server errors occur.

---

## TC-10 (Positive): Dateline-crossing scenario is either supported or rejected clearly

**Objective:** Validate behaviour around the ±180 longitude dateline, which is a common edge case for bbox logic.

**Preconditions:**
1. Either (a) the dataset contains PODs near +179 and -179 longitudes, or (b) a mock/test dataset can be used in the environment.

**Steps:**
1. Call `GET /pod/list` with a bbox intended to cross the dateline (e.g. minLon=170, maxLon=-170) using the implemented format.
2. Observe whether the system supports this representation (returns expected results) or rejects it.

**Expected result:** The system either supports dateline-crossing bbox queries and returns the correct set of PODs, or rejects such bbox with a clear validation error that indicates the limitation and how clients should query instead.

---

## TC-11 (Negative): Extremely large bbox (whole world) respects max response constraints

**Objective:** Ensure bbox queries cannot unintentionally cause performance or payload issues; paging must still apply.

**Preconditions:**
1. The endpoint supports pagination.

**Steps:**
1. Call `GET /pod/list` with a bbox that covers the maximum supported area (e.g. whole world) and `size` set to a reasonable value.
2. Call the same request with an extremely large `size` (above any documented max), if allowed to test.

**Expected result:** The system returns paged results without timing out. If a maximum `size` exists, it is enforced (reject or cap) consistently, and the system remains stable.

---

## TC-12 (Negative): Bbox search does not accept both bbox and point/radius in the same request

**Objective:** Ensure coordinate filter mode conflict is prevented for bbox-specific requests.

**Preconditions:**
1. Point/radius search exists (or is planned) alongside bbox.

**Steps:**
1. Call `GET /pod/list` with a bbox and also provide `lat`/`lon` (and `radius` if used) in the same request.

**Expected result:** The system rejects the request with a clear client error (e.g. HTTP 400) indicating conflicting coordinate filters, or applies a documented precedence deterministically (but must not silently return misleading results).

---

## References

- **Jira:** PHN-2197 – Backend GET: POD list - Search of pods by Coordinates on Map view.
- **Entry point:** GET `/pod/list` (search).
- **What could break (regression focus):** Pagination and sort with bbox; PODs without coordinates; existing `/pod/list` callers.

