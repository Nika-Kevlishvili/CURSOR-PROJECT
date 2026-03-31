# AI Agent Instructions for Playwright-automationTS

## Project Overview

This is an **API test automation framework** for an energy management system (Energo-Pro) using Playwright's API testing capabilities. Tests are organized by business domain (billing, customers, contracts, receivables, etc.) with a fixture system for test data management.

### Three-Phase Test Lifecycle

1. **Setup project** (`global-setup.ts`): Authenticates (main API + Sales Portal), generates nomenclatures and document templates, stores in `fixtures/envVariables.json` + `fixtures/token.json` + `fixtures/salesPortalToken.json`
2. **Main project**: Test execution with parallel workers
3. **Send report project** (`global-teardown.ts`): Aggregates results, sends to Slack/Jira

### Running Tests

```bash
npx playwright test                          # Full pipeline (setup → tests → reporting)
npx playwright test --grep "@billing"        # Specific domain by tag
npx playwright test --grep "\bREG-706\b"     # Single test by Jira ID
npx playwright test --project=setup --workers=1  # Setup only
```

### Key Fixtures

- `Request`: `RequestWrapper` around `APIRequestContext` — auto-injects Bearer token and attaches `_requestMetadata` to responses. Use `Request.raw` for the underlying context
- `SPRequest`: Same as `Request` but for Sales Portal API (separate base URL + OAuth2 token)
- `FileUploadRequest`: Raw `APIRequestContext` for multipart/form-data uploads (no JSON content-type)
- `GeneratePayload`: Access to all 11 domain payload generators
- `Responses`: Stateful `ResponsesContainer` — stores created entity IDs/data across test steps
- `Endpoints`: String constants for all API paths (typed as `EndpointsType`)
- `Nomenclatures`: Get-or-create methods for system reference data (account manager types, currencies, etc.)
- `OnlinePaymentUrl`: Resolved online payment URL string
- `saveResponsesToFile` / `loadResponsesFromFile` / `clearStashedResponses`: Persist/load `Responses` to `fixtures/stashedResponses/`

### Integration Points

**Authentication**: `tokenAuth()` from `fixtures/login.ts` (JWT) + `salesPortalTokenAuth()` from `fixtures/salesPortalLogin.ts` (OAuth2). Tokens read at runtime by `getToken()` / `getSPToken()` from `fixtures/utils/auth.ts`.

**CI/CD**: 9 GitHub Actions workflows — `main.yml` (GitLab pipeline), `jiraSingle.yml`, `jiraRegression.yml`, `jiraRegression2.yml`, `extension.yml`, `dev-regression.yml`, `dev-fix-regression.yml`, `dev2-regression.yml`, `test-regression.yml`.

**Reporting**: `reportGenerator` facade delegates to `ReportParser`, `SlackReporter`, `JiraReporter`, `ResponseLinker` in `utils/reporters/`.

**Environment Variables**: `BASE_URL`, `DEVAUTHAPI`, `TESTAUTHAPI`, `PORTAL_USER`, `PASSWORD`, `GRANT_TYPE`, `CLIENT_ID`, `CLIENT_SECRET`, `SAVE_OBJECT_LINKS`, `JIRA_EMAIL`, `JIRA_API_TOKEN`, `SLACK_API_TOKEN`.

## File Structure Reference

- `fixtures/baseFixture.ts` — Core fixture definitions, custom matchers, `baseFixture` type
- `fixtures/constants/endpoints.ts` — All API endpoint constants (`Endpoints` object)
- `fixtures/types/responses.ts` — `ResponsesContainer` interface and factory
- `fixtures/matchers/checkResponse.ts` — Custom `CheckResponse` expect matcher
- `fixtures/utils/auth.ts` — `getToken()` / `getSPToken()`
- `fixtures/utils/baseUrl.ts` — `normalizeBaseURL()` helper
- `fixtures/utils/stashResponses.ts` — Save/load/clear responses to JSON files
- `fixtures/login.ts` — `tokenAuth()` for main API JWT
- `fixtures/salesPortalLogin.ts` — `salesPortalTokenAuth()` for Sales Portal OAuth2
- `fixtures/envCashed.ts` — Cached loader for `envVariables.json`
- `utils/RequestWrapper.ts` — `RequestWrapper` class wrapping `APIRequestContext` with metadata
- `utils/generateReport.ts` — Report facade delegating to `utils/reporters/`
- `utils/reporters/` — `ReportParser`, `SlackReporter`, `JiraReporter`, `ResponseLinker`
- `utils/randomGens.ts` — Random data generators for test payloads
- `utils/onlinePaymentUrls.ts` — Online payment URL resolution
- `jsons/payloadGenerators/PayloadGenerator.ts` — Main `GeneratePayload` aggregating all domains
- `jsons/payloadGenerators/domains/` — 11 domain generators: `BillingPayloads`, `ContractsAndOrdersPayloads`, `CustomerCommunicationPayloads`, `CustomerPayloads`, `EnergyDataPayloads`, `MasterDataPayloads`, `OperationsManagementPayloads`, `PointsOfDeliveryPayloads`, `ProductAndServicesPayloads`, `ReceivablesManagementPayloads`, `salesPortalPayloads`
- `jsons/payloads/` — Raw payload templates organized by domain
- `jsons/payloads/create/nomenclatures/` — `nomenclatures.ts`, `baseNomenclature.ts`, `templates.ts`
- `mass-imports/generators/` — `massImportGenerator.ts` + `domains/` (customer, product contract, price parameter)
- `tests/setup/` — `global-setup.ts`, `global-teardown.ts`
- `tests/` — Domains: `billing/`, `contractsAndOrders/`, `customers/`, `customerComm/`, `receivableManagement/`, `productAndServices/`, `operationsManagement/`, `massImports/`, `salesPortal/`, `bigData/`, `miscellaneous/`
- `validations/` — Response validation helpers (e.g., `billingValidator.ts`)
- `.github/workflows/` — 9 CI/CD pipelines
- `.github/scripts/` — Shell scripts for GitLab integration
