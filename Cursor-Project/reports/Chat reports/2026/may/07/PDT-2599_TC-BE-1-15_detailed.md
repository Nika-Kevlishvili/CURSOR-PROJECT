# PDT-2599 Detailed Test Report (TC-BE-1 to TC-BE-15)

Run context
- Jira key: PDT-2599
- Date: 2026-05-07
- Branch: cursor
- Spec path: `Cursor-Project/EnergoTS/tests/cursor/PDT-2599-be-service-contract-version.spec.ts`
- Command: `npx playwright test tests/cursor/PDT-2599-be-service-contract-version.spec.ts --grep 'TC-BE-(1[0-5]|[1-9]):'`

Summary
- Total in scope: 15
- Passed: 15
- Failed: 0
- Skipped: 0
- Duration: ~1.8m (about 107.9s wall clock)

Test cases

TC-BE-1
- Status: PASSED
- Expected Result: Created service contract is retrievable and valid via API checks.
- Actual Result: Test passed with expected API behavior.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2854

TC-BE-2
- Status: PASSED
- Expected Result: Created contract appears in list/search by contract number.
- Actual Result: Test passed and created contract was found in list response.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2855

TC-BE-3
- Status: PASSED
- Expected Result: Negative GET/non-existing scenario returns expected rejection/validation behavior.
- Actual Result: Test passed with expected negative-path behavior.
- Portal data links:
  - Service contract: not available (no service contract portal preview URL was built in this run output).

TC-BE-4
- Status: PASSED
- Expected Result: Service contract detail/read path returns consistent data.
- Actual Result: Test passed with consistent detail response checks.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2856

TC-BE-5
- Status: PASSED
- Expected Result: Required query validation (missing parameter) returns expected 4xx/client error.
- Actual Result: Test passed with expected validation rejection.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2857

TC-BE-6
- Status: PASSED
- Expected Result: Third-tab/field validation path behaves as expected for invalid request shape.
- Actual Result: Test passed with expected negative validation behavior.
- Portal data links:
  - Service contract: not available (ResponseLinker did not emit serviceContract links for this flow).

TC-BE-7
- Status: PASSED
- Expected Result: Valid update/edit scenario keeps contract chain coherent.
- Actual Result: Test passed and contract chain checks were satisfied.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2858

TC-BE-8
- Status: PASSED
- Expected Result: Versioned update behavior matches expected status/date constraints.
- Actual Result: Test passed and version checks matched expectation.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2861

TC-BE-9
- Status: PASSED
- Expected Result: Contract version transition rules are enforced as expected.
- Actual Result: Test passed and transition assertions succeeded.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2862

TC-BE-10
- Status: PASSED
- Expected Result: Version ordering and update validations behave correctly.
- Actual Result: Test passed and ordering/validation assertions succeeded.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2863

TC-BE-11
- Status: PASSED
- Expected Result: Add non-first Draft with distinct start/open-end succeeds and chain remains valid.
- Actual Result: Test passed and chain validations succeeded.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2864

TC-BE-12
- Status: PASSED
- Expected Result: Draft duplicate start date is rejected with 4xx.
- Actual Result: Test passed and duplicate-date rejection matched expectation.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2865

TC-BE-13
- Status: PASSED
- Expected Result: Second Signed start earlier than first is rejected with 4xx.
- Actual Result: Test passed and validation rejection matched expectation.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2866

TC-BE-14
- Status: PASSED
- Expected Result: Middle Signed start edit keeps version chain coherent.
- Actual Result: Test passed and coherence checks succeeded.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2867

TC-BE-15
- Status: PASSED
- Expected Result: First Signed start date cannot be changed in-place.
- Actual Result: Test passed and in-place change was rejected as expected.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2868
