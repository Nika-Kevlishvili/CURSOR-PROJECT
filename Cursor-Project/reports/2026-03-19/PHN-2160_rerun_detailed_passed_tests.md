PHN-2160 - Playwright rerun (detailed passed tests)

Jira: PHN-2160
Title: Put: Update existing POD
Date: 2026-03-19
Spec: `Cursor-Project/EnergoTS/tests/cursor/PHN-2160-put-update-existing-pod.spec.ts`

Overall stats

- Passed: 14
- Failed: 15
- Skipped: 34
- Total: 63

Passed tests with technical details

1) TC-COMP-4 - Reject request when `podParameters.Name` is missing
- Request: `PUT /pod/{podId}`
- Payload: `{ "podParameters": {} }`
- Expected status: `400`
- Response/result: matched expected status contract, test passed.

2) TC-COMP-5 - Reject request when `podParameters.Name` is null
- Request: `PUT /pod/{podId}`
- Payload: `{ "podParameters": { "Name": null } }`
- Expected status: `400`
- Response/result: matched expected status contract, test passed.

3) TC-COMP-6 - Reject blank-only name
- Request: `PUT /pod/{podId}`
- Payload: `{ "podParameters": { "Name": "   " } }`
- Expected status: `400`
- Response/result: matched expected status contract, test passed.

4) TC-COMP-11 - Attempt to update extra fields beyond name
- Request: `PUT /pod/{podId}`
- Payload: name + extra field block (includes `invoicing`)
- Expected status: `2xx` or `4xx` (both are acceptable by test contract)
- Response/result: status matched contract and invariant checks passed (name behavior + protected fields checks).

5) TC-COMP-12 - Attempt to modify invoicing data through update endpoint
- Request: `PUT /pod/{podId}`
- Payload: name + `invoiceData` mutation attempt
- Expected status: `2xx` or `4xx` (both acceptable by test contract)
- Response/result: status matched contract and post-conditions passed (no invalid protected-field behavior).

6) TC-COMP-14 - Update non-existent POD ID
- Request: `PUT /pod/999999999`
- Payload: name-only update payload
- Expected status: `404` or `400`
- Response/result: matched expected not-found/invalid-id contract, test passed.

7) TC-COMP-23 - No sorting/filtering/pagination assumptions in update flow
- Request: `PUT /pod/{podId}?sort=name&page=0&size=1`
- Payload: name-only update payload
- Expected status: `2xx` or `4xx`
- Response/result: matched allowed status contract and state check assertions passed.

8) TC-API-4 - Update non-existent POD identifier
- Request: `PUT /pod/999999998`
- Payload: name-only update payload
- Expected status: `404` or `400`
- Response/result: matched expected not-found/invalid-id contract, test passed.

9) TC-API-5 - Invalid identifier format path validation
- Request: `PUT /pod/%%%invalid%%%`
- Payload: name-only update payload
- Expected status: `400` or `404`
- Response/result: matched expected invalid-identifier contract, test passed.

10) TC-API-6 - Payload validation missing required fields
- Request: `PUT /pod/{podId}`
- Payload: `{}`
- Expected status: `400`
- Response/result: matched expected validation-error contract, test passed.

11) TC-PERM-4 - Field-level protection for read-only fields
- Request: `PUT /pod/{podId}`
- Payload: includes protected fields (`id`, `createDate`) + name
- Expected status: `2xx` or `4xx` (by current test contract)
- Response/result: matched contract and protected-field assertions passed.

12) TC-PERM-5 - Validation required field missing or null
- Request: `PUT /pod/{podId}`
- Payload: `{ "podParameters": { "Name": null } }`
- Expected status: `400`
- Response/result: matched expected validation contract, test passed.

13) TC-PERM-8 - Error message hygiene
- Request: `PUT /pod/{podId}`
- Payload: `{ "podParameters": { "Name": null } }`
- Expected status: `400` and non-empty error body
- Response/result: response satisfied both status and body checks, test passed.

14) TC-REG-4 - `/exists` for non-existent identifier
- Request: `GET /pod/POD_DOES_NOT_EXIST_PHN_2160/exists`
- Expected status contract:
  - either `200` with `false` body semantics, or
  - `400/404` with invalid/non-existent identifier semantics
- Response/result: matched one allowed branch of contract and passed content checks.

Notes

- Passed test list is extracted from the latest JSON reporter output (`playwright-phn2160-detailed.json`).
- Remaining failed tests are primarily contract mismatches on success-path update assertions and route behavior.

