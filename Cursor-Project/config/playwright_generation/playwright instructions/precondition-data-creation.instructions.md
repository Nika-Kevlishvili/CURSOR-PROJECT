---
description: "MANDATORY rules for creating test data in Playwright specs. Read BEFORE generating any .spec.ts file."
applyTo: "**/*.spec.ts"
priority: 1
---

# Precondition Data Creation Rules (MANDATORY)

## Core Principle

**NEVER assume data exists. ALWAYS create all required entities from scratch.**

Every Playwright test MUST create ALL entities it needs. Tests that query existing data instead of creating it are **FORBIDDEN**.

## Why This Matters

| Anti-pattern | Problem |
|--------------|---------|
| Query existing data | Tests fail when data is missing or different |
| Use hardcoded IDs | IDs change between environments |
| Assume entities exist | Tests are not self-contained or repeatable |
| Skip preconditions | Tests cannot verify the complete data chain |

---

## PREFERRED PATTERN: Helper Functions + test.step()

**DO NOT use `test.beforeAll()` for preconditions** — some fixtures (like `GeneratePayload`) may not be available in `beforeAll`. Instead, use **helper functions** defined at the top of the file and call them within each test.

### Step 1: Define Helper Functions for Shared Preconditions

When many tests (e.g., 50 out of 70) share the same precondition data, create **helper functions** at the top of the spec file:

```typescript
import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

// ═══════════════════════════════════════════════════════════════════════════
// SHARED PRECONDITION HELPER FUNCTIONS
// These functions create common entities used by multiple tests.
// Call them within test.step('Precondition: ...') blocks inside each test.
// ═══════════════════════════════════════════════════════════════════════════

async function sharedTerm(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const termPayload = GeneratePayload.productAndServices.term();
    const termResponse = await Request.post(Endpoints.terms, { data: termPayload });
    await expect(termResponse).toBeOK();
    Responses.terms.push(await termResponse.json());
}

async function sharedPrice(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const pricePayload = GeneratePayload.productAndServices.electricity();
    const priceResponse = await Request.post(Endpoints.priceComponent, { data: pricePayload });
    await expect(priceResponse).toBeOK();
    Responses.priceComponent.push(await priceResponse.json());
}

async function sharedProduct(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const productPayload = GeneratePayload.productAndServices.product();
    productPayload.contractTypes = ['SUPPLY_ONLY'];
    productPayload.paymentGuarantees = ['NO'];
    const productResponse = await Request.post(Endpoints.product, { data: productPayload });
    await expect(productResponse).toBeOK();
    Responses.product.push(await productResponse.json());
}

// Add more shared helpers as needed: sharedCustomer, sharedPod, sharedContract, etc.
```

### Step 2: Call Helper Functions in Each Test

Tests that use the shared preconditions call the helper functions within a `test.step()`:

```typescript
test('[JIRA-KEY]: TC-BE-1 – Happy path – Valid product appears in list', async ({
    Request, GeneratePayload, Endpoints, Responses,
}) => {
    // Call shared precondition helpers
    await test.step('Precondition: Create shared entities', async () => {
        await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
        await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
    });

    // Test step using created data
    await test.step('Call POST /products/list and find created product', async () => {
        expect(Responses.product[0]).toBeTruthy();
        const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
        await expect(response).CheckResponse();
        const body = await response.json();
        const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
        expect(found).toBeTruthy();
    });

    test.info().attach('[JIRA-KEY] TC-BE-1 response', {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json',
    });
});
```

### Step 3: For Test-Specific Data, Create Inline

When a test needs **different data** (e.g., INACTIVE status, missing fields), use shared helpers for common parts and create the specific entity inline:

```typescript
test('[JIRA-KEY]: TC-BE-3 – Inactive product is excluded', async ({
    Request, GeneratePayload, Responses, Endpoints,
}) => {
    let inactiveProductId: number;

    // Use shared helpers for common dependencies
    await test.step('Precondition: Create shared entities (terms, price)', async () => {
        await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
        await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
    });

    // Create test-specific entity with different configuration
    await test.step('Create product with INACTIVE status', async () => {
        const payload = GeneratePayload.productAndServices.product();
        payload.contractTypes = ['SUPPLY_ONLY'];
        payload.paymentGuarantees = ['NO'];
        payload.productStatus = 'INACTIVE';  // ← Test-specific configuration
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

    test.info().attach('[JIRA-KEY] TC-BE-3 response', {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json',
    });
});
```

---

## Entity Creation Order (Follow Dependencies)

**CRITICAL:** Entities must be created in dependency order. The `GeneratePayload` methods often require `Responses` to be populated.

```
┌─────────────────────────────────────────────────────────────────┐
│                    ENTITY CREATION ORDER                        │
├─────────────────────────────────────────────────────────────────┤
│ 1. Reference data (envVariables - already seeded)               │
│    ↓                                                            │
│ 2. Terms → POST /terms                                          │
│    ↓                                                            │
│ 3. Price components → POST /price-components                    │
│    ↓                                                            │
│ 4. Products → POST /products (needs terms + price components)   │
│    ↓                                                            │
│ 5. Customers → POST /customer                                   │
│    ↓                                                            │
│ 6. PODs → POST /pod                                             │
│    ↓                                                            │
│ 7. Contracts → POST /product-contract (needs customer+POD+prod) │
│    ↓                                                            │
│ 8. Billing runs → POST /billing-run (needs contracts)           │
│    ↓                                                            │
│ 9. Invoices (generated by billing run execution)                │
│    ↓                                                            │
│ 10. Payments → POST /payment (needs invoice)                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## When to Use What

| Scenario | Pattern |
|----------|---------|
| Many tests share same preconditions | Create **helper functions** at file top, call in each test |
| Test needs standard entities | Call shared helpers: `await sharedTerm(...); await sharedProduct(...);` |
| Test needs entity with specific state | Use shared helpers for dependencies, create specific entity **inline** |
| Test needs completely unique setup | Create all entities **inline** within the test |

---

## Complete Example Structure

```typescript
import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

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

// ═══════════════════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════════════════

test.describe('[JIRA-KEY]: Feature Name', { tag: '@domain' }, () => {
    test.describe.configure({ mode: 'serial' });

    // Test using shared preconditions
    test('[JIRA-KEY]: TC-BE-1 – Happy path', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {
        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Execute test', async () => {
            // Use Responses.product[0], Responses.terms[0], etc.
        });

        test.info().attach('[JIRA-KEY] TC-BE-1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // Test needing specific data
    test('[JIRA-KEY]: TC-BE-3 – Negative case', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {
        let specificEntityId: number;

        await test.step('Precondition: Create shared dependencies', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create entity with specific state', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'INACTIVE'; // Test-specific
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            specificEntityId = (await response.json()).id;
        });

        await test.step('Verify behavior', async () => {
            // Assertions using specificEntityId
        });

        test.info().attach('[JIRA-KEY] TC-BE-3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
});
```

---

## Checklist Before Generating Playwright Code

- [ ] Identify which preconditions are **shared** across many tests → create helper functions
- [ ] Identify which tests need **specific/different** data → create inline within test
- [ ] Define helper functions at the **top** of the file (before `test.describe`)
- [ ] Each helper function takes `Request, GeneratePayload, Responses, Endpoints` as parameters
- [ ] Helper functions push created entities to `Responses` arrays
- [ ] Tests call helpers within `test.step('Precondition: Create shared entities', ...)`
- [ ] Test-specific entities use `let variableName: type;` declared at test scope
- [ ] Verify entity creation order follows dependencies
- [ ] Use `await expect(response).toBeOK()` or `await expect(response).CheckResponse()`

---

## FORBIDDEN

- ❌ Using `test.beforeAll()` for preconditions (fixtures may not be available)
- ❌ Querying `/list` endpoints to find existing data
- ❌ Using hardcoded IDs (e.g., `productId = 1234`)
- ❌ Assuming any entity exists in the test environment
- ❌ Skipping precondition steps from the test case document
- ❌ Creating products without first creating terms + price components
- ❌ Storing entity IDs in `let` variables at `describe` level (use `Responses` arrays instead)
