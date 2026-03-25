# POD coordinate search – Pagination, sorting, and regression scenarios (PHN-2197)

**Jira:** PHN-2197 (Phoenix)  
**Type:** Task  
**Summary:** Coordinate-filtered POD search must integrate correctly with pagination and sorting, and must not break existing flows that depend on `/pod/list` (e.g. contract creation/termination flows selecting PODs, existing automated tests and Postman collections).

**Scope:** This document focuses on the integration and regression risks identified in the cross-dependency data: **existing `/pod/list` callers**, **pagination and sorting with bbox**, **PODs without coordinates**, and **contract flows that list/select POD**. It validates stable paging across map interactions (panning/zooming), deterministic sorting, and that coordinate search does not introduce breaking changes in API gateway routing or response structure.

---

## Test data (preconditions)

- **Environment:** Test (or as per ticket).
- **Large POD dataset in one area:** At least 50 PODs exist within a single geographic area A so that multiple pages are required (e.g. `size=10` yields ≥5 pages) when bbox/point filters target that area.
- **Stable attribute for sorting:** There is at least one sortable field that is stable across requests (e.g. POD identifier, name, create date) so ordering can be validated.
- **PODs without coordinates:** At least 3 PODs exist without coordinates.
- **Contract flow preconditions:** A user with permissions exists to open any UI or API flow that lists/selects PODs for contract creation or termination processes, if available in the environment.
- **Known baseline for non-coordinate `/pod/list`:** A baseline call exists used by existing callers for regression comparison.

---

## TC-1 (Positive): Pagination works with bbox filter (page 0..N returns disjoint, complete result set)

**Objective:** Ensure coordinate-filtered results can be paged reliably, which is required for list views and potentially for incremental map loading.

**Preconditions:**
1. A bbox exists that matches at least 50 PODs.
2. Pagination parameters are supported (`page` and `size` or equivalent).

**Steps:**
1. Call `GET /pod/list` with the bbox and `size=10&page=0` and record returned POD identifiers.
2. Repeat for `page=1`, `page=2`, and `page=3`.
3. Check for duplicates across pages.
4. Optionally, call the first page again to check stability.

**Expected result:** Each page returns a disjoint subset (no duplicates across pages) and together pages represent a consistent slice of the full bbox-filtered result set. Repeated calls with the same parameters return consistent paging.

---

## TC-2 (Negative): Pagination does not break when requesting a page beyond the last page

**Objective:** Ensure the API handles out-of-range page requests gracefully.

**Preconditions:**
1. A bbox exists that returns a known total count and a known number of pages at the chosen `size`.

**Steps:**
1. Call `GET /pod/list` with bbox and a page index well beyond the last page (e.g. `page=999` with `size=10`).
2. Observe the response.

**Expected result:** The API returns success with an empty list (preferred) or returns a client error indicating page is out of range, but does not return a server error. Behaviour is consistent with non-coordinate paging rules.

---

## TC-3 (Positive): Sorting works with bbox filter (ASC vs DESC is consistent)

**Objective:** Verify that sorting is applied after coordinate filtering and is deterministic.

**Preconditions:**
1. A bbox exists that returns at least 20 PODs.
2. A stable sortable field exists (e.g. POD identifier).

**Steps:**
1. Call `GET /pod/list` with bbox, `sortBy=<stableField>`, `sortDirection=ASC`, `size=20`.
2. Call the same request with `sortDirection=DESC`.
3. Compare the ordering of the first and last elements.

**Expected result:** Both calls succeed and the order is correctly reversed. No server errors occur.

---

## TC-4 (Negative): Sorting + pagination does not produce duplicates or missing items across pages for stable sorting

**Objective:** Prevent a common paging bug where ordering is unstable across pages, which would cause duplicates/missing items in list/map results.

**Preconditions:**
1. A bbox exists that returns at least 50 PODs.
2. Sorting by a stable unique field is supported (e.g. POD identifier).

**Steps:**
1. Call bbox search with `sortBy=<uniqueStableField>`, `sortDirection=ASC`, `size=10`, for pages 0..4.
2. Combine all identifiers and check for duplicates.
3. Change only the page size (e.g. `size=25`) and compare the union of the first two pages with the first five pages from size=10 (they should represent the same first 50 items for stable sorting).

**Expected result:** No duplicates occur. The result slices are consistent when sorting by a stable unique field.

---

## TC-5 (Positive): Coordinate search does not alter response schema expected by existing callers

**Objective:** Protect existing `/pod/list` callers by ensuring response fields remain compatible when coordinate filtering is introduced.

**Preconditions:**
1. A baseline non-coordinate request exists and its response schema is known (at least key fields used by clients).

**Steps:**
1. Call baseline `/pod/list` request (no coordinate params) and record response structure (top-level fields, list element fields).
2. Call a coordinate-filtered request (bbox or point/radius) and record response structure.
3. Compare key schema aspects: presence of list field, paging metadata fields, and POD item fields used by clients (e.g. identifier, type, status, and coordinate fields if present).

**Expected result:** Response schema remains compatible (no breaking renames/removals). If new fields are added (e.g. coordinate fields), they are additive and do not break existing clients.

---

## TC-6 (Negative): PODs without coordinates do not break pagination and sorting when coordinate filtering is enabled

**Objective:** Ensure missing coordinate PODs do not cause null-handling errors or unstable sorting.

**Preconditions:**
1. There are PODs without coordinates in the overall dataset.

**Steps:**
1. Call coordinate-filtered request (bbox or radius) that returns multiple results.
2. Apply sorting and request multiple pages.
3. Observe whether any error occurs and whether results are consistent.

**Expected result:** The system does not error. PODs without coordinates are excluded from coordinate-filtered results (preferred) or handled deterministically. Pagination and sorting remain stable.

---

## TC-7 (Positive): Panning/zooming regression – small bbox changes result in expected incremental changes

**Objective:** Simulate map view behaviour: moving/zooming the bbox should change the result set predictably.

**Preconditions:**
1. A dense area exists with many PODs across a wider region.

**Steps:**
1. Query bbox A (initial view) and record a set of identifiers.
2. Query bbox B that slightly shifts the view east/west (overlapping A significantly) and record identifiers.
3. Query bbox C that zooms in (smaller box inside A) and record identifiers.
4. Compare overlaps and differences.

**Expected result:** Results change in a predictable way: bbox B shares many items with A but includes/excludes items near the shifted edges; bbox C is largely a subset of A. No unexpected spikes or empty results occur unless the bbox truly has no PODs.

---

## TC-8 (Positive): Contract-related flows that list/select POD still work (regression)

**Objective:** Validate that introducing coordinate params does not break flows where POD list is used for contract creation/termination selection.

**Preconditions:**
1. A contract-related UI or API flow exists that uses POD list/search.
2. A user can access and operate that flow in the environment.

**Steps:**
1. Open the contract creation (or termination) flow that requires selecting or listing PODs.
2. Use the POD search/list component without map view (standard list filtering) and verify it still returns results.
3. If map view is present, use the map view to trigger coordinate-based POD loading and verify results are displayed/usable.

**Expected result:** The contract flow still works end-to-end. Standard POD listing and selection is unaffected. Map-driven coordinate search works if exposed in the UI.

---

## TC-9 (Negative): Existing callers sending unknown parameters are handled according to existing rules (no silent break)

**Objective:** Ensure introducing new coordinate parameters does not change how the API handles unknown/unexpected parameters.

**Preconditions:**
1. Baseline behaviour for unknown query parameters is known (either ignored or rejected).

**Steps:**
1. Call `/pod/list` without coordinate params and add an unknown parameter (e.g. `unexpectedParam=1`).
2. Call `/pod/list` with coordinate params and also add an unknown parameter.

**Expected result:** Behaviour is consistent between coordinate and non-coordinate requests: unknown parameters are either ignored or rejected consistently. No server errors occur.

---

## TC-10 (Positive): API gateway forwards coordinate parameters correctly (integration regression)

**Objective:** Validate the gateway integration point does not drop or rename bbox/coordinate parameters.

**Preconditions:**
1. API gateway route for POD list exists and is reachable.

**Steps:**
1. Call the gateway route (e.g. `/api/pod/list`) with bbox parameters and paging/sorting.
2. Verify response matches expectations (filtered results, correct paging).
3. If possible, compare with calling the internal route directly (same query) to ensure parity.

**Expected result:** Gateway forwards coordinate parameters correctly, returning the same filtered results and metadata as the internal route.

---

## TC-11 (Negative): Concurrency regression – repeated rapid map requests do not cause inconsistent server errors

**Objective:** Simulate map UI behaviour sending rapid successive requests while user pans/zooms; ensure backend remains stable.

**Preconditions:**
1. A bbox that returns results is known.

**Steps:**
1. Send 10 requests in quick succession with slightly varying bbox values (small shifts) while keeping other filters constant.
2. Record response status codes and any error messages.

**Expected result:** All requests succeed (or fail only with valid client validation errors if inputs are invalid). No intermittent 5xx errors occur due to load or parsing issues.

---

## TC-12 (Positive): Regression for automated artifacts – existing Postman/EnergoTS tests for `/pod/list` remain valid

**Objective:** Ensure coordinate additions do not break existing automated tests that call `/pod/list` without coordinate parameters.

**Preconditions:**
1. There are existing automated tests or Postman collections that call `/pod/list`.

**Steps:**
1. Identify at least one existing automated call (e.g. from a Postman collection or EnergoTS test) that hits `/pod/list` without coordinate params.
2. Execute that call (manually via Postman, or by reproducing the same request) in the same environment.
3. Verify the call still succeeds and returns expected structure.

**Expected result:** Existing `/pod/list` test calls succeed unchanged. The coordinate enhancement is backward compatible.

---

## References

- **Jira:** PHN-2197 – Backend GET: POD list - Search of pods by Coordinates on Map view.
- **What could break:** Existing callers; pagination/sort with bbox; PODs without coordinates; contract/termination flows selecting POD; EnergoTS/Postman tests.

