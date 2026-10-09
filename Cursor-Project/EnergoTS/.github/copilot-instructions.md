# AI Agent Instructions for Playwright-automationTS

## Project Architecture

This is an **API test automation framework** for an energy management system (Energo-Pro) using Playwright's API testing capabilities. Tests are organized by business domain (billing, customers, contracts, receivables, etc.) with a sophisticated fixture system for test data management.

### Core Design Patterns

**Domain-Driven Test Organization**: Tests mirror business domains in `tests/` with corresponding payload generators in `jsons/payloadGenerators/domains/`. Each domain (e.g., `BillingPayloads`, `CustomerPayloads`) encapsulates API request construction logic.

**Stateful Response Management**: The `Responses` fixture acts as a shared state container across test steps. Created entities are pushed to domain-specific arrays (e.g., `Responses.customer.push(customerId)`), enabling cross-domain dependencies:

```typescript
// Customer created in one step
Responses.customer.push(await customerResponse.json());

// Referenced in another domain's payload
payload.customerId = this.responses.customer[0].id;
```

**Three-Phase Test Lifecycle**:
1. **Setup project** (`global-setup.ts`): Authenticates, generates nomenclatures (system reference data), and stores in `fixtures/env_variables.json` + `fixtures/token.json`
2. **Main project**: Actual test execution with parallel workers
3. **Send report project** (`global-teardown.ts`): Aggregates results, sends to Slack/Jira

## Critical Workflows

### Running Tests

```bash
# Full pipeline (setup → tests → reporting)
npx playwright test

# Specific domain by tag
npx playwright test --grep "@billing"

# Single test by Jira ID
npx playwright test --grep "\bREG-706\b"

# Setup only (refreshes nomenclatures and auth token)
npx playwright test --project=setup --workers=1
```

### Custom Expect Matcher Pattern

**Always use `.CheckResponse()` for API assertions**, not `.ok()` or manual checks:

```typescript
// ✅ CORRECT - auto-extracts [REG-XXX] from test title, formats error with payload/response
await expect(response).CheckResponse();

// ❌ WRONG - no error context, just boolean
expect(response.ok()).toBeTruthy();
```

The matcher **automatically extracts** `[REG-XXX]` IDs from test titles for error reporting. It formats failure messages with:
- Endpoint and HTTP method
- Request payload (JSON formatted)
- Response status code
- Response body (JSON formatted)

**Response bodies can only be read once** - the matcher handles this internally, so never call `response.json()` before the assertion.

### Fixture Usage Pattern

Import from `baseFixture.ts`, destructure needed fixtures:

```typescript
import { test, expect } from '../../fixtures/baseFixture';

test('[REG-XXX]: Test name', async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures }) => {
  await test.step('Create entity', async () => {
    const payload = GeneratePayload.customers.customer_legal();
    const response = await Request.post(Endpoints.customer, { data: payload });
    await expect(response).CheckResponse();
    Responses.customer.push(await response.json());
  });
});
```

**Key Fixtures**:
- `Request`: Authenticated API context (auto-includes Bearer token from `token.json`)
- `FileUploadRequest`: Same but for multipart/form-data uploads
- `GeneratePayload`: Access to all domain payload generators
- `Responses`: Stateful storage for created entity IDs/data
- `Endpoints`: String constants for all API paths
- `Nomenclatures`: Methods to fetch/create reference data (profiles, currencies, etc.)

## Project-Specific Conventions

### Test Naming & Tagging

All tests MUST include Jira ticket IDs in square brackets for tracking:

```typescript
test.describe('[REG-55]: Billing', { tag: '@billing' }, () => {
  test('[REG-706]: Manual invoice reversal | happy pass', async ({...}) => {
```

Tags enable CI/CD filtering - the GitHub workflow maps changed controller files to test tags (see `.github/workflows/main.yml`).

### Payload Generator Architecture

Payload generators in `jsons/payloadGenerators/domains/` are **domain-segregated classes** that handle complex object linking logic. When creating high-level entities like billing runs, contracts, or orders, the generator automatically pulls prerequisite entity IDs from `this.responses` and links them properly:

```typescript
// In BillingPayloads - creating billing run requires multiple linked entities
public async manualInvoice() {
  const payload = manualInvoice(); // Base template
  
  // Generator automatically links previously created entities:
  payload.customerId = this.responses.customer[0].id;
  payload.productContractId = this.responses.productContract[0].id;
  payload.priceComponentId = this.responses.priceComponent[0].id;
  // ... etc
  
  return payload;
}
```

**Critical Pattern**: Tests must create entities in **dependency order** before calling higher-level payload generators. Example for billing run:
1. Create customer → `Responses.customer.push()`
2. Create product → `Responses.product.push()`
3. Create term → `Responses.terms.push()`
4. Create product contract (links customer, product, term) → `Responses.productContract.push()`
5. Create billing run (links contract) → `Responses.billingRun.push()`

Each payload generator method knows which entities it needs from `this.responses` arrays.

### Nomenclatures System

Nomenclatures are **system reference data** (currencies, statuses, profiles) cached during setup. Access via `Nomenclatures` fixture:

```typescript
const profile = await Nomenclatures.profiles('Gio'); // Fetches or creates
payload.profileId = profile;
```

All nomenclatures are stored in `fixtures/env_variables.json` during `global-setup.ts` to avoid redundant API calls.

### Mass Import Pattern

Excel-based imports use `MassImportGenerator` fixture:

```typescript
const { payload, templateName } = MassImportGenerator.CustomerMassImportGenerator.privateCustomerMP();
const filePath = await MassImportGenerator.uploadAndExecuteMassImport(payload, templateName);
```

The generator populates Excel templates by cell reference, uploads via `FileUploadRequest`, and monitors job completion.

### Report Attachment Convention

Always attach response data to test info for debugging:

```typescript
test.info().attach('[REG-706] response', {
  body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
  contentType: 'application/json'
});
```

The `reportGenerator.setLinksToResponses()` method converts entity IDs to clickable links (requires `SAVE_OBJECT_LINKS=true` in env).

## Integration Points

**Authentication**: Global setup calls `tokenAuth()` from `fixtures/login.ts`, stores JWT in `fixtures/token.json`. All `Request` contexts auto-inject this token via `Authorization: Bearer ${token}`.

**CI/CD Workflows**: Three GitHub Actions workflows for different scenarios:
- `main.yml`: Tag-based execution - maps changed controller files to test tags (e.g., `billing` controller → `@billing` tag)
- `jira.yml`: Single test execution by Jira ID (e.g., `REG-706`)
- `extension.yml`: Single test with optional object link generation

**Jira/Slack Integration**: Global teardown (`global-teardown.ts`) sends test results:
- Slack: Always active, uploads HTML report + test statistics
- Jira: Separate project (WIP) - updates ticket statuses based on test pass/fail

**Environment Variables**: 
- `BASE_URL`: API base URL (default: `http://10.236.20.11:8091/`)
- `AUTHAPI`, `PORTAL_USER`, `PASSWORD`: Authentication credentials
- `SAVE_OBJECT_LINKS`: Enable clickable links in reports (used in `extension.yml` workflow)

**No Data Cleanup**: Tests assume a non-production environment where created entities don't need explicit deletion.

## Common Pitfalls

1. **Never call `response.json()` multiple times** - response bodies are streams, consumed on first read. Cache the result or let `CheckResponse()` handle it.

2. **Don't use hardcoded IDs in payloads** - always reference `Responses` arrays or `Nomenclatures` to ensure data consistency across test runs.

3. **Setup project MUST run with `--workers=1`** - parallel execution causes race conditions in token/nomenclature file writes.

4. **Use `test.step()` for granular failure reporting** - each step shows in HTML reports and helps pinpoint failures.

5. **Mass imports require `FileUploadRequest`** - don't use the standard `Request` fixture for file uploads (missing multipart content-type handling).

6. **Understand entity dependencies** - Before creating complex entities (billing runs, contracts), check the payload generator to see what prerequisite entities must be created first and added to `Responses`.

## File Structure Reference

- `fixtures/baseFixture.ts` - Core fixture definitions, custom matchers
- `jsons/payloadGenerators/` - Domain payload generator classes
- `jsons/payloads/` - Raw payload templates
- `mass-imports/generators/` - Excel-based mass import logic
- `tests/setup/` - Global setup/teardown scripts
- `utils/generateReport.ts` - Report parsing and Slack/Jira integration
- `.github/workflows/` - CI/CD pipelines with tag-based filtering
