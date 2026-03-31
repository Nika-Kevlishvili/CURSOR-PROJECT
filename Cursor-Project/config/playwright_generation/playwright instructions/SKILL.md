---
name: write-api-tests
description: "Write, modify, or debug Playwright API test cases for Energo-Pro. Use when creating spec files, adding test steps, working with payload generators, setting up entity dependencies, using CheckResponse, or troubleshooting test failures."
---

# Write API Tests

## When to Use

- Creating new `.spec.ts` test files
- Adding test cases or test steps to existing files
- Working with payload generators or entity dependencies
- Debugging test failures or assertion issues
- Setting up mass imports or nomenclature data

## Fixture Usage Pattern

Always import from `baseFixture.ts` and destructure only the fixtures you need:

```typescript
import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-XX]: Domain Name', { tag: '@domainTag' }, () => {
  test('[REG-XXX]: Test description | scenario', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
    await test.step('Step 1: Create entity', async () => {
      const payload = GeneratePayload.customers.customer_private_business();
      const response = await Request.post(Endpoints.customer, { data: payload });
      await expect(response).CheckResponse();
      Responses.customer.push(await response.json());
    });

    await test.step('Step 2: Use created entity', async () => {
      // Responses.customer[0] now has the created entity
    });

    test.info().attach('[REG-XXX] response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json'
    });
  });
});
```

## CheckResponse Matcher

**Always use `CheckResponse()` for API assertions** — never `.ok()` or `.toBeTruthy()`:

```typescript
// ✅ CORRECT
await expect(response).CheckResponse();

// ❌ WRONG — no error context
expect(response.ok()).toBeTruthy();
```

How it works:
1. `RequestWrapper` attaches `_requestMetadata` (endpoint, method, payload) to every response
2. `CheckResponse()` extracts `[REG-XXX]` from the test title automatically
3. On failure, it formats a detailed message with endpoint, method, payload, status, and response body
4. It consumes the response body internally — **never call `response.json()` before the assertion**

After `CheckResponse()` passes, you can safely call `response.json()` to store data:
```typescript
await expect(response).CheckResponse();
Responses.customer.push(await response.json()); // Safe — CheckResponse already read it on failure path only
```

## Payload Generator Architecture

All 11 domain generators are accessed via `GeneratePayload`:

| Property | Class | Domain |
|---|---|---|
| `customers` | `CustomerPayloads` | Customer CRUD |
| `pointsOfDelivery` | `PointsOfDeliveryPayloads` | POD and meters |
| `customerCommunication` | `CustomerCommunicationPayloads` | SMS, email |
| `contractsAndOrders` | `ContractsAndOrdersPayloads` | Contracts, orders, actions |
| `productAndServices` | `ProductAndServicesPayloads` | Products, services, prices, terms |
| `energyData` | `EnergyDataPayloads` | Billing profiles, scales |
| `billing` | `BillingPayloads` | Billing runs, invoices |
| `receivablesManagement` | `ReceivablesManagementPayloads` | Payments, deposits, reminders |
| `operationsManagement` | `OperationsManagementPayloads` | Tasks, activities, processes |
| `masterData` | `MasterDataPayloads` | Master data operations |
| `salesPortal` | `salesPortalPayloads` | Sales Portal API |

### Entity Dependency Order

Payload generators automatically link prerequisite entities from `this.responses`. Tests **must** create entities in dependency order. Example for a billing run:

```
1. Customer           → Responses.customer.push()
2. Product            → Responses.product.push()
3. Terms              → Responses.terms.push()
4. Price Component    → Responses.priceComponent.push()
5. Product Contract   → Responses.productContract.push()  (links customer, product, terms)
6. Energy Data        → Responses.dataByProfiles.push()    (links POD, contract)
7. Billing Run        → Responses.billingRun.push()        (links contract)
```

Each generator method knows what it needs from `this.responses` arrays. Check the specific domain class in `jsons/payloadGenerators/domains/` to see required prerequisites.

### Modifying Payloads

Generators return mutable objects. Override fields after generation:

```typescript
const payload = GeneratePayload.customers.customer_private_business();
payload.customerIdentifier = customNumberUIC;
payload.foreign = false;
payload.bankingDetails = null;        // Remove optional sub-objects
payload.address.localAddressData.districtId = null;  // Nullify nested fields
```

## Nomenclatures

Get-or-create pattern — searches for existing active nomenclature first, creates if not found:

```typescript
const accountManagerTypeId = await Nomenclatures.Account_manager_types('Manager');
const currencyId = await Nomenclatures.currencies('BGN');
```

Most nomenclature IDs are pre-cached in `fixtures/envVariables.json` during setup. Access cached values via:
```typescript
import { envVariables } from '../../fixtures/envCashed';
const cachedId = envVariables.someNomenclatureKey;
```

## Mass Import Pattern

The `MassImportGenerator` fixture is **commented out** in `baseFixture.ts`. Instantiate manually:

```typescript
import { massImportGenerator } from '../../mass-imports/generators/massImportGenerator';

const mig = new massImportGenerator(Request.raw, FileUploadRequest, Responses, uploadUrl, downloadUrl);
const { payload, templateName } = mig.CustomerMassImportGenerator.privateCustomerMP();
```

Domain generators: `CustomerMassImportGenerator`, `ProductContractMassImportGenerator` in `mass-imports/generators/domains/`.

Supports horizontal (default) and vertical (`vertical: true`) Excel template layouts.

## Sales Portal Tests

Use `SPRequest` fixture (separate base URL + OAuth2 token):

```typescript
test('[REG-XXX]: Sales Portal test', async ({ SPRequest, Endpoints }) => {
  const response = await SPRequest.get(Endpoints.salesPortalEndpoints.podCustomerListByCoordinates);
  await expect(response).CheckResponse();
});
```

## Report Attachment

Always attach response data at the end of a test for debugging and link generation:

```typescript
test.info().attach('[REG-XXX] response', {
  body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
  contentType: 'application/json'
});
```

Requires `SAVE_OBJECT_LINKS=true` in env to generate clickable links.

## Common Mistakes

1. **Calling `response.json()` before `CheckResponse()`** — response body is a stream, consumed on first read
2. **Hardcoded IDs** — always use `Responses` arrays or `Nomenclatures` for dynamic data
3. **Missing `test.step()`** — use steps for granular failure reporting in HTML reports
4. **Wrong fixture for uploads** — use `FileUploadRequest` (not `Request`) for multipart/form-data
5. **Missing entity prerequisites** — check the payload generator class to see what `this.responses` arrays must be populated before calling a method
6. **Forgetting `await` on `expect(...).CheckResponse()`** — this is async, must be awaited
