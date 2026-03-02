# Production Contract 1448 – Audit Log (Fresh Extract)

**Environment:** Prod (PostgreSQLProd)  
**Contract ID:** 1448  
**Schema:** product_contract.contracts  
**Report Date:** 2026-03-02  
**Source:** audit.logged_actions (read-only)

---

## Current Contract Snapshot (as of 2026-03-02)

| Field | Value |
|-------|--------|
| Contract number | ПКСП-2501002746 |
| Status | ACTIVE |
| Contract status | TERMINATED |
| Termination date | **2026-02-28** |
| Create date | 2025-12-14 09:40:24 UTC |
| Created by | phoenix.test |
| Last modified | **2026-03-02 11:51:04 UTC** |
| Last modified by | **a32434** |

---

## Full Audit Log (Creation to Latest)

| # | Event ID | Time (UTC) | Action | User | Changes |
|---|----------|------------|--------|------|---------|
| 1 | 3458291 | 2025-12-14 09:40:24 | **INSERT** | phoenix.test | Contract created. contract_number=TMP-TEMP, contract_status=SIGNED, contract_sub_status=SIGNED_BY_BOTH_SIDES, entry_into_force_date=2025-01-29, contract_term_end_date=2026-01-31. |
| 2 | 3458301 | 2025-12-14 09:40:24 | UPDATE | phoenix.test | contract_number: TMP-TEMP → EPES2512000419. |
| 3 | 3463837 | 2025-12-15 00:00:00 | UPDATE | system | contract_status: SIGNED → ENTERED_INTO_FORCE; contract_sub_status: SIGNED_BY_BOTH_SIDES → AWAITING_ACTIVATION. |
| 4 | 5429930 | 2025-12-31 08:07:39 | UPDATE | (unchanged) | contract_number: EPES2512000419 → ПКСП-2501002746. |
| 5 | 5536548 | 2026-01-02 06:47:02 | UPDATE | v6981 | activation_date=2025-02-01; contract_status: ENTERED_INTO_FORCE → ACTIVE_IN_TERM; contract_sub_status: AWAITING_ACTIVATION → DELIVERY. |
| 6 | 197981060 | 2026-02-01 02:00:06 | UPDATE | system | contract_status: ACTIVE_IN_TERM → ACTIVE_IN_PERPETUITY; perpetuity_date=2026-02-01; contract_term_end_date: 2026-01-31 → 2090-12-31. |
| 7 | 220367030 | 2026-02-13 15:38:13 | UPDATE | a36344 | modify_date, modify_system_user_id, contract_status_modify_date, contract_term_end_date_modify_date. |
| 8 | 225222570 | 2026-02-17 09:39:45 | UPDATE | a36388 | resign_to_contract_id: NULL → 33128. |
| 9 | 225222912 | 2026-02-17 10:31:25 | UPDATE | system | contract_status: ACTIVE_IN_PERPETUITY → TERMINATED; termination_date: NULL → **2025-12-31**; contract_sub_status: DELIVERY → ALL_PODS_ARE_DEACTIVATED. |
| 10 | 288659185 | 2026-02-25 08:52:06 | UPDATE | a36344 | contract_status: TERMINATED → ACTIVE_IN_PERPETUITY; termination_date: 2025-12-31 → NULL; contract_sub_status: ALL_PODS_ARE_DEACTIVATED → DELIVERY. |
| 11 | 288760922 | 2026-02-26 02:02:02 | UPDATE | system | contract_status: ACTIVE_IN_PERPETUITY → TERMINATED; termination_date: NULL → 2025-12-31; contract_sub_status: DELIVERY → ALL_PODS_ARE_DEACTIVATED. |
| 12 | 299594883 | 2026-02-27 12:55:05 | UPDATE | a36344 | contract_status: TERMINATED → ACTIVE_IN_PERPETUITY; termination_date: 2025-12-31 → NULL; contract_sub_status: ALL_PODS_ARE_DEACTIVATED → DELIVERY. |
| 13 | 299630468 | 2026-02-28 02:02:04 | UPDATE | system | contract_status: ACTIVE_IN_PERPETUITY → TERMINATED; termination_date: NULL → 2025-12-31; contract_sub_status: DELIVERY → ALL_PODS_ARE_DEACTIVATED. |
| **14** | **303620850** | **2026-03-02 11:50:19** | **UPDATE** | **a32434** | **perpetuity_date: 2026-02-01 → NULL; termination_date: 2025-12-31 → 2026-02-28; contract_sub_status: ALL_PODS_ARE_DEACTIVATED → FROM_CUSTOMER_WITH_NOTICE; modify_system_user_id: system → a32434.** |
| **15** | **303620867** | **2026-03-02 11:51:03** | **UPDATE** | **a32434** | **contract_sub_status: FROM_CUSTOMER_WITH_NOTICE → ALL_PODS_ARE_DEACTIVATED; modify_date, contract_term_end_date_modify_date.** |

---

## New Events (since previous report)

- **Event 14 (2026-03-02 11:50:19):** User **a32434** updated the contract: **termination_date** changed from **2025-12-31** to **2026-02-28** (aligned with second version POD deactivation); perpetuity_date set to NULL; contract_sub_status → FROM_CUSTOMER_WITH_NOTICE.
- **Event 15 (2026-03-02 11:51:03):** Same user a32434: contract_sub_status FROM_CUSTOMER_WITH_NOTICE → ALL_PODS_ARE_DEACTIVATED (no change to termination_date; it remains 2026-02-28).

**Summary:** Termination date is now **2026-02-28** (was 2025-12-31). Last modifier: **a32434** (2026-03-02 11:51:04).

---

## Data Source

- **Table:** audit.logged_actions  
- **Filter:** schema_name = 'product_contract', table_name = 'contracts', row_data/changed_fields/old_fields id = 1448  
- **Credentials:** readonly_user (read-only)
