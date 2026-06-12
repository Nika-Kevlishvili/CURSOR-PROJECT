# Test Cases

Test case files use a **two-folder layout** by testing layer:

- **`Backend/`** — Backend (API) test cases (`TC-BE-N`). **Always** one `.md` file per topic when test cases are generated.
- **`Frontend/`** — Frontend (UI) test cases (`TC-FE-N`). Created **only when** the user chose Backend+Frontend (TC-FRONTEND-ASK.0).

Topics may exist in Backend only (e.g. API-only bugs). The index table may show `—` in the Frontend column for those topics.

## Preconditions (STANDALONE vs legacy)

- **New topics (mandatory):** Rule **TC-STANDALONE-PRE.0** — each TC's `Preconditions:` lists the **full numbered setup chain** for that scenario. See `Cursor-Project/config/template/Test_case_template.md`.
- **Legacy topics (read-only):** Four PDT topics still use `Apply Test data steps 1–N` (DRY). **Do not** copy that pattern into new files. Automated expansion via `expand-test-data-standalone.ps1` is **disabled** until multiline Test data steps are supported — expand manually when editing those files.

## Template

Every `.md` file MUST follow **`Cursor-Project/config/template/Test_case_template.md`**.

## Index

| Topic | Backend | Frontend | Jira |
|-------|---------|----------|------|
| Zero-amount prevention across all liability and receivable generation flows | `Backend/Zero_amount_liability_receivable.md` | `Frontend/Zero_amount_liability_receivable.md` | PDT-2474 |
| Correction data by scales — header period must match original | `Backend/Correction_data_by_scales_header_period.md` | `Frontend/Correction_data_by_scales_header_period.md` | PDT-2708 |
| Get product list (Energy products) — Sales Portal product catalog | `Backend/Get_product_list_energy_products.md` | `Frontend/Get_product_list_energy_products.md` | GET-PRODUCT-LIST |
| Get product list (Energy products) — comprehensive API & UI coverage (all filtering rules, auth, schema, regression) | `Backend/Get_product_list_energy.md` | `Frontend/Get_product_list_energy.md` | PHN-2178 |
| Customer invoices list — invoice date label uses wrong translation key (billing_data_by_scales reuse) | `Backend/Customer_invoice_date_label.md` | `Frontend/Customer_invoice_date_label.md` | PDT-2811 |
| Service Contract versioning — lifecycle, timeline, bilingual validation, billing by effective Signed version (Backend **TC-BE-1..35** = Playwright titles 1:1) | `Backend/Service_Contract_Versioning_PDT_2599.md` | `Frontend/Service_Contract_Versioning_PDT_2599.md` (TC-FE manual / no PDT-2599 FE spec) | PDT-2599 |
| Missing interim invoice — standard billing, REAL parent selection, month-boundary churn; re-sign/terminate + FOR_VOLUMES exclusion; null POD deactivation (Keti thread) | `Backend/Missing_Interim_Invoice_PDT_2750.md` | `Frontend/Missing_Interim_Invoice_PDT_2750.md` | PDT-2750 |
| Eight-POD consolidated volume billing — WITH_ELECTRICITY tariff rows keyed by POD deactivation `YearMonth` vs invoice anchor month (`YearMonth`; preferred **2025-12-31**) | `Backend/Volume_billing_WITH_ELECTRICITY_POD_deactivation_PDT_2376.md` | `Frontend/Volume_billing_WITH_ELECTRICITY_POD_deactivation_PDT_2376.md` | PDT-2376 |
| Resources (Operations management) — business documents upload, list, search, sort, download, delete, edit | `Backend/Resources_Operations_management_PDT_2690.md` | `Frontend/Resources_Operations_management_PDT_2690.md` | PDT-2690 |
| General business document library (Portal) — **restored twin** of PDT-2690 scenarios under distinct filenames | `Backend/General_business_document_library_Portal_PDT2690.md` | `Frontend/General_business_document_library_Portal_PDT2690.md` | PDT-2690 |
| Payment mass import — automatic offsetting failure (`automatic_payment_offsetting_out`), transactional non-persistence, `:20:` bank header, process lifecycle | `Backend/Payment_Mass_Import_Offsetting_Failure_PDT_2713.md` | `Frontend/Payment_Mass_Import_Offsetting_Failure_PDT_2713.md` | PDT-2713 |
| Contract version validity — Mass Email, Mass SMS, Penalty (description-vs-code gaps; PDT-2599 attachment) | `Backend/Version_Validity_Three_Processes_PDT_2815.md` | `Frontend/Version_Validity_Three_Processes_PDT_2815.md` | PDT-2815 |
| Service Contract first version startDate re-alignment — auto-realign version 1 startDate to signingDate when signingDate < startDate during finalization; guard checks, version ordering, downstream impact | `Backend/Service_Contract_First_Version_StartDate_PDT_2846.md` | `Frontend/Service_Contract_First_Version_StartDate_PDT_2846.md` | PDT-2846 |
| Minimal interim payment — create interim invoice only when total incl. VAT ≥ 5 EUR (standard + manual paths; backend only) | `Backend/PDT_2872_minimal_interim_payment.md` | — | PDT-2872 |
| Product Contract creation — happy path, POD linkage, validation scenarios | `Backend/Product_contract_creation.md` | — | N/A (General) |
| Single email to multiple recipients — Email Communication (one mass-comm send, shared `task_id`) | `Backend/Single_email_multiple_recipients_PDT_2881.md` | — | PDT-2881 |
| Payment Partner export TXT — external documents, LPF logical date, rescheduling layout (423-char fixed width) | `Backend/Payment_Partner_Export_PDT_2187.md` | — | PDT-2187 |
| Skip RiskList permission — product contract mass import (create, edit same version, edit new version; all succeed with permission) | `Backend/Skip_Risklist_Product_Contract_Mass_Import_PDT_2931.md` | — | PDT-2931 |
| Sales Portal PUT update existing contract — private / private-with-business customers (legal entity TC-BE-22 only); field omission semantics, direct debit, proxy/manager, customer swap; different product on contract allowed; product swap version mismatch; third-tab productParameters; price-component restriction; fixed IAP accepted; add second POD; customer local/foreign address; contract status transitions (ENTERED_INTO_FORCE, ACTIVE_IN_TERM, TERMINATED); KYC; validation errors | `Backend/Put_Update_Existing_Contract_PHN2130.md` | — | PHN-2130 |

## Layout reference

See `.cursor/rules/workspace/test_cases_structure.mdc` for the full two-folder layout rules.
