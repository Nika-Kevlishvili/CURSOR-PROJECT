# PDT-2023 – Playwright rerun (post-assertion tightening)

**Working dir:** `d:/Asterbit/CURSOR-PROJECT/Cursor-Project/EnergoTS/`  
**Branch:** `cursor`  
**Run command:** `npx playwright test tests/cursor/PDT-2023-billing-run-termination.spec.ts`

## Totals

- **Total:** 55
- **Passed:** 2
- **Failed:** 43
- **Skipped:** 10

## Key failure reason (dominant)

Most failures are now failing early on the list call due to the tightened assertion:

- `expect(<list response>.ok()).toBe(true)` → **Received `false`**

This shows up on requests like:

- `GET` `Endpoints.list({ size: '50' })`
- `GET` `Endpoints.list({ status: 'CANCELLED', size: '50' })`
- `GET` `Endpoints.list({ status: 'IN_PROGRESS_TERMINATION', size: '50' })`

Example snippet from the run output:

```
Error: expect(received).toBe(expected) // Object.is equality
Expected: true
Received: false

expect(listRes.ok()).toBe(true);
```

## Notes

- Fixtures were present (`fixtures/token.json`, `fixtures/envVariables.json`), so setup was not executed for this rerun.
