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

```typescript
import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
```

Do not import from `@playwright/test` directly — always use the project's `baseFixture`.

## Data Handling

- **No hardcoded IDs** — use `Responses` arrays, `Nomenclatures`, or `envVariables` for all entity references
- **Push created entities** to `Responses` immediately after creation for downstream use
- **Create entities in dependency order** — check the payload generator class to see prerequisites
- Use `FileUploadRequest` (not `Request`) for multipart/form-data uploads

## Report Attachment

Attach response data at the end of each test:

```typescript
test.info().attach('[REG-234] response', {
  body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
  contentType: 'application/json'
});
```

## Don'ts

- Do not skip `test.step()` — every API call should be in a named step
- Do not use `test.only()` — CI will fail (`forbidOnly` is enabled)
- Do not create filesystem operations or shell commands outside project scope
- Do not hardcode base URLs or auth tokens — these come from fixtures and environment variables
