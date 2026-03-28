# POD coordinate search – Point and radius / proximity search scenarios (PHN-2197)

**Jira:** PHN-2197 (Phoenix)  
**Type:** Task  
**Summary:** The POD list endpoint should support searching PODs by a map “center point” (latitude/longitude) with an optional radius (distance) for proximity-based map view queries. Validation must prevent ambiguous or unsafe requests.

**Scope:** This document covers point-based coordinate search for **GET `/pod/list`** as inferred from the ticket title (map view coordinate search). Because the exact API shape is unknown, the tests cover alternative parameter designs (e.g. `lat`/`lon` alone meaning “nearest” search, or `lat`/`lon` + `radius` meaning “within radius”), unit variants (meters vs kilometers), and expected validation. It also covers edge cases such as radius = 0, extremely large radius, and behaviour when POD coordinates are missing.

---

## Test data (preconditions)

- **Environment:** Test (or as per ticket).
- **Center area C:** A geographic area C is chosen with at least 5 PODs whose coordinates are known and clustered within a few kilometers.
- **Near vs far PODs:** At least 2 PODs exist far outside area C (tens/hundreds of kilometers away).
- **Boundary POD (distance):** If feasible, one POD exists approximately at the chosen radius boundary from the center point.
- **POD without coordinates:** At least 3 PODs exist with missing coordinates.
- **Caller identity / permissions:** Caller can access `GET /pod/list` through the gateway.

---

## TC-1 (Positive): Point + radius returns PODs within radius and excludes PODs outside

**Objective:** Verify that proximity filtering works for the map view use case: results are limited to PODs within a specified distance from a center point.

**Preconditions:**
1. A center point (latC, lonC) near clustered PODs is known.
2. A radius R is chosen such that at least 3 clustered PODs are within R and far PODs are outside R.

**Steps:**
1. Call `GET /pod/list` with point-based parameters (e.g. `lat=latC&lon=lonC&radius=R`) using the implemented parameter names.
2. Record returned POD identifiers.
3. Verify known clustered PODs are included.
4. Verify known far PODs are excluded.

**Expected result:** The response succeeds and returns only PODs within the radius. Far PODs are not returned.

---

## TC-2 (Positive): Radius boundary inclusion is consistent (POD exactly at radius)

**Objective:** Ensure deterministic inclusion/exclusion for a POD located exactly at the radius boundary, important for map zoom consistency.

**Preconditions:**
1. A POD exists approximately at distance R from the center point (or test data can be prepared).

**Steps:**
1. Call the point+radius search with radius = R and record whether the boundary POD is included.
2. Repeat with radius slightly smaller than R.
3. Repeat with radius slightly larger than R.

**Expected result:** The system follows a consistent boundary rule (preferably inclusive for distance = R). Inclusion changes only when the radius meaningfully changes, not unpredictably.

---

## TC-3 (Negative): Non-numeric radius is rejected

**Objective:** Validate that radius parsing is strict and returns clear client errors for invalid values.

**Preconditions:**
1. Radius is supported in at least one variant.

**Steps:**
1. Call `GET /pod/list` with valid `lat`/`lon` but `radius=abc`.
2. Call `GET /pod/list` with `radius=` (empty).

**Expected result:** The system rejects the request with a client error (e.g. HTTP 400) stating the radius is invalid/missing.

---

## TC-4 (Negative): Negative radius is rejected

**Objective:** Ensure invalid radius values do not result in incorrect or expensive queries.

**Preconditions:**
1. Point+radius search is supported.

**Steps:**
1. Call `GET /pod/list` with valid `lat`/`lon` and `radius=-1` (or another negative value).

**Expected result:** The system returns a client error (e.g. HTTP 400) indicating radius must be a positive number (or non-negative if 0 is allowed).

---

## TC-5 (Positive): Radius = 0 behaves deterministically

**Objective:** Validate behaviour for “radius zero” queries, which may occur at extreme zoom levels.

**Preconditions:**
1. A POD exists exactly at the center point coordinate (if feasible), or the dataset can be prepared.

**Steps:**
1. Call `GET /pod/list` with `lat`/`lon` set to the POD coordinate and `radius=0`.
2. Observe results.

**Expected result:** The behaviour is deterministic: either returns only PODs exactly at the point (if any), or rejects radius=0 with a clear validation message. It must not return a broad unrelated set.

---

## TC-6 (Positive): If the API supports point-only mode, it returns nearest PODs deterministically

**Objective:** Cover an alternative design where radius is optional and point-only queries return nearest results for map view.

**Preconditions:**
1. The system supports calling the endpoint with `lat`/`lon` without `radius`, or this is a plausible implementation variant.

**Steps:**
1. Call `GET /pod/list` with only `lat` and `lon` (no radius) and otherwise valid baseline parameters.
2. Observe whether the response is accepted and which results are returned.

**Expected result:** One of the following is true and consistent:
1. The system accepts point-only mode and returns a deterministic set (e.g. nearest N PODs) with clear ordering, or
2. The system rejects point-only mode with a clear client error stating radius is required.

---

## TC-7 (Negative): Unreasonably large radius is rejected or constrained (performance guard)

**Objective:** Prevent overly expensive queries from map view due to client mistakes (e.g. radius in wrong units).

**Preconditions:**
1. The endpoint supports radius.

**Steps:**
1. Call the endpoint with a very large radius (e.g. large enough to cover a country/continent) while keeping pagination reasonable.
2. Call again with an extremely large radius (e.g. “whole world” scale).

**Expected result:** The system remains stable and responds within acceptable time. If there is a maximum allowed radius, the system rejects the request with a clear error or caps the radius deterministically. It must not crash or time out.

---

## TC-8 (Negative): Conflicting coordinate filters are rejected (radius mode + bbox)

**Objective:** Ensure point/radius filters cannot be combined with bbox in a single request without clear rules.

**Preconditions:**
1. Bbox mode exists or is planned alongside point/radius mode.

**Steps:**
1. Call `GET /pod/list` with `lat`/`lon`/`radius` and also provide `bbox` in the same request.

**Expected result:** The request is rejected with HTTP 400 and a clear message about conflicting filters, or a documented precedence is applied. No server error occurs.

---

## TC-9 (Positive): PODs without coordinates are excluded from point/radius results

**Objective:** Ensure missing coordinate PODs do not appear in coordinate-filtered results.

**Preconditions:**
1. At least 3 PODs exist without coordinates.
2. The point/radius search used for the test returns at least one POD with valid coordinates.

**Steps:**
1. Call the point/radius search.
2. Verify whether any returned POD has missing coordinates.

**Expected result:** PODs without coordinates are not returned (unless explicitly documented otherwise). The system remains stable and correct.

---

## TC-10 (Positive): Point/radius search combined with existing filters returns intersection

**Objective:** Ensure point/radius filtering works together with existing filters (e.g. POD types, voltage levels) as an intersection.

**Preconditions:**
1. In the target radius region, at least one POD matches filter X and at least one does not.

**Steps:**
1. Call point/radius search without additional filters; record results.
2. Call point/radius search with an existing filter (e.g. `podTypes=X`).
3. Compare results.

**Expected result:** The filtered request returns a subset that satisfies both coordinate and existing filter(s).

---

## TC-11 (Negative): Units mismatch is detected or produces clearly bounded results (meters vs kilometers)

**Objective:** Cover a common ambiguity where clients may pass radius in the wrong unit; ensure it does not silently create unsafe broad queries.

**Preconditions:**
1. The API either documents a radius unit or leaves it ambiguous.

**Steps:**
1. Call point/radius with a small radius value that would be reasonable in meters (e.g. 500) and observe result scale.
2. Call point/radius with a small radius value that would be reasonable in kilometers (e.g. 5) and observe results.
3. Compare behaviours and infer unit rules.

**Expected result:** The behaviour is consistent with a single unit interpretation and is either documented in the response/error, or can be verified from results. If radius values suggest a unit mismatch (e.g. “500” returns a huge region), the system should have guardrails (max radius, validation, or clear documentation) to prevent misuse.

---

## TC-12 (Positive): Precision handling (many decimal places) works without rounding errors causing unexpected inclusions/exclusions

**Objective:** Ensure coordinate precision does not produce unexpected results for map view panning/zooming.

**Preconditions:**
1. A center point is selected with many decimal places.

**Steps:**
1. Call point/radius search with `lat`/`lon` values including 6–8 decimal places.
2. Repeat with the same values rounded to fewer decimals (e.g. 4 decimals).
3. Compare result sets.

**Expected result:** Differences are explainable and minimal (only PODs near the boundary may change). The system does not error due to precision.

---

## References

- **Jira:** PHN-2197 – Backend GET: POD list - Search of pods by Coordinates on Map view.
- **Integration points:** Frontend map view coordinate queries; API gateway forwarding; existing `/pod/list` callers.
- **What could break:** Pagination/sort with coordinate filters; PODs without coordinates; contract flows selecting POD.

