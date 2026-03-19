# Sales Portal Customer By Coordinates - Test Cases (PHN-2529)

**Jira:** PHN-2529  
**Endpoint:** `GET /sales-portal/customer/by-coordinates`  
**Type:** Flow test suite

This folder contains test cases for endpoint behavior and data contract stability for the sales-portal customer lookup by coordinates flow.

## Files in this flow

| File | Scope |
|------|-------|
| `Happy_path_and_response_mapping.md` | Successful requests, response contract mapping, and schema/field consistency. |
| `Authorization_token_validity_and_expiry.md` | Authentication and authorization behavior, token validity, expiry, and failure modes. |
| `Pagination_sorting_and_consistency.md` | Pagination correctness, sorting stability, duplicate/missing rows, and deterministic ordering. |
| `Classification_recontract_acquisition_correctness.md` | Classification accuracy (`recontract` vs `acquisition`) and boundary/regression scenarios. |
| `Input_validation_and_invalid_parameters.md` | Query parameter validation, malformed coordinates, range checks, and API error contract. |
| `Version_and_deployment_mismatch_checks.md` | API compatibility checks for response contract changes and mixed-version deployment scenarios. |

## Entry points covered

- `GET /sales-portal/customer/by-coordinates`
- `SalesPortalCustomerController`
- `SalesPortalCustomerService`
- `CustomerDetailsRepository` native selection query
- Request/response/projection models

## Primary risks addressed

- Pagination correctness
- Classification accuracy
- API compatibility
- Sorting stability
- Input validation
- Auth behavior
- Query performance

## Notes

- Keep assertions focused on externally observable behavior and stable response contract.
- For data-sensitive scenarios, use controlled fixtures that include close coordinate values, ties in sort keys, and mixed classification candidates.
- Use one canonical baseline request as a snapshot test anchor for backward compatibility.
# SalesPortal_CustomerByCoordinates - Flow test cases (PHN-2529)

This folder contains test cases for **PHN-2529**: get customer list by sales agent coordinates via `GET /sales-portal/customers/by-coordinates`.

## Scope

- **Entry points:** controller, service, request/response models, projection, repository (native paginated classification query).
- **Core risks:** auth regression, mapping failures, classification drift, pagination inconsistencies, version skew, performance degradation, and input validation edge cases.
- **Implementation evidence used in rationale:** endpoint/service/repository changes, successful builds and health checks, unauthorized curl evidence, commits `26cbc0495` and `788f4cdfb6`.

## Files in this flow

| File | What it covers |
|------|----------------|
| `Happy_path_and_response_mapping.md` | `TC-HP-01..05`: successful retrieval by coordinates+radius, schema/mapping integrity, boundary filtering, and response contract checks. |
| `Authorization_token_validity_and_expiry.md` | `TC-AUTH-01..06`: valid/invalid/missing/expired token behavior plus insufficient scope and issuer/audience checks. |
| `Pagination_sorting_and_consistency.md` | `TC-PAG-01..06`: metadata correctness, deterministic ordering, page boundaries, default paging, and overlap/gap prevention. |
| `Classification_recontract_acquisition_correctness.md` | `TC-CLS-01..06`: recontract/acquisition correctness, historical contracts, region boundaries, and partial-data resilience. |
| `Input_validation_and_invalid_parameters.md` | `TC-VAL-01..07`: coordinate/radius/pagination validation including missing params and boundary values. |
| `Version_and_deployment_mismatch_checks.md` | `TC-VER-01..06`: rollout/version skew, contract compatibility, feature toggle behavior, and performance guardrails. |

## High-risk regression focus (from cross-dependency analysis)

- Shared repository/native query logic drift affecting filtering, classification, or pagination.
- Classification errors for customers with complex historical contracts.
- Pagination default changes causing consumer-visible behavior shifts.
- Endpoint path mismatch between `/sales-portal/customer/by-coordinates` (story/spec) and `/sales-portal/customers/by-coordinates` (runtime artifact).
- Security-scope regressions exposing endpoint to unauthorized roles.
- Mixed-version deployment mismatch between Sales Portal consumer and Phoenix provider.

## References

- Jira: **PHN-2529** - Get customer list by sales agent coordinates.
- Confluence:
  - Business Story (AI): <https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/740327425/Get+customer+list+by+sales+agent+coordinates+-+AI>
  - Business Story: <https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/733577218/Get+customer+list+by+sales+agent+coordinates>
  - Technical Story: <https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/753598465/Technical+User+Story+GET+customer+list+by+sales+agent+coordinates>
- Evidence: commits `26cbc0495`, `788f4cdfb6`; successful build and health check; curl unauthorized proof for auth behavior.
