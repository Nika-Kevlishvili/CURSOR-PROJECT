# Production Data Report: Invoice 38654 & Billing Data by Profile 313371, 207980

**Environment:** Prod (PostgreSQLProd)  
**Date:** 2026-03-09  
**Source:** invoice.invoices, pod.billing_data_by_profile (read-only)

---

## 1. Invoice 38654

| Field | Value |
|-------|--------|
| **create_date** | **2026-01-22 15:46:17 UTC** |
| id | 38654 |
| invoice_number | ЕФП-1100039497 |
| invoice_date | 2026-01-22 |
| status | REAL |
| type | STANDARD |
| system_user_id | system |

**Conclusion:** Invoice 38654 was created on **2026-01-22 at 15:46:17 UTC** in production.

---

## 2. Billing Data by Profile 207980

| Field | Value |
|-------|--------|
| **create_date** | **2026-01-10 07:32:47 UTC** |
| id | 207980 |
| billing_by_profile_id | 1078 |
| period_from | 2025-12-17 20:45:00 UTC |
| period_to | 2025-12-17 21:00:00 UTC |
| value | 0 |
| system_user_id | phoenix.testc |

**Conclusion:** Billing data by profile 207980 was created on **2026-01-10 at 07:32:47 UTC**.

---

## 3. Billing Data by Profile 313371

| Field | Value |
|-------|--------|
| **create_date** | **2026-01-10 07:33:29 UTC** |
| id | 313371 |
| billing_by_profile_id | 1113 |
| period_from | 2025-12-30 16:30:00 UTC |
| period_to | 2025-12-30 16:45:00 UTC |
| value | 36.8 |
| system_user_id | phoenix.testc |

**Conclusion:** Billing data by profile 313371 was created on **2026-01-10 at 07:33:29 UTC**.

---

## Summary

- **Invoice 38654:** created **2026-01-22 15:46:17 UTC** (table: invoice.invoices).
- **Billing data by profile 207980:** created **2026-01-10 07:32:47 UTC** (table: pod.billing_data_by_profile).
- **Billing data by profile 313371:** created **2026-01-10 07:33:29 UTC** (table: pod.billing_data_by_profile).

Both billing_data_by_profile records were created on the same day (2026-01-10) by user `phoenix.testc`, about 42 seconds apart.
