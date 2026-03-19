# PHN-2529 - Happy Path and Response Mapping

**Scope:** Validate successful endpoint behavior and response data contract mapping.

## Preconditions

- Environment has seeded customer data around known coordinates.
- Valid authenticated token with access to sales portal API.
- Known baseline request with deterministic expected result set.

## TC-1: Happy path returns expected customers for valid coordinates

**Steps:**
1. Call `GET /sales-portal/customer/by-coordinates` with valid latitude/longitude and default paging.
2. Capture response status, payload, and item count.
3. Compare returned customers with expected fixture window for that coordinate area.

**Expected result:**
- HTTP success response.
- Non-empty payload when matching data exists.
- Returned customers belong to expected geographic search result.

## TC-2: Response schema contains all required contract fields

**Steps:**
1. Execute a valid request.
2. Verify each item contains mandatory fields from projection/response model.
3. Validate field types (string/number/boolean/date) against contract.

**Expected result:**
- All required fields are present for each item.
- No type drift for contract fields.
- No unexpected nulls in mandatory fields.

## TC-3: Optional fields remain backward compatible

**Steps:**
1. Execute request for dataset containing both complete and partial customer records.
2. Validate optional fields behavior (`null` or omitted) is consistent with API contract.
3. Ensure clients can parse response when optional fields are absent.

**Expected result:**
- Optional field behavior is consistent and documented.
- Response remains parseable by existing clients.

## TC-4: Empty result set still returns valid contract

**Steps:**
1. Call endpoint with coordinates known to have no nearby customers.
2. Validate status code and payload structure.

**Expected result:**
- Success status with empty collection.
- Pagination metadata and wrapper contract remain valid.
- No schema deviation between empty and non-empty results.

## TC-5: Distance/coordinate-related fields are internally consistent

**Steps:**
1. Execute request with known coordinates and controlled nearby customers.
2. Validate each returned item has coherent coordinate-related fields (if exposed).
3. Ensure no impossible values (invalid range, missing pair values, malformed precision).

**Expected result:**
- Coordinate-derived fields are valid and internally consistent.
- No corrupt mapping from repository projection to response.
# Happy path and response mapping

## TC-HP-01 - Returns customer list for valid coordinates and radius
**Rationale:** Confirms endpoint/controller/service/repository integration introduced in PHN-2529.

**Preconditions**
- User has a valid auth token.
- Test data exists around the provided coordinates (inside and outside target radius).

**Steps**
1. Call `GET /sales-portal/customers/by-coordinates` with valid latitude/longitude, valid radius, and default pagination params.
2. Verify HTTP status and response shape.
3. Verify returned customers are geographically relevant to provided coordinates and radius.

**Expected result**
- Status is `200 OK`.
- Response contains expected paginated structure and non-empty `content` when data exists.
- Each returned item matches response model contract (required fields present and typed correctly).

## TC-HP-02 - Empty result set is handled correctly
**Rationale:** Validates stable behavior when no customers match coordinate range.

**Preconditions**
- User has a valid auth token.
- Coordinates and radius are valid but map to an empty area.

**Steps**
1. Call endpoint with valid coordinates known to have no matching customers.
2. Inspect response.

**Expected result**
- Status is `200 OK`.
- `content` is empty.
- Pagination metadata is valid (`totalElements=0`, `numberOfElements=0`, `totalPages` per API contract).

## TC-HP-03 - Projection-to-response mapping integrity
**Rationale:** Covers mapping failure risk between native query projection and API response DTO.

**Preconditions**
- User has a valid auth token.
- Dataset includes records with different classification values and optional fields.

**Steps**
1. Execute endpoint with coordinates that return mixed customer records.
2. Validate all mapped fields that originate from repository projection (IDs, names, classification, coordinate-related fields if present).
3. Compare a sample with expected database/query-derived values.

**Expected result**
- No null/shifted/misaligned mapped fields for mandatory attributes.
- Field semantics are preserved (no value swapped between columns).
- No serialization errors.

## TC-HP-04 - Radius filtering includes boundary and excludes outside records
**Rationale:** Covers high-risk geospatial filtering correctness from repository query logic.

**Preconditions**
- User has a valid auth token.
- Controlled dataset exists with at least:
  - one customer strictly inside radius,
  - one customer exactly at radius boundary,
  - one customer outside radius.

**Steps**
1. Call endpoint with coordinates and radius matching the dataset setup.
2. Inspect returned customer identifiers.
3. Compare returned identifiers with expected inside/boundary/outside sets.

**Expected result**
- Inside and boundary customers are included according to business rule.
- Outside-radius customers are excluded.
- No unrelated customers are returned.

-## TC-HP-05 - Response field contract for Sales Portal consumer
**Rationale:** Prevents consumer breakage from missing or renamed fields.

**Preconditions**
- User has a valid auth token.
- API contract/schema for Sales Portal consumer is available.

**Steps**
1. Request first page with valid coordinates and radius.
2. Validate top-level pagination fields and each customer item fields against contract.
3. Validate data types and nullability rules for required fields.

**Expected result**
- Response is contract-compliant for all required fields.
- No unexpected field type drift.
- Classification and coordinate-related fields are present when required by contract.

## TC-HP-06 - Classification correctness (acquisition vs recontract)
**Rationale:** Covers business classification semantics that drive Sales Portal UI behaviour.

**Preconditions**
- User has a valid auth token.
- Test data set contains customers explicitly modelled as acquisition and recontract cases according to business rules.
- Expected classification for a small set of coordinates is known from database/query inspection.

**Steps**
1. Call `GET /sales-portal/customers/by-coordinates` with coordinates and radius that return a mixed set of acquisition and recontract customers.
2. For a sampled subset of customers, compare response classification fields/flags with expected values from the underlying data (e.g., contract history, status).
3. Repeat the call to ensure classification values are stable across requests for the same dataset.

**Expected result**
- Each returned customer is classified correctly as acquisition vs recontract according to agreed business rules.
- No customer appears with different classification across repeated calls for the same dataset.
- No customer is left unclassified or mislabelled in a way that would mislead Sales Portal users.
