# Backend Test Cases

Backend (API) test cases. Each `.md` file covers one topic (task, bug, feature).

TC numbering: `TC-BE-1`, `TC-BE-2`, ...

## Files

| File | Topic | Jira | TCs |
|------|-------|------|-----|
| `Zero_amount_liability_receivable.md` | Zero-amount prevention across all liability and receivable generation flows | PDT-2474 | TC-BE-1 through TC-BE-60 |
| `Correction_data_by_scales_header_period.md` | Correction data by scales — header period must match original | PDT-2708 | TC-BE-1 through TC-BE-20 |
| `Get_product_list_energy_products.md` | Get product list (Energy products) — Sales Portal product catalog | GET-PRODUCT-LIST | TC-BE-1 through TC-BE-58 |
| `Get_product_list_energy.md` | Get product list (Energy products) — comprehensive API coverage (all filtering rules, auth, schema, regression) | PHN-2178 | TC-BE-1 through TC-BE-94 |
| `Customer_invoice_date_label.md` | Customer invoices list — invoice date label translation key (pure FE bug; no backend TCs applicable) | PDT-2811 | N/A |
| `Service_Contract_Versioning_PDT_2599.md` | Service Contract versioning — Signed/Not Valid rules, timelines, status transitions, billing resolution, Swagger parity | PDT-2599 | TC-BE-1 through TC-BE-26 |
| `Missing_Interim_Invoice_PDT_2750.md` | Missing interim invoice — interim data preparation, REAL parent / POD fallback, PDF gating, deductions; re-sign/terminate + FOR_VOLUMES parent exclusion; null contract-POD deactivation | PDT-2750 | TC-BE-1 through TC-BE-15 |
| `Volume_billing_WITH_ELECTRICITY_POD_deactivation_PDT_2376.md` | Consolidated eight-POD contract — WITH_ELECTRICITY invoice detailed rows obey `YearMonth(deactivation)` vs invoice month-end; OPEN accounting-period anchor (preferred **2025-12-31**) | PDT-2376 | TC-BE-1 through TC-BE-2 |
| `Payment_Mass_Import_Offsetting_Failure_PDT_2713.md` | Payment mass import vs `PaymentService.create`, `automatic_payment_offsetting_out`, rollback on `Automatic payment offsetting out failed;`, `:20:` date, parity, mixed/all-failed | PDT-2713 | TC-BE-1 through TC-BE-14 |
