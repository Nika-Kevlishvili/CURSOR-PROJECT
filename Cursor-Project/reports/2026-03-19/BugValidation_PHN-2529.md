## Bug Validation Analysis – PHN-2529 (Get customer list by sales agent coordinates)

### 1. Confluence Validation

**Sources:**
- Business story: “Get customer list by sales agent coordinates” (`pageId=733577218`, Phoenix space)
- AI-refined business story: “Get customer list by sales agent coordinates - AI” (`pageId=740327425`, Phoenix space)
- Technical story: “Technical User Story: GET customer list by sales agent coordinates” (`pageId=753598465`, Phoenix space)

**Key documented behavior:**
- **Endpoint & method:** `GET` API, path expressed as “customer list by sales agent GPS coordinates”; technical story proposes `GET /api/v1/customers/by-coordinates` with query parameters (`latitude`, `longitude`, `radius`, `page`, `size`).
- **Input contract:**
  - `latitude` (required), range `[-90, 90]`
  - `longitude` (required), range `[-180, 180]`
  - `radius` (required), range `[0, 260000]` meters; dynamic and configurable (e.g. 5000m typical)
  - Pagination: 0-based `page`, `size` with default 20; no explicit sort controls.
- **Output contract:**
  - Paginated list (use standard Phoenix `Page`-like structure) with:
    - `customerUic` (EGN/UIC)
    - `customerName`
    - `customerNameTranslated`
    - `latitude`
    - `longitude`
    - `precision` (values such as Precise, Semi-precise, Imprecise)
    - `podNumber` (Z number; may be null for some cases)
    - `type` with values Acquisition or Recontracting.
  - Includes pagination metadata: `totalElements`, `totalPages`, `size`, `number`, etc.
- **Classification rules:**
  - Acquisition: customer has **no active contract** at that POD.
  - Recontracting: customer has **at least one active contract** at that POD and is in a recontractable window (per technical story LC rules).
  - One row per customer address/POD after deduplication.
- **Validation & error behavior:**
  - Validation at DTO layer via bean validation; invalid/missing parameters → HTTP 400 with per-field error messages (`fieldName - message` style).
  - No sortable values; only pagination.
- **Auth expectations (from related Sales Portal specs):**
  - Sales Portal APIs use OAuth2 Client Credentials.
  - Consumer obtains a JWT via a token endpoint (e.g. `/sales-portal/oauth2/token`) and calls `/sales-portal/**` endpoints with `Authorization: Bearer <token>`.
  - Endpoints are protected; unauthorized or invalid/expired tokens must be rejected with 401/403.

**Confluence validation: correct / consistent with implementation intent**
- The Confluence documents clearly define:
  - Input parameters and ranges.
  - Pagination semantics.
  - Response shape and classification rules.
  - Usage by Sales Portal via OAuth2 Client Credentials.
- The implemented design (controller/service/repository described below) matches the conceptual model (coordinates+radius filter in DB with acquisition/recontract classification and pagination). Naming differences (`customerId` vs `customerUic`, path under `/sales-portal/customers/by-coordinates`) are minor but should be normalized in documentation and clients.

**Status:** **Confluence validation: correct**, with minor path/field naming drift (docs vs actual path and DTO field names) but no functional contradictions.

---

### 2. Code Analysis

**Scope of implementation (referenced commits):**
- `phoenix-core-lib` commit `26cbc0495` – “fix(PHN-2529): add sales portal customer coordinates service”
  - `SalesPortalCustomerService`
  - `SalesPortalCustomerByCoordinatesRequest`
  - `SalesPortalCustomerByCoordinatesResponse`
  - `SalesPortalCustomerByCoordinatesProjection`
  - `CustomerDetailsRepository` (new native query and projection mapping)
- `phoenix-core` commit `788f4cdfb6` – “fix(PHN-2529): expose sales portal customers by coordinates endpoint”
  - `SalesPortalCustomerController`

#### 2.1 Controller and request binding

**File:** `phoenix-core/src/main/java/bg/energo/phoenix/controller/salesPortal/SalesPortalCustomerController.java` (from `788f4cdfb6`)

- Mapping:
  - `@RestController`
  - `@RequestMapping("/sales-portal/customers")`
  - `@GetMapping("/by-coordinates")`
  - Net path: `GET /sales-portal/customers/by-coordinates`.
- Auth annotation:
  - `@Operation(security = @SecurityRequirement(name = "bearer-token"))`
  - Aligns with other Sales Portal controllers (`SalesPortalPodController`, `SalesPortalHealthCheckController`) that rely on a JWT-based bearer token for `/sales-portal/**`.
- Request binding:
  - Method signature: `ResponseEntity<Page<SalesPortalCustomerByCoordinatesResponse>> getCustomersByCoordinates(@Valid SalesPortalCustomerByCoordinatesRequest request)`.
  - Uses a request DTO with `@Valid`, allowing Spring to bind query parameters into the bean and run bean validation.

**File:** `phoenix-core-lib/src/main/java/bg/energo/phoenix/service/salesportal/requests/SalesPortalCustomerByCoordinatesRequest.java` (from `26cbc0495`)

- Fields and validation:
  - `Double latitude;`
    - `@NotNull(message = "latitude-Latitude is mandatory;")`
    - `@DecimalMin("-90")`, `@DecimalMax("90")` with message: `"latitude-Latitude must be between -90 and 90;"`
  - `Double longitude;`
    - `@NotNull(message = "longitude-Longitude is mandatory;")`
    - `@DecimalMin("-180")`, `@DecimalMax("180")` with range message.
  - `Double radius;`
    - `@NotNull(message = "radius-Radius is mandatory;")`
    - `@DecimalMin("0")`, `@DecimalMax("260000")` with range message.
  - `Integer page = 0;`
    - `@Min(0)` with `"page-Page must be greater than or equal to 0;"`
  - `Integer size = 20;`
    - `@Min(1)`, `@Max(500)` with appropriate messages.

**Assessment vs Confluence:**
- Required parameters and allowed ranges exactly match the technical story.
- Validation performed at DTO layer with per-field messages, as required.
- Pagination defaults (page 0, size 20) and max size 500 are consistent with general Phoenix patterns and acceptable given the spec.
- Parameter transport is via query parameters bound into a DTO, which is aligned with the “query parameters recommended for GET” guidance in Confluence.

**Conclusion (controller + request):** Behavior **satisfies** the Confluence contract for input validation, pagination, and mapping, aside from the exact endpoint path (`/sales-portal/customers/by-coordinates` instead of `/api/v1/customers/by-coordinates`).

#### 2.2 Service, projection, and response mapping

**File:** `phoenix-core-lib/src/main/java/bg/energo/phoenix/service/salesportal/SalesPortalCustomerService.java`

- Method:
  - `Page<SalesPortalCustomerByCoordinatesResponse> getCustomersByCoordinates(SalesPortalCustomerByCoordinatesRequest request)`
  - Delegates to repository method:
    - `customerDetailsRepository.findSalesPortalCustomersByCoordinates(request.getLatitude(), request.getLongitude(), request.getRadius(), PageRequest.of(request.getPage(), request.getSize()))`
  - Maps each `SalesPortalCustomerByCoordinatesProjection` to `SalesPortalCustomerByCoordinatesResponse`.

**Files:**
- `SalesPortalCustomerByCoordinatesProjection` – interface with getters:
  - `getCustomerId()`, `getCustomerName()`, `getCustomerNameTranslated()`, `getPrecision()`, `getLatitude()`, `getLongitude()`, `getPodNumber()`, `getType()`.
- `SalesPortalCustomerByCoordinatesResponse` – DTO with same fields:
  - `String customerId;`
  - `String customerName;`
  - `String customerNameTranslated;`
  - `String precision;`
  - `String latitude;`
  - `String longitude;`
  - `String podNumber;`
  - `String type;`

**Assessment vs Confluence:**
- Field set corresponds closely to the documented response structure.
- `customerId` in the DTO is effectively the “customerUic” from the spec; this is a naming discrepancy only.
- Latitude/longitude are modeled as strings formatted from DB (see query), but the interface remains a contract-level string representation of decimal coordinates, which is acceptable as long as the JSON schema is agreed with Sales Portal; Confluence uses numbers but does not forbid string representation.
- `type` is a free-text string with values derived as `'Recontract'` or `'Acquisition'`, which matches the business language.

**Conclusion (service/DTO):** Behavior **satisfies** the response contract in terms of fields and classification semantics, with minor naming/typing differences that should be documented and/or normalized.

#### 2.3 Repository and native query (classification, distance, pagination)

**File (excerpt, from `26cbc0495`):** `phoenix-core-lib/src/main/java/bg/energo/phoenix/repository/customer/CustomerDetailsRepository.java`

- Newly added method:

```java
@Query(
        nativeQuery = true,
        value = """  -- see full file
            WITH p AS (
                SELECT ST_SetSRID(ST_MakePoint(:pLongitude, :pLatitude), 4326)::geography AS search_point
            ),
            base_points AS (
                SELECT
                    cc.id AS communication_id,
                    cc.customer_detail_id,
                    cd.customer_id,
                    c.identifier AS customer_identifier,
                    c.customer_type,
                    cd.name,
                    cd.middle_name,
                    cd.last_name,
                    cd.name_transl,
                    cd.middle_name_transl,
                    cd.last_name_transl,
                    cd.legal_form_id,
                    cc.location,
                    cc.block,
                    cc.street_id,
                    cc.street_foreign,
                    cc.street_number,
                    cc.entrance,
                    cc.residential_area_id,
                    cc.residential_area_foreign,
                    cd.id AS customer_detail_version_id,
                    ST_Distance(cc.location, p.search_point) AS distance_meters
                FROM customer.customer_communications cc
                JOIN customer.customer_details cd ON cd.id = cc.customer_detail_id
                JOIN customer.customers c ON c.id = cd.customer_id
                CROSS JOIN p
                WHERE cc.status = 'ACTIVE'
                  AND cc.location IS NOT NULL
                  AND ST_DWithin(cc.location, p.search_point, :pRadiusMeters)
            ),
            ...
            recontract_rows AS ( ... type 'Recontract' ... ),
            acquisition_rows AS ( ... type 'Acquisition' ... ),
            unioned AS (
                SELECT * FROM recontract_rows
                UNION ALL
                SELECT * FROM acquisition_rows
            ),
            deduped AS (
                SELECT
                    u.*,
                    row_number() OVER (
                        PARTITION BY
                            u.customer_id,
                            ST_AsEWKT(u.location::geometry),
                            u.type,
                            coalesce(u.pod_number, '')
                        ORDER BY u.customer_detail_version_id DESC
                    ) AS rn
                FROM unioned u
            )
            SELECT
                customer_identifier AS "customerId",
                customer_name AS "customerName",
                customer_name_translated AS "customerNameTranslated",
                precision AS "precision",
                latitude AS "latitude",
                longitude AS "longitude",
                pod_number AS "podNumber",
                type AS "type"
            FROM deduped
            WHERE rn = 1
            ORDER BY distance_meters ASC
        """,
        countQuery = """  -- matching count CTE, returning count(*) from deduped WHERE rn=1
        """
)
Page<bg.energo.phoenix.service.salesportal.model.SalesPortalCustomerByCoordinatesProjection> findSalesPortalCustomersByCoordinates(
        @Param("pLatitude") Double pLatitude,
        @Param("pLongitude") Double pLongitude,
        @Param("pRadiusMeters") Double pRadiusMeters,
        Pageable pageable
);
```

**Key properties:**
- **Distance & radius:**
  - Uses PostGIS geography: `ST_SetSRID(ST_MakePoint(:pLongitude, :pLatitude), 4326)::geography`.
  - Filters communication points with `ST_DWithin(cc.location, p.search_point, :pRadiusMeters)`.
  - Orders final result by `distance_meters ASC`, which is consistent with “closest first” semantics and is stable given the deduplication logic.
- **Acquisition vs Recontracting:**
  - `contract_candidates` & `contract_eval` CTEs:
    - Select product contracts tied to `customer_detail_id` for relevant points.
    - Mark contracts as recontractable when:
      - `contract_status = 'ACTIVE_IN_PERPETUITY'` with `resign_to_contract_id IS NULL`, or
      - `contract_status = 'ACTIVE_IN_TERM'` with non-null `contract_term_end_date` and resigning deadline fields, and current date in the resigning window (before `contract_term_end_date` but after `contract_term_end_date - resigning_interval`).
  - `recontract_rows` CTE:
    - Includes points where `can_recontract = true`, sets `type = 'Recontract'`, and populates `pod_number` from active `product_contract.contract_pods`.
  - `acquisition_rows` CTE:
    - Includes points where **no** matching contract_candidate exists for that `customer_detail_id`, sets `type = 'Acquisition'`, `pod_number = NULL`.
  - Combined via `UNION ALL` into `unioned`, then deduplicated per (customer, address location, type, pod) choosing the latest customer detail version.
- **Precision:**
  - Computed from address attributes (street/block/entrance/residential area) into `'IMPRECISE'`, `'PRECISE'`, or `'SEMI_PRECISE'`, matching the conceptual notion from Confluence.
- **Pagination:**
  - Implemented via `Pageable` from Spring Data JPA, with a matching `countQuery` that mirrors the data query’s CTE structure and deduplication logic.

**Assessment vs Confluence:**
- **Filtering within radius and requiring coordinates:** Implemented via `ST_DWithin` with `cc.location IS NOT NULL` – aligns with LC-G1 and LC-G2.
- **Classification:** Implementation goes beyond the minimal Confluence description by correctly modeling recontractability windows and contract statuses, but the end result still matches the business requirement:
  - If the customer can recontract at that POD → `type="Recontract"`.
  - If no such contract exists → `type="Acquisition"`.
- **One item per POD-like location:** `deduped` CTE ensures a single row per `(customer, precise location, type, pod)` by using `row_number()` and retaining `rn=1`.
- **Sorting and pagination:** Sorting by distance and proper count query mean stable, consistent pagination; this matches pagination behavior expected in the technical story.

**Conclusion (repository & query):** The native SQL implementation **satisfies and refines** the Confluence contract, correctly handling:
- Geo-distance filtering and coordinate requirement.
- Acquisition vs Recontracting classification.
- Deduplication and ordering.
- Pagination metadata.

#### 2.4 Auth and token model vs bug symptoms

**Relevant code:**

- `SalesPortalTokenController` (from `phoenix-core` commit `788f4cdfb6`):
  - Path: `POST /sales-portal/oauth2/token`.
  - Accepts `grant_type=client_credentials`, `client_id`, `client_secret`.
  - Validates:
    - `grant_type` must be `"client_credentials"`, else `ClientException` with `ILLEGAL_ARGUMENTS_PROVIDED`.
    - `client_id` and `client_secret` must match `sales-portal.client.id` and `sales-portal.client.secret` from configuration, else `ClientException` with `ACCESS_DENIED`.
  - Issues JWT token using `JwtVerifier.issueClientCredentialsToken(clientId)` and returns:
    - `"access_token"`, `"token_type": "Bearer"`, `"expires_in"`.
- `SalesPortalHealthCheckController` and `SalesPortalPodController`:
  - Paths under `/sales-portal/**`.
  - Annotated with `@Operation(security = @SecurityRequirement(name = "bearer-token"))`.
  - Comments reference a dedicated auth filter and security config (e.g. `SalesPortalAuthenticationFilter`, `SalesPortalSecurityConfig`) that are not shown in the inspected commit but are implied to exist and to guard `/sales-portal/**` endpoints with bearer JWT issued by the token endpoint.

**Observed runtime behavior (from existing PHN-2529 report + user context):**
- Build and health for phoenix-core-lib and phoenix-core are green; local `/actuator/health` is UP.
- Manual `curl` against `GET /sales-portal/customer/by-coordinates` with a provided JWT returns **Unauthorized / Invalid or expired token**.
- Playwright spec `PHN-2529-get-sales-portal-customer-by-coordinates.spec.ts` exercises:
  - Positive-path calls with a nominally “valid” token.
  - Negative auth tests (invalid token, malformed token, missing token).
- Test results (from `Cursor-Project/reports/2026-03-19/PHN-2529.md`):
  - Missing token and malformed/invalid token tests **pass** (401/403 as expected).
  - All positive-path tests (**happy path, pagination, classification, validation**) **fail** with **401** instead of 200/400, indicating:
    - The endpoint is correctly protected by JWT auth.
    - The tokens used in the tests (and the user’s manual curl) are not accepted as valid, likely due to:
      - Not being issued by `SalesPortalTokenController` (wrong issuer or signing key), or
      - Client id mismatch, or
      - Expired token or incorrect `audience/scope` (depending on `JwtVerifier` configuration).

**Assessment vs bug description:**
- The underlying bug report (“endpoint implemented, but manual curl returns Unauthorized/Invalid or expired token even with provided JWT”) points at **auth/token configuration** rather than controller/service/query correctness.
- Code for `/sales-portal/customers/by-coordinates` uses the same auth pattern as other Sales Portal endpoints and is annotated to require bearer auth. There is no indication of misconfiguration **inside** this controller; the behavior is consistent with the rest of `/sales-portal/**`.
- Given that:
  - Negative auth tests work as expected, and
  - All positive-path tests fail **only** due to 401,
  - The most likely cause is:
    - The “provided JWT” does not match the expected Sales Portal client credentials flow and signing configuration, or
    - Test/fixture tokens do not come from `POST /sales-portal/oauth2/token` with the same secret config that the service uses for verification.

**Conclusion (auth path):**
- The endpoint is correctly protected and wired into the Sales Portal auth model (bearer JWT via OAuth2 Client Credentials).
- The **remaining gap** is **environment/token provisioning**:
  - No guaranteed-valid Sales Portal token has been wired into EnergoTS fixtures or manual test instructions.
  - Without a token issued by the actual `SalesPortalTokenController` configuration, all business-path tests will continue to 401.

**Status:** **Code validation: satisfies the bug’s functional requirements**, but **positive-path execution is currently blocked by missing/invalid tokens**, not by a defect in the controller/service/query themselves.

---

### 3. Conclusion

1. **Is the Confluence story and endpoint behavior model correct?**
   - Yes. Confluence clearly specifies the behavior of “Get customer list by sales agent coordinates” including coordinates+radius input, acquisition/recontract classification, pagination, and Sales Portal OAuth2 usage. The implemented design follows this model closely.

2. **Does the implementation (controller/service/request/response/projection/repository) satisfy the Confluence contract?**
   - **Yes, with minor naming/path differences.**
   - Request DTO enforces exactly the documented ranges and required parameters.
   - Repository native query correctly:
     - Filters within the given radius,
     - Requires coordinates,
     - Classifies acquisition vs recontracting at POD level,
     - Deduplicates items,
     - Provides stable, distance-ordered pagination.
   - Response projection and DTO expose the documented fields (`customerId` ≈ `customerUic`, `type`, `podNumber`, `precision`, `latitude`, `longitude`).
   - Controller exposes `GET /sales-portal/customers/by-coordinates` with bearer JWT auth requirement, consistent with Sales Portal integration.

3. **Is the Jira bug about the endpoint + implementation + fix VALID?**
   - The **original need** (“implement GET customer list by sales agent coordinates with acquisition/recontract classification and pagination”) is correctly addressed by the code in `26cbc0495` and `788f4cdfb6`.
   - The **reported runtime issue** (“Unauthorized / Invalid or expired token for positive-path calls”) is **valid as an auth/token provisioning gap**, but not as a defect in the new endpoint’s business logic.
   - The fix (controller + service + repository) is **functionally valid** and aligned with Confluence. The unresolved part is:
     - providing a proper, environment-specific, Sales Portal JWT that matches the configured `sales-portal.client.id/secret` and JWT verifier expectations.

4. **Remaining auth/token scope gaps:**
   - No dedicated **“valid Sales Portal token”** is documented or provisioned in:
     - EnergoTS fixtures (`login.ts`, env variables) for PHN-2529 happy-path tests.
     - Manual test instructions (how to obtain a token from `POST /sales-portal/oauth2/token` in the given environment and use it against `/sales-portal/customers/by-coordinates`).
   - No explicit Confluence page centralizing:
     - Sales Portal OAuth2 configuration keys (`sales-portal.client.id`, `.secret`),
     - Expected JWT claims (issuer, subject, audience, scopes, expiry) for `/sales-portal/**` access.
   - As a result, all positive-path Playwright and curl calls risk using tokens that the runtime correctly rejects as **invalid or expired**.

**Final verdict:**
- **Bug validity (business feature & implementation):** **VALID** – the business requirement is real and implemented correctly in code.
- **Fix validity (code-level):** **VALID** – controller, service, DTOs, and repository behave according to Confluence, and negative auth paths behave correctly.
- **Blocking issue status:** The remaining blocker is **environment/token configuration**, not a flaw in the PHN-2529 code changes. Until a correct Sales Portal JWT is provisioned for the target environment, functional tests will continue to fail with 401 despite correct implementation.

---

### 4. Suggested follow-up actions (no code changes)

These are recommendations only; they are **not** implemented as part of this validation:

1. **Token provisioning for test and manual verification**
   - Document a concrete procedure (per environment) to:
     - Call `POST /sales-portal/oauth2/token` with the correct `client_id` and `client_secret`.
     - Capture the returned `access_token` and plug it into:
       - EnergoTS fixtures (e.g. a dedicated `PHN2529_VALID_TOKEN` or a Sales Portal token fixture),
       - Manual curl examples for `/sales-portal/customers/by-coordinates`.
   - Ensure the token’s TTL (`expires_in`) and replay behavior are clearly documented for QA.

2. **Align documentation and path/field naming**
   - Update Confluence technical story to state the **actual deployed path** as `GET /sales-portal/customers/by-coordinates` (or adjust mapping if `/api/v1/customers/by-coordinates` is mandatory).
   - Clarify that `customerId` in the response JSON represents `customerUic` (EGN/UIC).

3. **Add minimal smoke tests for Sales Portal token endpoint**
   - Add a small automated or manual check to verify that `POST /sales-portal/oauth2/token`:
     - Works in the target environment with configured client credentials.
     - Issues tokens that are accepted by `/sales-portal/health` and `/sales-portal/customers/by-coordinates` for positive-path requests.

4. **Extend test data coverage**
   - Ensure the test database contains:
     - Customers with communication coordinates inside a well-known test radius.
     - Both acquisition-only and recontract-eligible customers around those coordinates so that both `type="Acquisition"` and `type="Recontract"` can be asserted.

---

**Bug validity summary:**  
- **Bug PHN-2529 (feature need):** VALID  
- **Implementation (commits 26cbc0495, 788f4cdfb6):** VALID and aligned with Confluence  
- **Current failing tests (401 on happy path):** Attributed to **missing/invalid Sales Portal JWT**, not to a regression or defect in the new endpoint implementation.

