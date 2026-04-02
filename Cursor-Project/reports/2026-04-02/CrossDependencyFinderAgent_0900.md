# CrossDependencyFinderAgent Report

**Date:** 2026-04-02  
**Time:** 09:00  
**Agent:** CrossDependencyFinderAgent

## Task

Analyse cross-dependencies for the **POST /contract/template** endpoint (Confluence page 745046029) to identify upstream/downstream dependencies, integration points, and what could break.

## Scope

Contract document generation from Phoenix templates for product and service contracts. The Confluence story describes a POST endpoint that receives Product_ID, Product_Version, and ContractJSON, selects a "CSP"-prefixed template, and returns a generated PDF.

## Entry Points

| Entry Point | Method |
|-------------|--------|
| `POST /product-contract/generate` | ProductContractController.generateDocuments |
| `GET /product-contract/{id}/generate-popup` | ProductContractController.getTemplatePopup |
| `POST /service-contract/generate` | ServiceContractController.generateDocuments |
| `GET /service-contract/{id}/generate-popup` | ServiceContractController.getTemplatePopup |
| Angular UI: ProductContractService.templateGenerate | POST to `/{url}/generate` |
| Angular UI: ProductContractService.getTemplateParametersList | GET to `/{url}/{id}/generate-popup` |

## Upstream Dependencies (17)

### Services
- ProductContractDocumentCreationService
- ServiceContractDocumentCreationService
- DocumentGenerationService (DOCX → PDF/DOCX/XLSX)
- DocumentGenerationUtil (FTP template download, path resolution)
- AbstractDocumentCreationService (base class)
- TemplateService / TemplateController
- SignerChainManager (signing pipeline)
- FileService (FTP I/O)
- FileArchivationService + EDMSAttributeProperties
- ContractDocumentTranslationUtil
- PermissionService

### Database Repositories
- ProductTemplateRepository / ServiceTemplateRepository
- ContractTemplateRepository / ContractTemplateDetailsRepository / ContractTemplateFileRepository
- ProductContractRepository / ProductContractDetailsRepository
- ServiceContractsRepository / ServiceContractDetailsRepository
- ContractPodRepository / ServiceContractPodsRepository
- ProductPriceComponentRepository / ServicePriceComponentRepository
- CustomerRepository / CustomerDetailsRepository / LegalFormRepository
- DocumentsRepository
- ProductContractSignableDocumentRepository / ServiceContractSignableDocumentsRepository
- CompanyDetailRepository / CompanyLogoRepository

## Downstream Dependencies (7)

- Phoenix Web UI (Product contracts)
- Phoenix Web UI (Service contracts)
- Sales Portal / Energo-Pro portals
- Signing subsystem (QES/Tablet via SignerChainManager)
- EDMS / archivation backend
- Email job services (ProductContractEmailJobService, ServiceContractEmailJobService)
- Download endpoints (product-contract/download-file, service-contract/download-contract-file)

## What Could Break (10 items)

1. Portal PDF generation — template selection or request shape changes
2. Template selection correctness — CSP naming and filtering queries
3. Output format/signing validation — invalid combinations previously allowed
4. File naming conventions — EDMS indexing and operational procedures
5. Signing pipeline — incorrect signer computation or configuration
6. EDMS archivation — metadata and audit trail
7. Template lifecycle — status handling marks valid templates unusable
8. Permission-based template visibility — blocking authorised users
9. POD consumption-purpose filtering — wrong template for consumption type
10. Document download endpoints — wrong paths or formats

## Technical Details

Internally, the Confluence POST /contract/template maps to `POST /product-contract/generate` and `POST /service-contract/generate`. Template selection uses `ProductTemplateRepository.findTemplatesForContractDocumentGeneration()` with product detail, contract version, customer type, and POD consumption purpose. Document generation uses `DocumentGenerationService` with DOCX templates merged with `ContractDocumentModel`. Files stored on FTP at `{ftp.server.base.path}/{productContractDocumentFtpPath}/{date}/`. Request model: `ContractDocumentSaveRequest` with custom `ContractDocumentSaveRequestValidator`.

## Output

Structured cross-dependency data was passed to TestCaseGeneratorAgent as `context['cross_dependency_data']` for test case generation.
