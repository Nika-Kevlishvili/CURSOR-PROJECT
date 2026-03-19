# PHN-2529 - Pagination, Sorting, and Consistency

**Scope:** Validate paging correctness, stable ordering, and deterministic multi-page reads.

## Preconditions

- Dataset size exceeds one page.
- Test data includes ties for primary sort key to validate secondary ordering stability.
- Known reference query for reproducible page traversal.

## TC-1: Page size and page number are respected

**Steps:**
1. Call endpoint with `page=0`, `size=N`.
2. Call endpoint with `page=1`, same `size`.
3. Compare counts and item continuity.

**Expected result:**
- Each page returns at most `N` items.
- Pagination metadata matches requested page and size.
- No overlap unless contract explicitly allows it.

## TC-2: Deterministic ordering across repeated calls

**Steps:**
1. Execute the same request multiple times.
2. Compare item order and IDs per page.

**Expected result:**
- Order is stable across identical calls.
- Results do not shuffle between calls without data changes.

## TC-3: Stable ordering when sort key ties exist

**Steps:**
1. Query dataset where multiple items share the same primary sort value.
2. Validate secondary/implicit tie-break ordering behavior.

**Expected result:**
- Tied items are consistently ordered.
- No non-deterministic jumps across pages.

## TC-4: No duplicates or missing items when walking all pages

**Steps:**
1. Retrieve all pages sequentially for the same filter.
2. Build a set of returned IDs.
3. Compare set size with summed unique results.

**Expected result:**
- No duplicate IDs across pages.
- No missing items relative to expected full result.

## TC-5: Boundary conditions for pagination inputs

**Steps:**
1. Test `size=1`, maximum allowed `size`, and out-of-range size.
2. Test negative page or non-numeric page/size input.

**Expected result:**
- Valid boundaries succeed.
- Invalid inputs return validation errors with consistent contract.

## TC-6: Performance guardrail for paginated query

**Steps:**
1. Run representative request under realistic data volume.
2. Measure response time for first page and deep page.

**Expected result:**
- Response time remains within agreed SLA/guardrail.
- No major degradation for deep pagination patterns.
# Pagination sorting and consistency

## TC-PAG-01 - First page metadata correctness
**Rationale:** Validates native paginated query contract.

**Preconditions**
- Valid auth token is available.
- Dataset has more records than one page size for selected coordinates/radius.

**Steps**
1. Request page `0` with size `N` and valid coordinates.
2. Verify metadata fields (`page`, `size`, `totalElements`, `totalPages`, `numberOfElements`).

**Expected result**
- Metadata is internally consistent.
- `numberOfElements` is `<= size`.

## TC-PAG-02 - Page transition has no overlaps or gaps
**Rationale:** Covers pagination inconsistency risk.

**Preconditions**
- Valid auth token is available.
- Stable test dataset for repeated requests.

**Steps**
1. Request page `0` and page `1` with same filters and size.
2. Compare customer identifiers across pages.

**Expected result**
- No duplicate records between adjacent pages for stable dataset.
- Combined records reflect deterministic ordering.

## TC-PAG-03 - Last page boundary handling
**Rationale:** Ensures repository paging implementation handles upper boundaries.

**Preconditions**
- Valid auth token is available.
- Dataset has known `totalPages` > 0.

**Steps**
1. Determine `totalPages`.
2. Request the last valid page.
3. Request one page beyond the last.

**Expected result**
- Last valid page returns remaining records correctly.
- Out-of-range page returns empty `content` or project-standard behavior without server error.

## TC-PAG-04 - Size parameter boundary behavior
**Rationale:** Checks max/min page size handling and defaults.

**Preconditions**
- Valid auth token is available.
- Allowed min/max page size documented.

**Steps**
1. Call endpoint with minimal allowed size.
2. Call endpoint with maximal allowed size.
3. Call endpoint with size outside allowed range.

**Expected result**
- Allowed sizes work correctly.
- Out-of-range size is rejected with validation error or clamped per API contract.

## TC-PAG-05 - Deterministic ordering across repeated calls
**Rationale:** Covers high-risk shared repository ordering drift from `what_could_break`.

**Preconditions**
- Valid auth token is available.
- No dataset changes during test run window.

**Steps**
1. Call same request (same coordinates/radius/page/size) three times.
2. Compare record order and identifiers in each response.

**Expected result**
- Responses are identical in order and membership.
- No random shuffling across calls.

## TC-PAG-06 - Pagination defaults when page/size omitted
**Rationale:** Verifies default paging behavior and protects client compatibility.

**Preconditions**
- Valid auth token is available.
- API default page/size values are known.

**Steps**
1. Call endpoint without `page` and `size`.
2. Call endpoint explicitly with default page/size values.
3. Compare metadata and content.

**Expected result**
- Omitted params use documented defaults.
- Results match explicit default values.
- No hidden behavior change across deployments.

## TC-PAG-07 - Sorting correctness and stability for documented default order
**Rationale:** Ensures the native query and repository paging return results in the documented default sort order.

**Preconditions**
- Valid auth token is available.
- Documented default sort order for this endpoint is known (e.g., classification, then name, or other defined key sequence).
- Dataset contains records that exercise sort-key ties and ordering differences.

**Steps**
1. Call endpoint with coordinates/radius that return multiple pages of data using only default sort (no explicit `sort` parameter).
2. Verify that the order within a single page matches the documented default sort order using underlying data as reference.
3. Walk all pages and verify that the combined list remains correctly sorted and stable across repeated identical calls.

**Expected result**
- All pages respect the documented default sort order.
- Tie-breaking behaviour is consistent and deterministic across calls.
- No reordering or drift occurs between identical requests for the same stable dataset.
