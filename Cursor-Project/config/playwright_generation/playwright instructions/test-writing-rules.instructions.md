---
description: "Rules and conventions for Playwright API test spec files. Use when creating, editing, or reviewing .spec.ts test files in the tests/ directory."
applyTo: "**/*.spec.ts"
---

# Test Writing Rules

## Reference Example

All test files follow the same architecture. Use `tests/contractsAndOrders/productContract.spec.ts` as the single reference example for structure, patterns, and conventions. Do **not** scan multiple test files to learn the pattern — one example is sufficient for any domain.

## Naming & Structure

- Every `test()` title MUST include a Jira ID in square brackets: `[REG-XXX]`
- Every `test.describe()` block MUST include a Jira ID and a `tag` for CI/CD filtering
- Use `test.step()` to wrap each logical operation — these appear in HTML reports

```typescript
test.describe('[REG-55]: Billing', { tag: '@billing' }, () => {
  test('[REG-706]: Invoice reversal | happy path', async ({ Request, ... }) => {
    await test.step('Create customer', async () => { ... });
    await test.step('Create billing run', async () => { ... });
  });
});
```

## Assertions

- **Always** use `await expect(response).CheckResponse()` — never `.ok()` or `.toBeTruthy()`
- **Never** call `response.json()` before `CheckResponse()` — the body stream can only be read once
- After `CheckResponse()` passes, `response.json()` is safe to call for storing data

## Imports

**New** `tests/cursor/*.spec.ts` (AI-authored):

```typescript
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
```

`cursor-test.fixtures` re-exports `baseFixture` and adds `afterEach`: console `[API responses]` + Playwright attach. No manual `reportGenerator` attach or local `test.afterEach` for API response logging. Existing specs that already use `../../fixtures/baseFixture` should not be migrated unless the user asks.

**Legacy / domain** specs under other `tests/` folders:

```typescript
import { test, expect } from '../../fixtures/baseFixture';
```

Do not import from `@playwright/test` directly.

## Data Handling

- **No hardcoded IDs** — use `Responses` arrays, `Nomenclatures`, or `envVariables` for all entity references
- **Push created entities** to `Responses` immediately after creation for downstream use
- **Create entities in dependency order** — check the payload generator class to see prerequisites
- Use `FileUploadRequest` (not `Request`) for multipart/form-data uploads

## CRITICAL: Precondition Data Creation Rule (MANDATORY)

**NEVER assume data already exists in the test environment.** Every test MUST create ALL required entities from scratch as part of preconditions.

### Why This Rule Exists

- Tests must be **self-contained** and **repeatable** — they should pass on any clean environment
- Relying on existing data causes **flaky tests** that fail when data is missing or different
- Every test must verify the complete data creation chain, not just the final operation

### Implementation Pattern: Helper Functions (PREFERRED)

**DO NOT use `test.beforeAll()`** — some fixtures (like `GeneratePayload`) may not be available there. Instead, use **helper functions** defined at the top of the file and call them within each test.

#### Step 1: Define Helper Functions for Shared Preconditions

```typescript
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';

// ═══════════════════════════════════════════════════════════════════════════
// SHARED PRECONDITION HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

async function sharedTerm(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.productAndServices.term();
    const response = await Request.post(Endpoints.terms, { data: payload });
    await expect(response).toBeOK();
    Responses.terms.push(await response.json());
}

async function sharedPrice(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.productAndServices.electricity();
    const response = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(response).toBeOK();
    Responses.priceComponent.push(await response.json());
}

async function sharedProduct(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.productAndServices.product();
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    const response = await Request.post(Endpoints.product, { data: payload });
    await expect(response).toBeOK();
    Responses.product.push(await response.json());
}
```

#### Step 2: Call Helper Functions in Each Test

```typescript
test('[JIRA-KEY]: TC-BE-1 – Happy path', async ({
    Request, GeneratePayload, Endpoints, Responses, TestRunSummary,
}) => {
    // Call shared helpers in a precondition step
    await test.step('Precondition: Create shared entities', async () => {
        await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
        await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
    });

    // Test using created data from Responses
    await test.step('Execute test', async () => {
        expect(Responses.product[0]).toBeTruthy();
        const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
        await expect(response).CheckResponse();
        const body = await response.json();
        const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
        expect(found).toBeTruthy();
    });

    await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'JIRA-KEY',
            relevantEntityKeys: ['product'],
        });
    });
});
```

#### Step 3: For Test-Specific Data, Create Inline

When a test needs **different data** (e.g., INACTIVE status), use shared helpers for dependencies and create the specific entity inline:

```typescript
test('[JIRA-KEY]: TC-BE-3 – Inactive product is excluded', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
}) => {
    let inactiveProductId: number;

    // Use shared helpers for common dependencies
    await test.step('Precondition: Create shared entities', async () => {
        await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
        await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
    });

    // Create test-specific entity
    await test.step('Create product with INACTIVE status', async () => {
        const payload = GeneratePayload.productAndServices.product();
        payload.contractTypes = ['SUPPLY_ONLY'];
        payload.paymentGuarantees = ['NO'];
        payload.productStatus = 'INACTIVE';  // ← Test-specific
        const response = await Request.post(Endpoints.product, { data: payload });
        await expect(response).CheckResponse();
        inactiveProductId = (await response.json()).id;
    });

    await test.step('Verify inactive product excluded from list', async () => {
        const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
        await expect(response).CheckResponse();
        const body = await response.json();
        const found = body.content?.find((p: any) => p.id === inactiveProductId);
        expect(found).toBeFalsy();
    });

    await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'JIRA-KEY',
            relevantEntityKeys: ['product'],
            snapshot: { inactiveProductId },
        });
    });
});
```

### FORBIDDEN Patterns

```typescript
// ❌ BAD: Using test.beforeAll() for preconditions (fixtures may not be available)
test.beforeAll(async ({ GeneratePayload }) => { ... }); // WRONG

// ❌ BAD: Assuming data exists
const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
const existingProduct = body.content[0]; // WRONG - don't rely on existing data

// ❌ BAD: Using hardcoded IDs
const productId = 1234; // WRONG - this product may not exist

// ❌ BAD: Storing IDs in describe-level variables
test.describe('...', () => {
    let productId: number; // WRONG - use Responses arrays instead
});
```

### REQUIRED Patterns

```typescript
// ✅ GOOD: Helper function for shared preconditions
async function sharedProduct(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.productAndServices.product();
    const response = await Request.post(Endpoints.product, { data: payload });
    await expect(response).toBeOK();
    Responses.product.push(await response.json());
}

// ✅ GOOD: Call helper in test
await test.step('Precondition: Create shared entities', async () => {
    await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
});

// ✅ GOOD: Use Responses arrays
expect(Responses.product[0]).toBeTruthy();

// ✅ GOOD: Test-specific data with local variable
let specificId: number;
await test.step('Create specific entity', async () => {
    specificId = (await response.json()).id;
});
```

### Data Creation Order (Follow Dependencies)

1. **Nomenclatures / Reference data** — use `envVariables` (already seeded)
2. **Terms** — `POST /terms` (required before products)
3. **Price components** — `POST /price-components` (required before products)
4. **Products** — `POST /products` (requires terms + price components)
5. **Customers** — `POST /customer`
6. **PODs** — `POST /pod`
7. **Contracts** — `POST /product-contract` (requires customer + POD + product)
8. **Billing runs** — `POST /billing-run` (requires contracts)
9. **Invoices** — Generated by billing run
10. **Payments** — `POST /payment` (requires invoice)

## Report Attachment (mandatory — test run summary)

**Every** `test()` in `tests/cursor/` ends with a unified test run summary:
test title, relevant payloads, expected vs actual results, filtered portal links.
Use `./cursor-test.fixtures` (includes `TestRunSummary` fixture + API response `afterEach`).

```typescript
import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProcessPreviewLink,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';

test('...', async ({ Responses, TestRunSummary, Request, ... }) => {
  TestRunSummary.registerPayload('product', productPayload);
  TestRunSummary.recordCheck({
    check: 'Short scenario title',
    expectedResult: 'What should happen in this verification step.',
    actualResult: 'As expected — what was observed.',
    passed: true,
  });

  await test.step('Attach test run summary', async () => {
    const extra: Record<string, string[]> = {};
    const pUrl = buildProcessPreviewLink(processId);
    if (pUrl) extra.process = [pUrl];
    Object.assign(extra, buildProductContractTabLinks(contractId));

    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: 'PDT-XXXX',
      relevantEntityKeys: ['customer', 'product', 'productContract'],
      extraLinks: Object.keys(extra).length ? extra : undefined,
      snapshot: { processId, contractId },
    });
  });
});
```

Produces: console block + Playwright attachments (summary plain/JSON + entity snapshot).
`SAVE_OBJECT_LINKS=true` is optional extra logging in `ResponseLinker` only.

## Don'ts

- Do not skip `test.step()` — every API call should be in a named step
- Do not use `test.only()` — CI will fail (`forbidOnly` is enabled)
- Do not create filesystem operations or shell commands outside project scope
- Do not hardcode base URLs or auth tokens — these come from fixtures and environment variables
