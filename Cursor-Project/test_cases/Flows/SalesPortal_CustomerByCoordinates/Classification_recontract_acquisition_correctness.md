# PHN-2529 - Classification Recontract vs Acquisition Correctness

**Scope:** Verify classification accuracy and regression safety for customer categorization.

## Preconditions

- Test dataset includes:
  - clear `recontract` candidates,
  - clear `acquisition` candidates,
  - boundary/ambiguous records near rule thresholds.
- Business classification rules are documented for expected outcomes.

## TC-1: Known recontract customers are classified as recontract

**Steps:**
1. Query coordinates that include known recontract fixtures.
2. Validate returned classification field/value per customer.

**Expected result:**
- Recontract fixtures are classified as `recontract`.

## TC-2: Known acquisition customers are classified as acquisition

**Steps:**
1. Query coordinates that include known acquisition fixtures.
2. Validate classification values.

**Expected result:**
- Acquisition fixtures are classified as `acquisition`.

## TC-3: Boundary-condition records classify per business rule

**Steps:**
1. Query for records on temporal/status thresholds used in classification.
2. Compare output class with documented rule expectation.

**Expected result:**
- Boundary records are classified exactly per specification.
- No inconsistent class toggling across repeated calls.

## TC-4: Classification remains stable across pagination

**Steps:**
1. Fetch all pages for the same query.
2. Re-fetch and compare classification for each customer ID.

**Expected result:**
- Same customer ID has same classification across pages and repeats.

## TC-5: Classification field contract is always present and valid

**Steps:**
1. Execute valid requests over mixed datasets.
2. Validate classification field is present and within allowed enum values.

**Expected result:**
- No unknown or malformed classification values.
- Nullability behavior matches API contract.

## TC-6: Regression check when underlying status/date data changes

**Steps:**
1. Update fixture state to move a customer from one category rule branch to another (in test setup).
2. Re-run query and validate new classification.

**Expected result:**
- Classification changes only when rule-driving data changes.
- No stale classification artifacts.
# Classification recontract/acquisition correctness

## TC-CLS-01 - Recontract customer classification correctness
**Rationale:** Covers classification drift risk in native repository query output.

**Preconditions**
- Dataset includes customers that must be classified as recontract.

**Steps**
1. Call endpoint with coordinates covering recontract customers.
2. Inspect classification field in response.
3. Cross-check sample records against expected business classification source.

**Expected result**
- Recontract customers are labeled exactly as defined by business contract.
- No false acquisition labels for recontract records.

## TC-CLS-02 - Acquisition customer classification correctness
**Rationale:** Verifies opposite branch of classification logic.

**Preconditions**
- Dataset includes customers that must be classified as acquisition.

**Steps**
1. Call endpoint with coordinates covering acquisition customers.
2. Validate response classification values.

**Expected result**
- Acquisition customers are labeled correctly.
- No false recontract labels.

## TC-CLS-03 - Mixed dataset classification stability across pages
**Rationale:** Detects drift caused by mapping or pagination interaction.

**Preconditions**
- Dataset includes both classification groups across multiple pages.
- Valid auth token is available.

**Steps**
1. Query a coordinate range with both recontract and acquisition records across multiple pages.
2. Validate classifications on each page and after repeated calls.

**Expected result**
- Classification remains stable across pages and repeated calls.
- No intermittent flip/flop in labels.

## TC-CLS-04 - Historical contracts drive expected classification
**Rationale:** Covers high-risk classification rule tied to contract history.

**Preconditions**
- Dataset includes customers with multiple contracts and known historical outcomes.
- Expected classification is pre-validated from business rules/data setup.
- Valid auth token is available.

**Steps**
1. Query coordinates including customers with rich contract history.
2. Compare returned classification with expected business classification per customer.

**Expected result**
- Classification matches expected historical contract interpretation.
- No customer is misclassified due to older/newer contract precedence issues.

## TC-CLS-05 - Region boundary behavior keeps classification correct
**Rationale:** Verifies region/area boundaries do not alter classification semantics.

**Preconditions**
- Dataset includes customers near region boundary lines.
- Valid auth token is available.

**Steps**
1. Call endpoint for coordinates/radius covering region-boundary customers.
2. Validate both inclusion/filtering and classification values for boundary records.

**Expected result**
- Boundary records follow expected inclusion rule.
- Classification remains correct and independent from boundary artifacts.

## TC-CLS-06 - Null/partial contract data handled without misclassification
**Rationale:** Guards shared repository logic when source data is incomplete.

**Preconditions**
- Dataset includes edge records with partial/null auxiliary data relevant to classification.
- Valid auth token is available.

**Steps**
1. Query dataset containing partial-data records.
2. Inspect classification values and response stability.

**Expected result**
- Endpoint returns stable response without runtime errors.
- Classification follows fallback business rule (or documented default) for partial data.
