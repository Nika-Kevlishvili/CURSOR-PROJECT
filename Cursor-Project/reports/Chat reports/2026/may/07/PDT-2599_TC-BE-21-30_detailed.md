# PDT-2599 Detailed Test Report (TC-BE-21 to TC-BE-30)

Run context
- Jira key: PDT-2599
- Date: 2026-05-07
- Branch: cursor
- Spec path: `Cursor-Project/EnergoTS/tests/cursor/PDT-2599-be-service-contract-version.spec.ts`
- Command: `npx playwright test "tests/cursor/PDT-2599-be-service-contract-version.spec.ts" --grep "TC-BE-(2[1-9]|30)"`
- Report path: `Cursor-Project/reports/Chat reports/2026/may/07/PDT-2599_TC-BE-21-30_detailed.md`
- Evidence path: `Cursor-Project/EnergoTS/playwright-report.json`

Artifact paths
- Spec: `Cursor-Project/EnergoTS/tests/cursor/PDT-2599-be-service-contract-version.spec.ts`
- JSON evidence: `Cursor-Project/EnergoTS/playwright-report.json`
- This report: `Cursor-Project/reports/Chat reports/2026/may/07/PDT-2599_TC-BE-21-30_detailed.md`

Summary
- Total in scope: 10
- Passed: 3
- Failed: 3
- Skipped (did not run): 4
- Exit code: 1
- Duration: ~127574 ms (Playwright), ~129375 ms wall clock

Test cases

TC-BE-21
- Title: [PDT-2599] TC-BE-21: PUT with unknown versionId -> 4xx, GET unchanged
- Status: PASSED
- Expected Result: PUT with non-existing versionId is rejected with 4xx and versions list remains unchanged.
- Actual Result: Test passed; API behavior matched expected rejection and unchanged state checks.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2845
  - Billing run: not created in this test scope.

TC-BE-22
- Title: [PDT-2599] TC-BE-22: new Signed start strictly before first version start -> 4xx
- Status: PASSED
- Expected Result: Creating a Signed version with start date earlier than first existing version is rejected with 4xx.
- Actual Result: Test passed; rejection behavior matched expected validation rule.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2848
  - Billing run: not created in this test scope.

TC-BE-23
- Title: [PDT-2599] TC-BE-23: invalid contractVersionStatus in PUT body -> client error
- Status: PASSED
- Expected Result: Invalid contractVersionStatus in PUT payload returns client error (4xx).
- Actual Result: Test passed; API returned expected client-side validation error.
- Portal data links:
  - Service contract: http://10.236.20.11:8080/service-contracts/preview?id=2849
  - Billing run: not created in this test scope.

TC-BE-24
- Title: [PDT-2599] TC-BE-24: PER_PIECE - first Signed window - invoices issued (distinct drivers v1/v2)
- Status: FAILED
- Expected Result: Invoice issuance in first Signed window should resolve and apply expected formula/driver values for v1/v2.
- Actual Result: Assertion failed (`expect(...).not.toBeNull()`); received `null` for formula-related value after billing run/drafts. Attachment indicated unresolved contract formula / missing formula variable linkage.
- Failure excerpt: expected value not null, received null.
- Portal data links:
  - Service contract: not captured in this billing flow output.
  - Billing run: not captured; test failed before billing portal attachment/logging step.

TC-BE-25
- Title: [PDT-2599] TC-BE-25: PER_PIECE - second Signed window - billingDate aligns second driver fingerprints
- Status: SKIPPED (did not run)
- Expected Result: Billing in second Signed window aligns with second driver fingerprints.
- Actual Result: Not executed in this run due to prior failure flow in the same suite group.
- Portal data links:
  - Service contract: not available (test not executed).
  - Billing run: not available (test not executed).

TC-BE-26
- Title: [PDT-2599] TC-BE-26: PER_PIECE - billing after Draft v3 start - links latest Signed v2
- Status: SKIPPED (did not run)
- Expected Result: Billing after Draft v3 start should link to latest Signed v2.
- Actual Result: Not executed in this run due to prior failure flow in the same suite group.
- Portal data links:
  - Service contract: not available (test not executed).
  - Billing run: not available (test not executed).

TC-BE-27
- Title: [PDT-2599] TC-BE-27: OVER_TIME_ONE_TIME - first Signed window - invoices issued (distinct drivers v1/v2)
- Status: FAILED
- Expected Result: Invoice line totals in first Signed window should match expected v1 price component calculation.
- Actual Result: Assertion failed (`toBeCloseTo`); expected 14, actual 98 for invoice line sum comparison.
- Failure excerpt: invoice line sum mismatch vs v1 component.
- Portal data links:
  - Service contract: not captured in this billing flow output.
  - Billing run: not captured; test failed before billing portal attachment/logging step.

TC-BE-28
- Title: [PDT-2599] TC-BE-28: OVER_TIME_ONE_TIME - second Signed window - billing aligns second driver
- Status: SKIPPED (did not run)
- Expected Result: Billing in second Signed window aligns with second driver.
- Actual Result: Not executed in this run due to prior failure flow in the same suite group.
- Portal data links:
  - Service contract: not available (test not executed).
  - Billing run: not available (test not executed).

TC-BE-29
- Title: [PDT-2599] TC-BE-29: OVER_TIME_ONE_TIME - billing after Draft v3 start - links latest Signed v2
- Status: SKIPPED (did not run)
- Expected Result: Billing after Draft v3 start links to latest Signed v2.
- Actual Result: Not executed in this run due to prior failure flow in the same suite group.
- Portal data links:
  - Service contract: not available (test not executed).
  - Billing run: not available (test not executed).

TC-BE-30
- Title: [PDT-2599] TC-BE-30: OVER_TIME_PERIODICAL - first Signed window - invoices issued (distinct drivers v1/v2)
- Status: FAILED
- Expected Result: Invoice line totals in first Signed window should match expected periodic calculation for v1/v2 drivers.
- Actual Result: Assertion failed (`toBeCloseTo`); expected 21, actual 28 for invoice line sum comparison.
- Failure excerpt: invoice line sum mismatch vs v1 component.
- Portal data links:
  - Service contract: not captured in this billing flow output.
  - Billing run: not captured; test failed before billing portal attachment/logging step.
