PHN-2160 - Detailed report for passed tests (29)

Jira: PHN-2160
Title: Put: Update existing POD
Date: 2026-03-19
Spec: `Cursor-Project/EnergoTS/tests/cursor/PHN-2160-put-update-existing-pod.spec.ts`

Run summary used for this report
- Passed: 29
- Failed: 0
- Skipped: 34
- Total: 63

Notes on detail fields
- Request/endpoint: exact method and route pattern used by the test.
- Request body: payload shape used by the test.
- Expected status: contract asserted in test.
- Response/assertion outcome: what the assertion validated for pass.

==================================================
PASSED DETAILS - PART 1 (COMP + API)
==================================================

[PHN-2160 TC-COMP-1]
- Request: `PUT /pod/{id}`
- Body: valid flat `PodUpdateRequest` from current POD + updated `name`
- Expected status: `2xx`
- Response/assertion outcome: update returned `2xx`; follow-up `GET /pod/{id}` reflected updated name.

[PHN-2160 TC-COMP-2]
- Request: `PUT /pod/pod/{id}` (documented mismatch route test)
- Body: valid flat `PodUpdateRequest`
- Expected status: `404`
- Response/assertion outcome: route mismatch confirmed by `404` and test passed.

[PHN-2160 TC-COMP-3]
- Requests:
  - `PUT /pod/{id}` (legacy/actual route)
  - `PUT /pod/pod/{id}` (documented mismatch route)
- Body: valid flat `PodUpdateRequest`
- Expected status: primary `2xx`, documented route `404`
- Response/assertion outcome: primary updated name successfully; documented route remained non-updating (`404`), state integrity preserved.

[PHN-2160 TC-COMP-4]
- Request: `PUT /pod/{id}`
- Body: `{ "podParameters": {} }` (invalid by design)
- Expected status: `400`
- Response/assertion outcome: validation error status accepted and no unintended name change after verification.

[PHN-2160 TC-COMP-5]
- Request: `PUT /pod/{id}`
- Body: `{ "podParameters": { "Name": null } }` (invalid by design)
- Expected status: `400`
- Response/assertion outcome: validation error status accepted and no unintended name change after verification.

[PHN-2160 TC-COMP-6]
- Request: `PUT /pod/{id}`
- Body: whitespace name via helper (`podParameters.Name = "   "`) (invalid by design)
- Expected status: `400`
- Response/assertion outcome: validation error status accepted and no unintended name change after verification.

[PHN-2160 TC-COMP-10]
- Request: `PUT /pod/{id}`
- Body: valid flat `PodUpdateRequest` with special-character name
- Expected status: `2xx`
- Response/assertion outcome: update accepted and persisted name matched the special-character input.

[PHN-2160 TC-COMP-11]
- Request: `PUT /pod/{id}`
- Body: name update + extra non-contract field block (`invoicing`)
- Expected status: `2xx` or `4xx` (defensive contract)
- Response/assertion outcome: accepted contract branch; invariants checked (no unsafe side effects on protected data).

[PHN-2160 TC-COMP-12]
- Request: `PUT /pod/{id}`
- Body: name update + extra non-contract field block (`invoiceData`)
- Expected status: `2xx` or `4xx` (defensive contract)
- Response/assertion outcome: accepted contract branch; protected-field behavior remained safe.

[PHN-2160 TC-COMP-13]
- Request: two consecutive `PUT /pod/{id}`
- Body: same valid flat `PodUpdateRequest` payload both times
- Expected status: both `2xx`
- Response/assertion outcome: repeat update remained stable and final read matched expected name.

[PHN-2160 TC-COMP-14]
- Request: `PUT /pod/999999999`
- Body: invalid-target update payload
- Expected status: `404` or `400`
- Response/assertion outcome: non-existent entity contract satisfied.

[PHN-2160 TC-COMP-22]
- Requests:
  - `PUT /pod/{id}` (valid update)
  - `GET /pod/{id}` (verify persistence)
  - `GET /pod/{identifier}/exists`
- Body: valid flat `PodUpdateRequest`
- Expected status:
  - update `2xx`
  - exists: either `200` with boolean semantics OR `400/404` invalid/non-existent semantics (accepted)
- Response/assertion outcome: update persisted and exists endpoint matched accepted contract branch.

[PHN-2160 TC-COMP-23]
- Request: `PUT /pod/{id}?sort=name&page=0&size=1`
- Body: valid flat `PodUpdateRequest`
- Expected status: `2xx` or `4xx`
- Response/assertion outcome: query-parameter noise did not break contract; state assertion path passed.

[PHN-2160 TC-API-1]
- Request: `PUT /pod/{id}`
- Body: valid flat `PodUpdateRequest`
- Expected status: `2xx`
- Response/assertion outcome: update success and persisted name validation passed.

[PHN-2160 TC-API-2]
- Request: two consecutive `PUT /pod/{id}`
- Body: same valid flat `PodUpdateRequest`
- Expected status: both `2xx`
- Response/assertion outcome: idempotent-style retry behavior passed under test assertions.

[PHN-2160 TC-API-3]
- Requests:
  - subset update `PUT /pod/{id}`
  - fuller update `PUT /pod/{id}`
- Body: valid flat `PodUpdateRequest` variants
- Expected status: both `2xx`
- Response/assertion outcome: update semantics remained consistent with preserved invariant checks.

[PHN-2160 TC-API-4]
- Request: `PUT /pod/999999998`
- Body: invalid-target update payload
- Expected status: `404` or `400`
- Response/assertion outcome: non-existent ID handling contract satisfied.

[PHN-2160 TC-API-5]
- Request: `PUT /pod/%%%invalid%%%`
- Body: invalid-id-format update payload
- Expected status: `400` or `404`
- Response/assertion outcome: invalid identifier contract satisfied.

[PHN-2160 TC-API-6]
- Request: `PUT /pod/{id}`
- Body: `{}`
- Expected status: `400`
- Response/assertion outcome: required-field validation contract satisfied.

==================================================
PASSED DETAILS - PART 2 (PERM + CONC + REG)
==================================================

[PHN-2160 TC-PERM-1]
- Request: `PUT /pod/{id}`
- Body: valid flat `PodUpdateRequest`
- Expected status: `2xx`
- Response/assertion outcome: authorized update flow passed with persisted name check.

[PHN-2160 TC-PERM-3]
- Request: `PUT /pod/{id}` with explicit invalid bearer token
- Body: valid flat `PodUpdateRequest`
- Expected status: `401` or `403`
- Response/assertion outcome: authorization rejection contract satisfied; no unauthorized data mutation observed.

[PHN-2160 TC-PERM-4]
- Request: `PUT /pod/{id}`
- Body: valid update payload plus read-only field mutation attempts
- Expected status: `2xx` or `4xx`
- Response/assertion outcome: contract branch accepted and protection invariants preserved.

[PHN-2160 TC-PERM-5]
- Request: `PUT /pod/{id}`
- Body: `{ "podParameters": { "Name": null } }`
- Expected status: `400`
- Response/assertion outcome: validation contract satisfied.

[PHN-2160 TC-PERM-8]
- Request: `PUT /pod/{id}`
- Body: `{ "podParameters": { "Name": null } }`
- Expected status: `400` + non-empty response body
- Response/assertion outcome: status and error-message hygiene assertions passed.

[PHN-2160 TC-CONC-3]
- Request: 10 repeated `PUT /pod/{id}`
- Body: valid flat `PodUpdateRequest` with changing name values
- Expected status: each response in (`2xx` or `409`)
- Response/assertion outcome: retry-storm tolerance contract satisfied.

[PHN-2160 TC-CONC-5]
- Requests:
  - concurrent-ish update and exists checks
  - `PUT /pod/{id}` + `GET /pod/{identifier}/exists`
- Body: valid flat `PodUpdateRequest`
- Expected status:
  - update `2xx`
  - exists: accepted branch (`200` boolean semantics OR `400/404` invalid/non-existent semantics)
- Response/assertion outcome: update and consistency checks passed under accepted exists contract.

[PHN-2160 TC-REG-1]
- Requests:
  - `PUT /pod/{id}`
  - `GET /pod/{id}`
- Body: valid flat `PodUpdateRequest`
- Expected status: update `2xx`, read `200`
- Response/assertion outcome: list/search-regression proxy check passed via persisted name verification.

[PHN-2160 TC-REG-3]
- Requests:
  - `GET /pod/{identifier}/exists` (before)
  - `PUT /pod/{id}` (update)
  - `GET /pod/{identifier}/exists` (after)
- Body: valid flat `PodUpdateRequest`
- Expected status:
  - update `2xx`
  - exists: accepted contract branch (`200` boolean or `400/404` invalid semantics)
- Response/assertion outcome: pre/post exists behavior stayed within accepted contract branch and test passed.

[PHN-2160 TC-REG-4]
- Request: `GET /pod/POD_DOES_NOT_EXIST_PHN_2160/exists`
- Expected status:
  - either `200` with `false` semantics, or
  - `400/404` with invalid/non-existent semantics
- Response/assertion outcome: endpoint satisfied one allowed branch and passed.

