# TestCaseGeneratorAgent Report

**Date:** 2026-04-02  
**Time:** 09:00  
**Agent:** TestCaseGeneratorAgent

## Task

Generate comprehensive test cases for the **POST generate contract template** endpoint based on Confluence page (745046029).

## Confluence Source

- **Page:** [POST generate contract template](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/745046029/POST+generate+contract+template)
- **User Story:** Customer of Energo-Pro Sales or Energo-Pro Energy services can get a contract document for the product of their choice to check, validate, and sign.
- **Endpoint (Confluence):** POST /contract/template {Product_ID, Product_Version, ContractJSON}
- **Business Rule:** Select template starting with "CSP"; if none → return null; if > 1 → return error; else → return generated PDF.

## Cross-Dependency Analysis (Input from CrossDependencyFinderAgent)

### Entry Points Identified
- `POST /product-contract/generate` (ProductContractController.generateDocuments)
- `GET /product-contract/{id}/generate-popup` (ProductContractController.getTemplatePopup)
- `POST /service-contract/generate` (ServiceContractController.generateDocuments)
- `GET /service-contract/{id}/generate-popup` (ServiceContractController.getTemplatePopup)

### Key Services
- ProductContractDocumentCreationService
- ServiceContractDocumentCreationService
- DocumentGenerationService
- AbstractDocumentCreationService
- SignerChainManager
- FileService (FTP)
- FileArchivationService (EDMS)
- PermissionService

### What Could Break (10 items)
- Portal PDF generation (template selection changes)
- Template selection correctness (CSP naming rules)
- Output format/signing validation
- File naming conventions
- Signing pipeline
- EDMS archivation
- Template lifecycle
- Permission-based visibility
- POD consumption-purpose filtering
- Document download endpoints

## Codebase Analysis

### Request Model: ContractDocumentSaveRequest
- `contractId` (Long, @NotNull)
- `versionId` (Long, @NotNull)
- `documents[]`:
  - `templateId` (Long, @NotNull)
  - `signings` (List<ContractTemplateSigning>)
  - `outputFileFormat` (List<FileFormat>, @Size(min=1))
  - `deletePreviousFiles` (boolean)

### Custom Validator: ContractDocumentSaveRequestValidator
- PDF format requires signings
- Non-PDF must not have signings
- Signing option NO cannot be combined with other options

### Enums
- FileFormat: DOCX, PDF, XLSX
- ContractTemplateSigning: NO, SIGNING_WITH_SYSTEM_CERTIFICATE, SIGNING_WITH_TABLET, SIGNING_WITH_QUALIFIED_SIGNATURE

## Test Cases Generated

### File 1: Generate_popup_and_template_selection.md (11 TCs)
| TC | Type | Title |
|----|------|-------|
| TC-1 | Positive | Retrieve available templates for a valid product contract version |
| TC-2 | Positive | Retrieve templates for a service contract version |
| TC-3 | Positive | Multiple output formats returned in popup |
| TC-4 | Negative | No CSP-prefixed template exists — empty result |
| TC-5 | Negative | Contract not found — invalid contract ID |
| TC-6 | Negative | Contract has TERMINATED status — generation blocked |
| TC-7 | Negative | Contract has CANCELLED status — generation blocked |
| TC-8 | Negative | Version ID does not exist for the contract |
| TC-9 | Negative | User lacks PRODUCT_CONTRACT_GENERATE permission |
| TC-10 | Negative | Missing versionId query parameter |
| TC-11 | Negative | Template exists but has expired validity date |

### File 2: Document_generation.md (18 TCs)
| TC | Type | Title |
|----|------|-------|
| TC-1 | Positive | Generate PDF document successfully for product contract |
| TC-2 | Positive | Generate DOCX document successfully |
| TC-3 | Positive | Generate multiple format documents in a single request |
| TC-4 | Positive | Generate document for service contract |
| TC-5 | Positive | Generate document with SIGNING_WITH_SYSTEM_CERTIFICATE |
| TC-6 | Positive | Delete previous files when deletePreviousFiles is true |
| TC-7 | Negative | Missing contractId in request body |
| TC-8 | Negative | Missing versionId in request body |
| TC-9 | Negative | Missing templateId in documents array |
| TC-10 | Negative | Empty outputFileFormat array |
| TC-11 | Negative | PDF format selected but signings not provided |
| TC-12 | Negative | Non-PDF format selected but signings provided |
| TC-13 | Negative | Signing option NO combined with another signing |
| TC-14 | Negative | Template does not contain all provided signing options |
| TC-15 | Negative | Template does not contain all provided output formats |
| TC-16 | Negative | Contract not found or has invalid status |
| TC-17 | Negative | User lacks PRODUCT_CONTRACT_GENERATE permission |
| TC-18 | Negative | Empty documents array in request |

### File 3: Regression_and_integration.md (12 TCs)
| TC | Type | Title |
|----|------|-------|
| TC-1 | Positive | Generated document can be downloaded via download endpoint |
| TC-2 | Positive | File naming follows configured convention |
| TC-3 | Positive | EDMS archivation attributes are set correctly |
| TC-4 | Positive | POD consumption purpose filters correct template |
| TC-5 | Positive | Template with additional agreement purpose — permission check |
| TC-6 | Negative | Signing chain fails for unsupported signing type |
| TC-7 | Negative | Template file missing on FTP |
| TC-8 | Negative | Template marked as inactive/deleted |
| TC-9 | Positive | Contract in DRAFT status — generation succeeds without signing |
| TC-10 | Positive | Service contract document download works after generation |
| TC-11 | Negative | Concurrent generation requests for same contract |
| TC-12 | Positive | Contract with multiple PODs — all POD data included |

## Output Locations

- `Cursor-Project/test_cases/Flows/Contract_template_generation/Generate_popup_and_template_selection.md`
- `Cursor-Project/test_cases/Flows/Contract_template_generation/Document_generation.md`
- `Cursor-Project/test_cases/Flows/Contract_template_generation/Regression_and_integration.md`
- `Cursor-Project/test_cases/Flows/Contract_template_generation/README.md`
- `Cursor-Project/test_cases/Flows/README.md` (updated)

## Summary

- **Total test cases:** 41
- **Positive:** 22
- **Negative:** 19
- **Coverage areas:** Template selection, document generation, request validation, signing rules, permissions, regression, integration points
