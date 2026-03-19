# PHN-2529 - Version and Deployment Mismatch Checks

**Scope:** Validate API compatibility and behavior during mixed-version deployments.

## Preconditions

- Test environment supports staged or canary deployment simulation.
- Baseline response contract snapshot exists for client compatibility checks.

## TC-1: Backward-compatible response contract across versions

**Steps:**
1. Execute baseline request against previous stable version.
2. Execute same request against new version.
3. Compare required fields, field types, and semantic meaning.

**Expected result:**
- Required contract remains backward compatible.
- Any additive fields do not break existing clients.

## TC-2: Removed/renamed fields are detected as breaking change

**Steps:**
1. Validate response against consumer contract tests.
2. Check for missing required fields or type changes.

**Expected result:**
- Breaking changes are detected by contract checks.
- Release is blocked or flagged if compatibility is violated.

## TC-3: Mixed-version pods return functionally consistent results

**Steps:**
1. Route repeated requests across instances with different app versions.
2. Compare status code, pagination metadata, and classification semantics.

**Expected result:**
- No major behavioral divergence across versions.
- Results are equivalent within accepted tolerance.

## TC-4: Error payload compatibility across versions

**Steps:**
1. Trigger validation and auth errors against both versions.
2. Compare error code, structure, and parsing-critical fields.

**Expected result:**
- Error contract remains stable for existing consumers.

## TC-5: Sorting and pagination behavior unchanged after deployment

**Steps:**
1. Run deterministic multi-page query before deployment.
2. Re-run same query after deployment and compare item order/partition.

**Expected result:**
- No unexpected ordering/paging behavior drift.

## TC-6: Query performance regression guard after rollout

**Steps:**
1. Measure p50/p95 response time for representative coordinate requests before rollout.
2. Measure same metrics after rollout.

**Expected result:**
- Performance remains within approved regression threshold.
- Any degradation beyond threshold is flagged.
# Version and deployment mismatch checks

## TC-VER-01 - Endpoint availability after deployment
**Rationale:** Uses successful build and health-check evidence to verify runtime availability.

**Preconditions**
- Environment is deployed with PHN-2529 artifacts.
- Valid auth token and valid request params are available.
- Agreement exists in documentation which path is canonical: `GET /sales-portal/customer/by-coordinates` vs runtime artifact `/sales-portal/customers/by-coordinates`.

**Steps**
1. Deploy build containing commits `26cbc0495` and `788f4cdfb6`.
2. Verify service health endpoint is UP.
3. Call `GET /sales-portal/customer/by-coordinates` with valid token and params.
4. Call `GET /sales-portal/customers/by-coordinates` with the same token and params.

**Expected result**
- Health check passes.
- At least one of the documented endpoints (`/customer` or `/customers`) responds successfully (not 404/501).
- Canonical path from documentation matches the path that returns `200 OK`, or both paths are intentionally supported and documented.
- No ambiguous behavior (e.g., both returning 404 while story claims endpoint is deployed).

## TC-VER-02 - Backward compatibility for consumers during rollout
**Rationale:** Covers version skew risk during partial deployment.

**Preconditions**
- Rolling deployment environment where component versions can differ temporarily.
- Consumer expects documented response schema.

**Steps**
1. Test in environment where gateway/service versions may briefly differ.
2. Call endpoint repeatedly during rollout window.

**Expected result**
- No unexpected contract break (field removals/type changes) for clients expecting agreed response schema.
- Failures, if any, are controlled and observable (no silent data corruption).

## TC-VER-03 - Repository/query compatibility in mixed artifacts
**Rationale:** Verifies service-model-repository compatibility under deployment mismatch.

**Preconditions**
- Environment can emulate mixed artifact state (older/newer compatible components).
- Valid auth token is available.

**Steps**
1. Run smoke tests that hit controller -> service -> repository native query chain after deployment.
2. Validate response mapping for at least one page of data.

**Expected result**
- No runtime mapping exceptions due to mismatched model/projection versions.
- Response remains contract-compliant.

## TC-VER-04 - Performance regression guardrail
**Rationale:** Mitigates perf degradation risk from native paginated classification query.

**Preconditions**
- Baseline performance threshold is defined for target environment.
- Representative datasets exist for low/medium/high cardinality.

**Steps**
1. Execute repeated calls for representative coordinate ranges (small, medium, high cardinality).
2. Measure response times and error rate.

**Expected result**
- Response latency remains within agreed non-functional threshold for target environment.
- No timeout spike or elevated 5xx under expected load.

## TC-VER-05 - Feature toggle off behavior (if toggle exists)
**Rationale:** Ensures controlled rollout and safe fallback path.

**Preconditions**
- Feature toggle for PHN-2529 is available (if implemented).
- Ability to switch toggle ON/OFF in non-production environment.

**Steps**
1. Set feature toggle OFF and call endpoint.
2. Set feature toggle ON and call endpoint with same parameters.
3. Compare behavior with expected rollout design.

**Expected result**
- Toggle OFF behavior matches documented fallback (e.g., endpoint disabled or controlled error).
- Toggle ON behavior returns normal successful response.
- No partial/undefined behavior during toggle transitions.

## TC-VER-06 - Sales Portal and Phoenix contract mismatch detection
**Rationale:** Covers high-risk consumer/provider schema mismatch during phased releases.

**Preconditions**
- Test setup allows running Sales Portal consumer against differing Phoenix versions.
- Monitoring/logging is enabled.

**Steps**
1. Execute request from consumer build A to provider build B (intentional skew).
2. Validate consumer parsing behavior and user-visible outcome.
3. Inspect logs/metrics for contract mismatch signals.

**Expected result**
- Contract mismatch, if present, is explicit and observable.
- No silent truncation/corruption of classification or pagination fields.
- Recovery path is documented and testable.
