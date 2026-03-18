# Summary – global-setup fixture paths

**Issue:** `path.resolve('fixtures', …)` is relative to process cwd → wrote to `CURSOR-PROJECT/fixtures/` when Playwright ran from workspace root.

**Fix:** `energoTsRoot = path.resolve(__dirname, '..', '..')` from `tests/setup/`, then `path.join(energoTsRoot, 'fixtures', …)` for token.json, envVariables.json, salesPortalToken.json.

**File:** `EnergoTS/tests/setup/global-setup.ts`

Agents involved: None (direct tool usage)
