# TestCaseGeneratorAgent Report — 2026-04-03 17:00

## Task
Generate test cases for PDT-2708: "The system doesn't restrict creation of the Correction data by scales with a difference in the header period with the original data."

## Workflow
1. **CrossDependencyFinderAgent** analyzed the codebase for billing-data-by-scales (controller, service, entity, request DTO, custom validators, UI components).
2. **PhoenixExpert** consultation: identified the bug location in `BillingByScalesService.create()` line 80 — correction flag skips overlap check but no header period equality validation exists.
3. **Playwright instructions** read: `project-description.md`, `general-rules.md`, `test-writing-rules.instructions.md`, `SKILL.md`.
4. **Test Case Template** followed: `Cursor-Project/config/template/Test_case_template.md`.

## Cross-Dependency Findings
- **Scope:** `POST /billing-by-scales` (create), `PUT /billing-by-scales` (edit), `GET /billing-by-scales/{id}` (view)
- **Bug location:** `BillingByScalesService.create()` line 80 — `if (!(request.getCorrection() || request.getOverride()))` skips `checkBillingByScalesWithPodPeriodFromAndPeriodTo()` but does NOT validate header period equality with original.
- **Missing validation:** No code fetches the original record to compare dateFrom/dateTo when creating a correction.
- **Entities:** `BillingByScale` (pod.billing_by_scale), `BillingDataByScale` (pod.billing_data_by_scale).
- **Validators:** `ValidBillingByScalesPeriod` (range/max 1 year), `ValidInvoiceCorrection` (correction/override/invoiceCorrection), `ValidPeriods`.
- **What could break:** Billing runs, invoice calculations, energy data consistency, reports.

## Output
- **Backend:** `Cursor-Project/test_cases/Backend/Correction_data_by_scales_header_period.md` — 20 test cases (TC-BE-1 through TC-BE-20)
- **Frontend:** `Cursor-Project/test_cases/Frontend/Correction_data_by_scales_header_period.md` — 10 test cases (TC-FE-1 through TC-FE-10)
- **READMEs updated:** `test_cases/README.md`, `test_cases/Backend/README.md`, `test_cases/Frontend/README.md`

## Test Case Summary

### Backend (20 TCs)
| TC | Type | Title |
|----|------|-------|
| TC-BE-1 | Negative | Reject correction with different dateFrom than original |
| TC-BE-2 | Negative | Reject correction with different dateTo than original |
| TC-BE-3 | Negative | Reject correction when both dateFrom and dateTo differ |
| TC-BE-4 | Positive | Successfully create correction with matching header period |
| TC-BE-5 | Positive | Create original (non-correction) billing data by scales |
| TC-BE-6 | Negative | Reject correction with dateFrom one day earlier |
| TC-BE-7 | Negative | Reject correction with dateTo one day later |
| TC-BE-8 | Negative | Reject correction with swapped dateFrom and dateTo |
| TC-BE-9 | Positive | Create correction with override=true and matching period |
| TC-BE-10 | Negative | Reject correction with override=true and mismatched period |
| TC-BE-11 | Negative | Reject correction when no original data exists |
| TC-BE-12 | Negative | Reject correction when invoiceCorrection is blank |
| TC-BE-13 | Negative | Reject override=true with correction=false |
| TC-BE-14 | Negative | Reject dateFrom after dateTo |
| TC-BE-15 | Negative | Reject period exceeding one year |
| TC-BE-16 | Positive | View correction confirms matching header period |
| TC-BE-17 | Negative | Reject correction with missing dateFrom |
| TC-BE-18 | Negative | Reject correction with missing dateTo |
| TC-BE-19 | Negative | Reject non-correction with overlapping period for same POD |
| TC-BE-20 | Positive | Create multiple corrections with matching period |

### Frontend (10 TCs)
| TC | Type | Title |
|----|------|-------|
| TC-FE-1 | Negative | UI allows mismatched Date from — should warn or block |
| TC-FE-2 | Negative | UI allows mismatched Date to — should warn or block |
| TC-FE-3 | Positive | Successfully create correction with matching period via UI |
| TC-FE-4 | Negative | Correction checkbox requires Invoice Correction field |
| TC-FE-5 | Positive | Correction enables/disables Override toggle |
| TC-FE-6 | Positive | Preview displays correction with correct header period |
| TC-FE-7 | Negative | Error message displayed when backend rejects mismatch |
| TC-FE-8 | Positive | Number of days recalculates on date changes |
| TC-FE-9 | Negative | Correction allows negative values in specific fields |
| TC-FE-10 | Negative | Date range beyond one year shows validation error |

## Agents Involved
- CrossDependencyFinderAgent
- TestCaseGeneratorAgent
- PhoenixExpert
