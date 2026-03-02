# Contract 1448 – Test vs Prod Difference

**Contract ID:** 1448  
**Report date:** 2026-03-02  
**Environments:** PostgreSQLTest (10.236.20.24) vs PostgreSQLProd (10.236.20.78:5000)

---

## Comparison (product_contract.contracts)

| Field | Test | Prod | Difference |
|-------|------|------|------------|
| id | 1448 | 1448 | Same |
| contract_number | ПКСП-2501002746 | ПКСП-2501002746 | Same |
| status | ACTIVE | ACTIVE | Same |
| contract_status | TERMINATED | TERMINATED | Same |
| contract_sub_status | ALL_PODS_ARE_DEACTIVATED | ALL_PODS_ARE_DEACTIVATED | Same |
| create_date | 2025-12-14 09:40:24.367 | 2025-12-14 09:40:24.367 | Same |
| system_user_id | phoenix.test | phoenix.test | Same |
| **modify_date** | **2026-03-02 11:46:23.312 UTC** | **2026-03-02 11:51:04.084 UTC** | **Different (~5 min)** |
| modify_system_user_id | a32434 | a32434 | Same |
| signing_date | 2025-01-29 | 2025-01-29 | Same |
| entry_into_force_date | 2025-01-29 | 2025-01-29 | Same |
| activation_date | 2025-02-01 | 2025-02-01 | Same |
| termination_date | 2026-02-28 | 2026-02-28 | Same |
| contract_term_end_date | 2090-12-31 | 2090-12-31 | Same |
| perpetuity_date | NULL | NULL | Same |
| supply_activation_date | 2026-01-01 | 2026-01-01 | Same |
| initial_term_start_date | 2025-02-01 | 2025-02-01 | Same |
| resign_to_contract_id | 33128 | 33128 | Same |
| is_locked | false | false | Same |
| contract_status_modify_date | 2026-03-02 00:00:00 | 2026-03-02 00:00:00 | Same |
| **contract_term_end_date_modify_date** | **2026-03-02 11:46:23.312 UTC** | **2026-03-02 11:51:04.083 UTC** | **Different (~5 min)** |

---

## Summary of differences

**Only 2 fields differ:**

1. **modify_date**  
   - Test: 2026-03-02 **11:46:23** UTC  
   - Prod: 2026-03-02 **11:51:04** UTC  
   → Prod was updated about 5 minutes later than Test.

2. **contract_term_end_date_modify_date**  
   - Test: 2026-03-02 **11:46:23** UTC  
   - Prod: 2026-03-02 **11:51:04** UTC  
   → Same time difference (last change that touched this field was later on Prod).

**Conclusion:** Business data is the same in both environments (contract_number, status, contract_status, termination_date, dates, resign_to_contract_id, etc.). Only the “last modified” timestamps differ: last update in **Test** at 11:46:23 (a32434), in **Prod** at 11:51:04 (a32434). So the same user updated the contract in Test first, then in Prod (or in a different order), and no other field differs.
