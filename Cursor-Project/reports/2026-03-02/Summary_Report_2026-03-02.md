# Summary Report – 2026-03-02

**Report date:** 2026-03-02  
**Scope:** Contract 1448 (Prod/Test), bug PDT-2610, test cases structure, comparisons.

---

## 1. Reports generated or updated this session

| Report | Path | Description |
|--------|------|-------------|
| Contract 1448 – Prod audit log (fresh) | `reports/2026-03-02/Contract_1448_Prod_Audit_Log_2026-03-02.md` | Full audit log from creation to latest; 15 events; new events 14–15 (2026-03-02, user a32434). |
| Contract 1448 – Test vs Prod diff | `reports/2026-03-02/Contract_1448_Test_vs_Prod_diff.md` | Field-by-field comparison; only modify_date and contract_term_end_date_modify_date differ (~5 min). |

---

## 2. Contract 1448 – Key facts (Prod)

- **Contract ID:** 1448  
- **Contract number:** ПКСП-2501002746  
- **Current state:** status ACTIVE, contract_status TERMINATED, termination_date **2026-02-28**  
- **Last modified:** 2026-03-02 11:51:04 UTC by **a32434**  
- **Last change to termination date:** 2026-03-02 11:50:19 (a32434): 2025-12-31 → 2026-02-28  
- **Source:** product_contract.contracts + audit.logged_actions (PostgreSQLProd, readonly_user)

---

## 3. Contract 1448 – Test vs Prod

- **Same:** All business fields (contract_number, status, contract_status, contract_sub_status, termination_date 2026-02-28, dates, resign_to_contract_id 33128, etc.).  
- **Different:** Only **modify_date** and **contract_term_end_date_modify_date** (Test: 11:46:23 UTC, Prod: 11:51:04 UTC).  
- **Conclusion:** Data aligned; Prod updated ~5 minutes after Test by same user (a32434).

---

## 4. Bug PDT-2610 (Product Contract – Incorrect Termination date)

- **Validated:** Bug VALID (BugFinderAgent).  
- **Issue:** Multi-version contract; termination date showed 31.12.2025 (first version) instead of 28.02.2026 (second version); system overwrote user correction.  
- **Related:** Contract 1448; fix/update by a32434 on 2026-03-02 set termination_date to 2026-02-28 in both Test and Prod.  
- **Reports:** BugValidation_PDT-2610_Product_Contract_Termination_Date.md, PDT-2610_Reproduction_Steps_Dev.md (if present in repo).

---

## 5. Test cases

- **Structure:** `Cursor-Project/test_cases/` – **Objects/** (by entity) and **Flows/** (by flow) as siblings; README in each folder (see `.cursor/rules/workspace/test_cases_structure.mdc`).  
- **Objects/Product_contract/Create.md:** Product contract create – 7 test cases (valid create, validations, API, termination date disabled, duplicate number).  
- **Flows/Contract_termination/Multi_version_termination_date.md:** PDT-2610 – 6 test cases (display, system overwrite, POD vs term-based, regression Basic Parameter, audit, test endpoints).  

---

## 6. Data sources

- **Prod:** PostgreSQLProd (10.236.20.78:5000, phoenix, readonly_user).  
- **Test:** PostgreSQLTest (10.236.20.24:5432, phoenix, postgres).  
- **Audit:** audit.logged_actions (schema_name = 'product_contract', table_name = 'contracts').

---

**End of summary report.**
