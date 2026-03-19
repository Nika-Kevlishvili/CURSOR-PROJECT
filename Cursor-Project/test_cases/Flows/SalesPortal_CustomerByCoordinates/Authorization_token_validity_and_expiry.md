# PHN-2529 - Authorization, Token Validity, and Expiry

**Scope:** Validate auth behavior for valid, missing, invalid, and expired tokens.

## Preconditions

- Endpoint protected by authentication middleware.
- At least one valid test token and one expired/invalid token available.
- Test user roles for authorized and unauthorized access paths.

## TC-1: Valid token allows access

**Steps:**
1. Send request with a valid bearer token.
2. Verify response status and payload structure.

**Expected result:**
- Request is authorized and processed normally.

## TC-2: Missing token is rejected

**Steps:**
1. Send request without `Authorization` header.
2. Capture status and error body.

**Expected result:**
- Request is rejected with authentication error.
- Error response follows API error contract.

## TC-3: Malformed token is rejected

**Steps:**
1. Send request with malformed bearer token format.
2. Capture response.

**Expected result:**
- Request is rejected.
- No partial processing of business query occurs.

## TC-4: Expired token is rejected with clear error

**Steps:**
1. Send request with an expired token.
2. Validate status and error payload.

**Expected result:**
- Request is rejected as expired.
- Error message/category indicates token expiry condition.

## TC-5: Token for user without required role/permissions is forbidden

**Steps:**
1. Send request using authenticated token lacking required access scope.
2. Validate status and response body.

**Expected result:**
- Request is rejected as forbidden.
- No customer data is returned.

## TC-6: Expiring token during repeated calls behaves consistently

**Steps:**
1. Execute repeated calls with a short-lived token near expiry.
2. Observe transition from success to auth failure.

**Expected result:**
- Calls before expiry succeed.
- Calls after expiry fail consistently without unstable behavior.
# Authorization token validity and expiry

## TC-AUTH-01 - Valid token with required scope allows access
**Rationale:** Confirms authorized access path for new sales-portal endpoint.

**Preconditions**
- Token provider is available.
- A token exists with required role/scope for Sales Portal customer listing.
- Valid coordinate/radius request parameters are prepared.

**Steps**
1. Send request with a valid bearer token and valid coordinates.
2. Inspect status and payload.

**Expected result**
- Status is `200 OK`.
- Response payload is returned normally.

## TC-AUTH-02 - Missing token is rejected
**Rationale:** Aligns with provided curl unauthorized evidence.

**Preconditions**
- Valid request parameters are prepared.

**Steps**
1. Send request without `Authorization` header.

**Expected result**
- Status is `401 Unauthorized` (or project-standard unauthorized code for this gateway).
- Error body follows standard auth error schema.

## TC-AUTH-03 - Invalid token is rejected
**Rationale:** Prevents auth regression when endpoint is added to security config.

**Preconditions**
- Valid request parameters are prepared.

**Steps**
1. Send request with malformed or forged bearer token.

**Expected result**
- Unauthorized response is returned.
- No internal stack trace leakage.

## TC-AUTH-04 - Expired token is rejected
**Rationale:** Covers token lifetime checks and expiry path.

**Preconditions**
- An expired token is available.
- Valid request parameters are prepared.

**Steps**
1. Send request with an expired but otherwise correctly structured token.

**Expected result**
- Unauthorized response is returned with expiry-related reason (if exposed by API standard).
- Endpoint does not return business data.

## TC-AUTH-05 - Token with insufficient scope/role is forbidden
**Rationale:** Covers high-risk security-scope regression from `what_could_break`.

**Preconditions**
- A valid token exists without the required scope/role for this endpoint.
- Valid request parameters are prepared.

**Steps**
1. Send request with token that is valid but lacks required authorization scope.
2. Inspect status and error payload.

**Expected result**
- Request is denied with `403 Forbidden` (or project-standard authorization denial code).
- Error payload is standardized and does not expose internals.
- No business data is returned.

## TC-AUTH-06 - Token from unexpected issuer/audience is rejected
**Rationale:** Ensures strict JWT validation and protects endpoint exposure.

**Preconditions**
- A token exists with wrong audience and/or issuer.
- Valid request parameters are prepared.

**Steps**
1. Send request using token with mismatched audience/issuer claims.
2. Inspect status and error body.

**Expected result**
- Unauthorized response is returned per security policy.
- Endpoint does not execute business flow for invalid claims.
