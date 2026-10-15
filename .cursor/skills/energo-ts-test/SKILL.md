---
name: energo-ts-test
description: Manages EnergoTS API Playwright specs (src/tests *.spec.ts, *.fixtures.ts). Mandatory Swagger refresh + playwright instructions pack before spec edits. Frontend page objects and src/frontend specs for a merged UI change belong to playwright-frontend (Rule 45, cursorMerged only).
---

# EnergoTS Test Skill

**Subagent (I/O):** `.cursor/agents/energo-ts-test.md`  
**Validator after authoring:** `playwright-test-validator`

## When to apply

- User asks to create, modify, or analyze EnergoTS API Playwright tests (map test cases → `EnergoTS/src/tests/cursor/{KEY}-*.spec.ts` when TC `.md` exists)
- API specs and fixtures go under `Cursor-Project/EnergoTS/src/tests/`. Branch: stay on `cursorMerged` or `staging` (Rule ENERGOTS.0). Do not switch to `cursor`.
- Frontend specs and page-object updates for a merged UI change are **playwright-frontend** (Rule 45). That workflow stays on `cursorMerged` and does not checkout. Do not author those `src/frontend` specs here.

## Repository layout (post staging merge)

`playwright.config.ts` sets `testDir: './src/tests'`. JSON report: `src/backend/playwright-report.json`. HTML report: `src/backend/playwright-report/`. Frontend project `testDir`: `./src/frontend`.

| Path | Role |
|------|------|
| `src/tests/cursor/` | New AI-authored API specs, `cursor-test.fixtures.ts`, `shared/` |
| `src/tests/<domain>/` | Legacy domain API specs |
| `src/tests/setup/` | `global-setup.ts`, `global-teardown.ts`, `jira-update.ts` |
| `src/backend/fixtures/` | `baseFixture.ts`, login, `token.json`, `envVariables.json`, `salesPortalToken.json` |
| `src/backend/jsons/` | Payload generators and payload templates (`payloads/<domain>/`, no `payloads/create/`) |
| `src/backend/mass-imports/` | Mass-import generator library |
| `src/backend/utils/` | `RequestWrapper`, reporters, `randomGens` |
| `src/backend/validations/` | Response validators |
| `src/frontend/` | UI specs, POM, frontend fixtures |
| `tests/regression/` | Visual regression only. Own config `tests/regression/playwright.regression.config.ts`. Not part of `testDir` |

### Import prefix rule

- From `src/tests/cursor/*.spec.ts` or `src/tests/<domain>/*.spec.ts`, support code is reached through `backend/`: `../../backend/fixtures/baseFixture`. One extra directory (`src/tests/cursor/shared/`, `src/tests/billing/<sub>/`) uses `../../../backend/...`.
- Files already under `src/backend/**` import sibling folders directly (`../../fixtures/baseFixture` from `src/backend/jsons/payloadGenerators/`). Do not insert another `backend/` segment.
- Frontend files under `src/frontend/**` keep local `../utils` and `../fixtures` imports. Cross-imports into the API stack use `../backend/...` or `../../backend/...`.

## Mandatory before `.spec.ts` / `.fixtures.ts` edits

1. **Playwright instructions pack** — read **`Cursor-Project/config/playwright_generation/playwright instructions/`** in order: `project-description.md` → `general-rules.md` → `test-writing-rules.instructions.md` → `SKILL.md` → other `*.md` alphabetically. Ignore `__MACOSX` / `._*`.
2. **Swagger refresh (Rule 41):**
   ```powershell
   powershell -ExecutionPolicy Bypass -File ".cursor/commands/update-swagger-specs.ps1"
   ```
   Grep `Cursor-Project/config/swagger/{env}/swagger-spec.json` for each endpoint; validate field names, enums, types from spec — **not** from TC .md alone.
3. **Reference specs (multi-entity chains):** Grep `EnergoTS/src/tests/` (prefer `src/tests/cursor/`, then domain specs such as `src/tests/contractsAndOrders/productContract.spec.ts`). Read one reference end-to-end. Entity order → **`precondition-data-creation.instructions.md`** unless citing a deliberate reference. Summary must list **Reference spec(s):** paths.

## Test creation workflow

1. **Jira** — read title + description (MCP or REST).
2. **Requirements** — endpoints, methods, payloads, edge cases; scope negatives to business behavior unless auth/routing explicitly in scope.
3. **Swagger** — per-endpoint schema validation (Step 2 above).
4. **Clarify** — ask if requirements ambiguous.
5. **Author** — only after steps 1–4 complete.

## Test naming [CRITICAL]

Format: `test('[JIRA-KEY]: {Exact Jira Task Title}', async ({...}) => {`

- **Exact** Jira title — no abbreviations, no added "| Happy path" unless in Jira title.

## Test cases → spec bridge

**Inputs:** Backend TC path (required); Frontend path when exists; Jira key + title.

1. Read all provided `.md` files; extract TC-BE-N / TC-FE-N, steps, expected results, endpoints.
2. One `test()` per main TC scenario; titles include Jira key.
3. **Output:** `EnergoTS/src/tests/cursor/{JIRA_KEY}-*.spec.ts`
4. EnergoTS fixtures only — no ad-hoc `getToken()` / `apiRequest()`.

## Precondition rules [CRITICAL]

- **No** `test.beforeAll` / `beforeAll` for data setup (Rule 40).
- Helpers at file top + `test.step('Precondition: …')` per test.
- Per-test **delta** preconditions when TCs differ — no identical setup claiming different scenarios.
- Follow **`precondition-data-creation.instructions.md`** entity order.

## Negative assertions

- Assert intended business rejection: exact status, error code/message, field.
- Do not use wrong URLs to force 404 unless routing is in scope.
- 401/403 only for permission tests.

## Framework quick reference

- Playwright API, TypeScript; fixtures: Request, Endpoints, GeneratePayload, Responses
- **New** `src/tests/cursor/*.spec.ts`: `./cursor-test.fixtures` — see test-writing-rules § Imports
- **Legacy** specs at `src/tests/<domain>/*.spec.ts`: `../../backend/fixtures/baseFixture` (add `../` per extra folder). Do not migrate legacy specs unless the user asks
- Assertions: prefer `await expect(response).CheckResponse()` for POST/create chains
- Branch: stay on **`cursorMerged` or `staging`** when `testDir` is `./src/tests` (Rule ENERGOTS.0). Do not switch to `cursor`

## Manual verification links [CRITICAL — new specs]

Every **new** `test()` ends with `finalizeTestRunSummary` (or `attachManualVerificationLinks` with `testRunSummary`):

- **`TestRunSummary` fixture** (from `./cursor-test.fixtures`) — register only **relevant** payloads + record expected/actual outcomes during the test.
- **`relevantEntityKeys`** — portal links section lists only buckets that matter for this test (not every `Responses` entry).
- Unified console + Playwright attach: test title, payloads, expected vs actual, filtered portal links.

```typescript
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary, buildProductContractTabLinks } from './shared/manual-verification-links.fixtures';

test('...', async ({ Responses, TestRunSummary, Request, ... }) => {
  TestRunSummary.registerPayload('product', productPayload);
  TestRunSummary.recordCheck({
    check: 'Short scenario title',
    expectedResult: 'What should happen in this verification step.',
    actualResult: 'As expected — what was observed. (Or: Not as expected — …)',
    passed: true,
  });
  await test.step('Attach test run summary', async () => {
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: '{JIRA_KEY}',
      relevantEntityKeys: ['customer', 'product', 'productContract'],
      extraLinks: buildProductContractTabLinks(contractId),
      snapshot: { contractId },
    });
  });
});
```

Legacy snippet (`attachManualVerificationLinks` without `testRunSummary`) — do not use for new specs.

## Permissions

- ✅ `EnergoTS/src/tests/**/*.spec.ts`, `*.fixtures.ts`
- ✅ `EnergoTS/src/frontend/**` when the user asked for page objects or frontend cases (Rule 0.8), on `cursorMerged` or `staging`
- ❌ Phoenix (Tier A)

## Post-authoring validation [MANDATORY — all paths]

After **any** new or materially changed `.spec.ts` / `.fixtures.ts` write — standalone bug automation, Rule 35 flow, or direct user request — the authoring agent **MUST** invoke **playwright-test-validator** before declaring completion or before **energo-ts-run**.

| Input | When |
|-------|------|
| `backend_path` (+ optional `frontend_path`) | TC `.md` exists on disk (Rule 35) |
| `jira_key` only (no TC file) | Bug-only automation — align coverage to Jira reproduce steps + expected/actual |

1. Invoke **playwright-test-validator** (`.cursor/agents/playwright-test-validator.md`) with spec path + inputs above.
2. **≥80/100** required to proceed to run or hand off to user.
3. **<80** → fix via energo-ts-test (max **3** iterations); then **BLOCK** and escalate.
4. Do **not** skip with “validator later” or manual self-assessment unless user **explicitly** opts out in the current chat.

User may re-validate anytime: `/playwright-validate <JIRA_KEY>` (`.cursor/commands/playwright-validate.md`).

## Completion

- New cursor spec imports `./cursor-test.fixtures` (includes `TestRunSummary` fixture)
- Every new `test()` ends with `finalizeTestRunSummary` (payloads + expected/actual + relevant portal links)
- **playwright-test-validator** PASS reported (score + iteration)
- Summary + `Agents involved: EnergoTSTestAgent` (+ PlaywrightTestValidatorAgent when validator ran)

## Confidence Score (Rule CONF.1 — Three-Zone) [MANDATORY]

Include `**Confidence: XX% (ZONE)**` with evidence factors. Zones: **GO** (≥ 85%), **CAUTION** (55–84% + assumptions list + verify items with method), **STOP** (< 55% — do not deliver spec, ask user). See `.cursor/rules/scoring/confidence_scoring_matrix.mdc`.
