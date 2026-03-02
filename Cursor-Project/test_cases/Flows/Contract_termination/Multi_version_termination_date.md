# Product Contract – Multi-Version Termination Date (PDT-2610)

**Bug reference:** PDT-2610 – Product Contract Incorrect Termination Date  
**Related reports:** BugValidation_PDT-2610_Product_Contract_Termination_Date.md, Contract_1448_Prod_Audit_Log.md, PDT-2610_Reproduction_Steps_Dev.md  
**Cross-dependency:** cross_dependencies/2026-03-02_PDT-2610-product-contract-termination-date.json

**Scope:** Contract with multiple versions and different POD deactivation dates per version; termination_date in Basic Parameter tab; scheduler overwriting user correction; POD-based vs term-based termination.

---

## Test data (preconditions)

- **Contract:** One product contract with **2 versions** (2 contract_details with different `start_date`).
- **Version 1:** All PODs have `deactivation_date = 31.12.2025` and are deactivated.
- **Version 2:** All PODs have `deactivation_date = 28.02.2026` and are deactivated.
- **Eligibility:** Contract is eligible for "all PODs deactivated" termination (current version has no ACTIVE PODs without deactivation_date).
- **Expected business rule (per bug):** Termination date should reflect the second version (e.g. 28.02.2026) or the correct rule for multiple versions; not the first version's POD deactivation (31.12.2025).

---

## TC-1: Display of termination date for multi-version contract (wrong date vs expected)

**Objective:** Verify that the termination date shown in the Basic Parameter tab correctly reflects the intended rule for multi-version contracts (e.g. latest POD deactivation across versions or current version), and document current wrong behaviour (31.12.2025 vs expected 28.02.2026).

**Preconditions:** Contract with 2 versions as in Test data; scheduler (POD-based termination) has already run and set `contract_status` and `termination_date`.

**Steps:**
1. Open the product contract in Phoenix EPRES (or equivalent UI).
2. Navigate to the **Basic Parameter** tab.
3. Read the displayed **Termination date** value.
4. (Optional) Call GET `/product-contract/{id}` and confirm `termination_date` in the response matches the UI.
5. In the database, verify for the same contract: `product_contract.contracts.termination_date`, and `product_contract.contract_pods.deactivation_date` per version (version 1: 31.12.2025, version 2: 28.02.2026).

**Expected result (current bug):** Basic Parameter tab and API show **31.12.2025** (first version's POD deactivation) instead of **28.02.2026** (second version). After fix: termination date should reflect the defined business rule (e.g. 28.02.2026 or latest across versions).

**References:** BugValidation report §2.4; cross-dependency "Basic Parameter tab display", "Multi-version contracts".

---

## TC-2: System overwriting user correction (scheduler re-applying wrong date)

**Objective:** Verify that when the user manually corrects the termination date, the scheduler (or term-based job) does not overwrite it back to the first version's date (31.12.2025) with `modify_system_user_id = system`.

**Preconditions:** Same contract as TC-1; termination date is currently 31.12.2025 (wrong). User has permission to edit Basic Parameters.

**Steps:**
1. Open the contract → **Basic Parameter** tab.
2. Change **Termination date** to 28.02.2026 (or another valid value reflecting second version) and save (e.g. via PUT `/product-contract/{id}` with `ProductContractBasicParametersUpdateRequest.terminationDate` or UI save).
3. Verify that the UI (and GET `/product-contract/{id}`) now shows 28.02.2026.
4. Trigger or wait for the next run of the termination scheduler (POD-based and/or term-based).
5. Re-open the contract → Basic Parameter tab and check Termination date.
6. In the database, check `product_contract.contracts.termination_date` and `modify_system_user_id` (or equivalent "last modified by").
7. (Optional) Query `audit.logged_actions` for this contract and confirm sequence: user update then system update (TERMINATED, termination_date 2025-12-31).

**Expected result (current bug):** After scheduler run, termination date reverts to **31.12.2025** and last modifier is **system**. After fix: user correction is either preserved or overwrite behaviour is aligned with defined business rules.

**References:** BugValidation report §2.5 (audit log events 8–13); cross-dependency "User manual correction", "Audit trail".

---

## TC-3: POD-based vs term-based termination and eligibility (integration)

**Objective:** Ensure test cases cover which termination path sets termination_date (POD-based vs term-based) and that eligibility/query returns the expected contract and deactivation date for multi-version contracts.

**Preconditions:** Contract with 2 versions as in Test data. Access to run or stub termination logic (e.g. test endpoints or scheduler).

**Steps:**
1. Call GET `/ttest/contracts-to-terminate-with-pods` (or equivalent) and confirm the contract appears in the list; note the deactivation date returned for it (intended: latest across versions, e.g. 28.02.2026).
2. Run POD-based termination only (e.g. GET `/ttest/pod-termination` or equivalent) for the scope that includes this contract; then read `product_contract.contracts.termination_date` for the contract.
3. If term-based termination is available (e.g. GET `/ttest/term-termination` or PATCH `/ttest/execute-termination-by-terms`), run it for the same contract and check whether termination_date changes (e.g. overwritten by `contract_term_end_date` from first version).
4. Verify repository behaviour: `ProductContractRepository.getProductContractsToTerminationWithPodsDeactivation` – deactivation subquery uses `order by cp.deactivation_date desc limit 1`; eligibility uses current version. Document which deactivation date is used for this contract in the test environment.

**Expected result (current bug):** Either the query returns 31.12.2025 (so POD-based sets wrong date), or term-based termination overwrites with contract_term_end_date (e.g. 31.12.2025). After fix: POD-based uses correct date (e.g. 28.02.2026); term-based does not incorrectly overwrite when POD-based is the correct source.

**References:** BugValidation report §2.1–2.3; cross-dependency "Eligibility and query result", "Term-based termination", "Integration points".

---

## TC-4: Regression – Basic Parameter tab and API GET/PUT

**Objective:** After any fix for PDT-2610, ensure Basic Parameter tab and product-contract API still correctly display and accept termination_date (no regression on single-version contracts or on edit/save flow).

**Preconditions:** Fix for PDT-2610 deployed; at least one single-version contract and one multi-version contract (with known expected termination date per business rule).

**Steps:**
1. Open a **single-version** product contract with termination date set → Basic Parameter tab shows the same value; GET `/product-contract/{id}` returns the same `termination_date`.
2. Edit termination date via UI or PUT `/product-contract/{id}` and save; reload and confirm value is persisted and displayed.
3. Repeat for the **multi-version** contract: display matches defined rule; edit and save does not get overwritten incorrectly by scheduler (per TC-2).

**Expected result:** No regression: display and update of termination_date work for both single-version and multi-version contracts according to the defined business rules.

**References:** Cross-dependency "Basic Parameter tab display", "product-contract-controller GET/PUT".

---

## TC-5: Regression – Audit trail for termination_date and contract_status

**Objective:** Ensure audit trail (audit.logged_actions) still correctly records who set termination_date and contract_status (user vs system) after any fix.

**Preconditions:** Fix for PDT-2610 deployed; contract that undergoes termination and optionally user correction.

**Steps:**
1. Perform user update of termination date (or status) for a product contract; query `audit.logged_actions` for that contract and confirm the user id and new values are logged.
2. Run the termination scheduler so that it updates the same contract; query `audit.logged_actions` again and confirm system-driven update is logged.
3. Verify ordering of events and that no audit entries are lost or duplicated.

**Expected result:** Audit trail remains correct and complete; no regression in logging of termination_date and contract_status changes.

**References:** Cross-dependency "Audit trail"; BugValidation report §2.5.

---

## TC-6: Regression – Test endpoints (contracts-to-terminate, pod-termination, term-termination)

**Objective:** After any fix, test endpoints that list contracts to terminate and execute POD-based or term-based termination still return consistent data and behave as expected.

**Preconditions:** Fix for PDT-2610 deployed; test environment with known multi-version contract.

**Steps:**
1. GET `/ttest/contracts-to-terminate-with-pods`: verify response includes expected contracts and that the deactivation date per contract matches the intended rule (e.g. latest across versions).
2. GET `/ttest/pod-termination` (or equivalent): run and verify termination_date set on target contract(s) matches POD-based rule.
3. GET `/ttest/term-termination` or PATCH `/ttest/execute-termination-by-terms`: verify term-based termination does not overwrite POD-based date when it should not.
4. Re-run TC-1 and TC-2 scenarios using these endpoints where applicable to ensure full flow still passes.

**Expected result:** Test endpoints behave consistently with the fixed business rules; contract list and dates are predictable and documented.

**References:** Cross-dependency "Test endpoints"; BugValidation report (test-product-contract-termination-controller).

---

## Confluence / code references (summary)

| Topic | Reference |
|-------|-----------|
| Termination date (contract-level) | Confluence: Product contract edit (2228419), Contract statuses logic (80510980), Contract Termination (172490754). |
| POD-based termination | ProductContractTerminationWithPodsService.java (lines 66–88); termination_date from terminationModel.getDeactivationDate(). |
| Repository query | ProductContractRepository.getProductContractsToTerminationWithPodsDeactivation; deactivation subquery order by cp.deactivation_date desc limit 1; eligibility current version. |
| Term-based overwrite | ProductContractTermTerminationService (lines 156–168): sets termination_date from productContract.getContractTermEndDate() for EXPIRED. |
| Basic Parameter / API | product_contract.contracts.termination_date; GET/PUT /product-contract/{id}. |
| Test endpoints | /ttest/pod-termination, /ttest/term-termination, /ttest/contracts-to-terminate-with-pods, PATCH /ttest/execute-termination-by-terms. |
