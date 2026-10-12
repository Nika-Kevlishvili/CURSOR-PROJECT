# AI Agent Instructions for Playwright-automationTS

## Project Overview

This is an **API test automation framework** for an energy management system (Energo-Pro) using Playwright's API testing capabilities. Tests are organized by business domain (billing, customers, contracts, receivables, etc.) with a fixture system for test data management.

### Three-Phase Test Lifecycle

1. **Setup project** (`src/tests/setup/global-setup.ts`): Authenticates (main API + Sales Portal), generates nomenclatures and document templates, stores in `src/backend/fixtures/envVariables.json` + `src/backend/fixtures/token.json` + `src/backend/fixtures/salesPortalToken.json` and `src/frontend/fixtures/frontendEnvVariables.json`
2. **Main project**: Test execution with parallel workers
3. **Send report project** (`src/tests/setup/global-teardown.ts`): Aggregates results, sends to Slack/Jira

### Running Tests

```bash
npx playwright test                          # Full pipeline (setup → src/tests → reporting)
npx playwright test --grep "@billing"        # Specific domain by tag
npx playwright test src/tests/cursor/FILE.spec.ts
npx playwright test --grep "\bREG-706\b"     # Single test by Jira ID
npx playwright test --project=setup --workers=1  # Setup only
npx playwright test --project=frontend src/frontend/customerCreateBasic.spec.ts
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
- `saveResponsesToFile` / `loadResponsesFromFile` / `clearStashedResponses`: Persist/load `Responses` to `src/backend/fixtures/stashedResponses/`

### Integration Points

**Authentication**: `tokenAuth()` from `src/backend/fixtures/login.ts` (JWT) + `salesPortalTokenAuth()` from `src/backend/fixtures/salesPortalLogin.ts` (OAuth2). Tokens read at runtime by `getToken()` / `getSPToken()` from `src/backend/fixtures/utils/auth.ts`.

**Reporting**: `reportGenerator` → `src/backend/utils/reporters/` (`ResponseLinker`, `JiraReporter`, `SlackReporter`, `ReportParser`, `StatusMapStore`). Playwright writes `src/backend/playwright-report.json` and HTML under `src/backend/playwright-report/`. **New `src/tests/cursor/` specs**: import `./cursor-test.fixtures` (`TestRunSummary` + auto `[API responses]` log) and end each test with `finalizeTestRunSummary` from `src/tests/cursor/shared/manual-verification-links.fixtures.ts`.

**Environment Variables**: `BASE_URL`, `DEVAUTHAPI`, `TESTAUTHAPI`, `PORTAL_USER`, `PASSWORD`, `GRANT_TYPE`, `CLIENT_ID`, `CLIENT_SECRET`, `SAVE_OBJECT_LINKS`, `JIRA_EMAIL`, `JIRA_API_TOKEN`, `SLACK_API_TOKEN`.

`playwright.config.ts` `testDir` is `./src/tests`. Default `baseURL` is `devUrl` unless `BASE_URL` is set. The `frontend` project uses `testDir: './src/frontend'`.

## File Structure Reference

- `src/backend/fixtures/baseFixture.ts` — Core fixture definitions, custom matchers, `baseFixture` type. Exposes `Request`, `SPRequest`, `FileUploadRequest`, `GeneratePayload`, `MassImportGenerator`, `Responses`, `Endpoints`, `Nomenclatures`, validators
- `src/tests/cursor/cursor-test.fixtures.ts` — Re-exports `baseFixture` + `TestRunSummary` + global `afterEach` for API response logging. **New specs under `src/tests/cursor/` import `{ test, expect }` from `./cursor-test.fixtures`**
- `src/backend/fixtures/constants/endpoints.ts` — All API endpoint constants (`Endpoints` object)
- `src/backend/fixtures/types/responses.ts` — `ResponsesContainer` interface and factory
- `src/backend/fixtures/matchers/checkResponse.ts` — Custom `CheckResponse` expect matcher
- `src/backend/fixtures/utils/auth.ts` — `getToken()` / `getSPToken()`
- `src/backend/fixtures/utils/baseUrl.ts` — `normalizeBaseURL()` helper
- `src/backend/fixtures/utils/stashResponses.ts` — Save/load/clear responses to JSON files
- `src/backend/fixtures/login.ts` — `tokenAuth()` for main API JWT
- `src/backend/fixtures/salesPortalLogin.ts` — `salesPortalTokenAuth()` for Sales Portal OAuth2
- `src/backend/fixtures/envCashed.ts` — Cached loader for `envVariables.json`
- `src/backend/utils/RequestWrapper.ts` — `RequestWrapper` class wrapping `APIRequestContext` with metadata
- `src/backend/utils/generateReport.ts` — Report facade delegating to `src/backend/utils/reporters/`
- `src/backend/utils/reporters/` — `ReportParser`, `SlackReporter`, `JiraReporter`, `ResponseLinker`, `StatusMapStore`
- `src/backend/utils/randomGens.ts` — Random data generators for test payloads
- `src/backend/utils/onlinePaymentUrls.ts` — Online payment URL resolution
- `src/backend/jsons/payloadGenerators/PayloadGenerator.ts` — Main `GeneratePayload` aggregating all domains
- `src/backend/jsons/payloadGenerators/domains/` — 11 domain generators: `BillingPayloads`, `ContractsAndOrdersPayloads`, `CustomerCommunicationPayloads`, `CustomerPayloads`, `EnergyDataPayloads`, `MasterDataPayloads`, `OperationsManagementPayloads`, `PointsOfDeliveryPayloads`, `ProductAndServicesPayloads`, `ReceivablesManagementPayloads`, `salesPortalPayloads`
- `src/backend/jsons/payloads/` — Payload templates by domain. There is no `payloads/create/` folder
- `src/backend/jsons/payloads/nomenclatures/` — `nomenclatures.ts`, `baseNomenclature.ts`, `templates.ts`
- `src/backend/mass-imports/generators/` — `massImportGenerator.ts` + `domains/` (`customerMP`, `productContractMP`, `paymentMP`, `supplyActionDeactivationMP`; `priceParameterMP.ts` is present and not constructed on the generator class)
- `src/tests/setup/` — `global-setup.ts`, `global-teardown.ts`, `jira-update.ts`
- `src/tests/cursor/shared/test-run-summary.fixtures.ts` — payloads, expected/actual, filtered portal links
- `src/tests/cursor/shared/manual-verification-links.fixtures.ts` — link builders + `finalizeTestRunSummary` export
- `src/tests/` — Domains: `billing/`, `contractsAndOrders/`, `customers/`, `customerComm/`, `receivableManagement/`, `productAndServices/`, `operationsManagement/`, `massImports/`, `pointOfDelivery/`, `bigData/`, `cursor/`
- `src/frontend/` — UI specs and page objects. Project name in config: `frontend`
- `src/backend/validations/` — Response validation helpers (`billingValidator.ts`, `receivableValidations.ts`)
- `tests/regression/` — Visual regression suite with its own `playwright.regression.config.ts`. Not picked up by the main `testDir`
- Repo-root `mass-imports/` and `scripts/` — local export/helper scripts, not the generator library

### Import prefix rule

- Specs under `src/tests/cursor/` and `src/tests/<domain>/` import support code through `backend/`: `../../backend/fixtures/baseFixture`. Add one `../` for each extra directory (`src/tests/cursor/shared/`, `src/tests/billing/<sub>/` → `../../../backend/...`).
- Files already inside `src/backend/**` import sibling folders without another `backend/` segment (`../../fixtures/baseFixture` from `src/backend/jsons/payloadGenerators/`).
- `src/frontend/**` local imports (`../utils`, `../fixtures`, `../pom`) stay inside `src/frontend`. API helpers from a frontend file use `../../backend/...` or `../backend/...` depending on depth.
