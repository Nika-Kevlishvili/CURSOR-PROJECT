# Production: Invoice 38654 – POD 32Z103000002647T Usage Check

**Date:** 2026-03-09  
**Environment:** Production (PostgreSQLProd, readonly_user)  
**Request:** Check if POD `32Z103000002647T` is used in invoice 38654.

---

## Result: **NO** – POD 32Z103000002647T is **not** used in invoice 38654

---

## Data checked

### Invoice 38654 (header)
| Field           | Value        |
|----------------|--------------|
| id             | 38654        |
| invoice_number | ЕФП-1100039497 |
| pod_id         | null         |
| customer_id    | 6024432      |
| status         | REAL         |

### POD 32Z103000002647T
| Field      | Value |
|-----------|-------|
| id        | 9136  |
| identifier| 32Z103000002647T |

### Checks performed

1. **invoice.invoices** – `pod_id` for invoice 38654 is **null** (no single POD at header level).
2. **invoice.invoice_detailed_data** – no rows for `invoice_id = 38654` (table empty for this invoice).
3. **invoice.invoice_standard_detailed_data** – 1,113 rows for `invoice_id = 38654`; **none** with `pod_id = 9136`.
   - Distinct PODs in this invoice do **not** include 9136 (32Z103000002647T).

### Conclusion

POD **32Z103000002647T** (pod_id 9136) is **not** present in invoice 38654 (ЕФП-1100039497). The invoice contains many other PODs in its standard detailed data, but 9136 is not among them.
