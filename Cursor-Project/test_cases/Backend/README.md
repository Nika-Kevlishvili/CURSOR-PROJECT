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
| `Version_Validity_Three_Processes_PDT_2815.md` | Mass Email/SMS/penalty version validity (Valid/Signed); description-vs-code gap tests; PDT-2599 attachment | PDT-2815 | TC-BE-1 through TC-BE-17 |
| `Service_Contract_First_Version_StartDate_PDT_2846.md` | Service Contract first version startDate re-alignment — auto-realign v1 startDate to signingDate during finalization; guard checks, uniqueness, version ordering | PDT-2846 | TC-BE-1 through TC-BE-20 |
| `PDT_2872_minimal_interim_payment.md` | Minimal interim payment — 5 EUR incl. VAT gate; self-contained preconditions per TC; no skip/log observability TCs | PDT-2872 | TC-BE-1 through TC-BE-14 |
| `Product_contract_creation.md` | Product Contract creation — happy path, POD linkage, validation errors (missing customer, grid operator mismatch, inactive product) | N/A (General) | TC-BE-1 through TC-BE-5 |
| `Single_email_multiple_recipients_PDT_2881.md` | Email Communication — one physical outbound email for multiple recipients (shared `task_id`, semicolon addresses, invoice aggregation, resend) | PDT-2881 | TC-BE-1 through TC-BE-10 |
| `Skip_Risklist_Product_Contract_Mass_Import_PDT_2931.md` | Skip RiskList permission on product contract mass import — create, edit same version (`E`), edit new version (`C`); all rows succeed with skip; negative control without permission | PDT-2931 | TC-BE-1 through TC-BE-7 |
