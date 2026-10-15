---
name: playwright-frontend
description: Authors and runs EnergoTS frontend Playwright specs on cursorMerged only. Inspects phoenix-ui for the merged UI change, updates the page object with playwright-cli when locators are missing, writes src/frontend specs from the frontend Playwright instructions file, applies one validator critique, then executes and classifies failures. Use for /playwright-frontend or when the user asks to automate frontend cases.
---

# Playwright Frontend

Frontend automation for EnergoTS. API specs under `src/tests/` stay with **energo-ts-test**. This skill owns `src/frontend/**` specs and the page objects they need for a new UI change.

All EnergoTS paths are under `Cursor-Project/EnergoTS/`. Phoenix UI is `Cursor-Project/Phoenix/phoenix-ui/` and stays read-only.

## When to apply

- User runs `/playwright-frontend`, or asks to automate frontend cases, update a page object for a merged UI change, or run the frontend specs that came from test-case generation.
- Test-case-generator has already saved `Cursor-Project/test_cases/Frontend/<Topic>.playwright.md` because the user chose Backend+Frontend.

Do not use this skill for Backend-only API specs.

## Branch lock (BLOCK)

These two agents work only on **`cursorMerged`**:

1. In `Cursor-Project/EnergoTS/`, run `git rev-parse --abbrev-ref HEAD`.
2. If HEAD is not `cursorMerged`, **stop**. Tell the user the checkout is on another branch. Do not `git checkout`, `git switch`, `git reset`, or `git clean`. Do not move from `staging` or `cursor` onto `cursorMerged` yourself.
3. Confirm `playwright.config.ts` contains `testDir: './src/tests'` and a project named `frontend` with `testDir: './src/frontend'`.
4. **playwright-frontend-validator** follows the same lock. It does not checkout either.

`cursor` and `staging` are allowed for other EnergoTS work (Rule ENERGOTS.0). They are not allowed for this skill.

## Gates before writing

| Gate | If missing |
|------|------------|
| Environment (dev, dev2, test, preprod, prod, experiments) | Ask and stop. Do not infer it. |
| `test_cases/Frontend/<Topic>.md` | Stop. Run test-case-generator with Backend+Frontend. |
| `test_cases/Frontend/<Topic>.playwright.md` | Stop. The generator writes this file. Do not invent a substitute. |
| `src/backend/fixtures/token.json` has `token` | Stop. Tell the user to run `npx playwright test --project=setup` from EnergoTS. Do not use the portal login form. |

PreProd or Prod **execution** needs an explicit user yes. Discovery on those environments also needs that yes before opening the live page.

## Read first

1. The topic's `*.playwright.md` and the matching `Frontend/<Topic>.md` (TC-FE-N).
2. `src/frontend/meterCreate.spec.ts` — the spec shape to copy.
3. `src/frontend/.instructions.md`, `src/frontend/pom/PageObjects.ts`, `src/frontend/fixtures/FrontEndFixture.ts`, and the closest existing POM for the page.
4. `.cursor/skills/pom/SKILL.md` sections **Playwright CLI**, **Reach the page**, and **Locator rules**. Drive the live page with `playwright-cli` only. Do not call Playwright MCP (`browser_navigate`, `browser_snapshot`, or any `browser_*` tool).

Before the first `.spec.ts` edit, run Swagger refresh (Rule 41). API preconditions use the refreshed spec for field names and enums. If refresh fails, say so and continue with the cached spec.

## 1. Find the merged UI change (no checkout)

Inspect phoenix-ui. Do not checkout, switch, reset, or clean that repo. Do not run `switch-phoenix-branches.ps1` from this skill.

```bash
git -C Cursor-Project/Phoenix/phoenix-ui fetch origin --prune
git -C Cursor-Project/Phoenix/phoenix-ui log --oneline \
  origin/dev origin/dev2 origin/test origin/preprod origin/prod origin/experiments \
  --grep="<JIRA_KEY>"
```

If the key is absent, search by the page or component name from the instructions file.

For each candidate commit:

- `git -C Cursor-Project/Phoenix/phoenix-ui branch -r --contains <sha>` — which remote branches contain it.
- `git -C Cursor-Project/Phoenix/phoenix-ui show <sha> --stat` and the patch for `*.html`, `*.ts`, and i18n JSON.
- Read a file from a branch without checkout: `git show origin/<branch>:src/app/pages/...`

Record: commit, branches that contain it, page route, added or changed controls (labels, tabs, buttons, inputs), and the component paths.

If `fetch` fails, use the local `origin/*` refs and say they may be stale. Do not checkout to refresh them.

## 2. Update the page object before any spec

Compare that UI diff to the existing POM class named in the instructions file (or the closest class under `src/frontend/pom/createPOM/` or `editPOM/`).

**Locators and methods already cover the new controls:** do not crawl the page. Say the POM already has them.

**A new control is missing** (new tab, button, field, dialog): record only that added surface.

1. Confirm `playwright-cli --version`. If it is missing, stop and tell the user to install `@playwright/cli`.
2. Read `.cursor/skills/playwright-cli/SKILL.md` (or `.agents/skills/playwright-cli/SKILL.md`).
3. One session: `playwright-cli -s=pom`. Open the frontend root with `attachFrontendJwt` from `src/frontend/pom/MainPage.ts`. Never print the token. Never open the portal login page. Then `goto` the page route without `jwt`.
4. Snapshot the new tab, dialog, or section. `generate-locator <ref> --raw`. Keep the locator only when it matches the pom skill order (`getByRole`, then label, placeholder, test id). Verify it matches one element.
5. Add locators and private atomic methods to the **existing** class. Register a new class in `PageObjects.ts` only when no class exists for that page.
6. Do not put business assertions in the POM. Do not store snapshot refs. Do not hardcode nomenclature labels; use `frontendNomenclatureName` the way `MeterCreatePage` does.
7. Close the session: `playwright-cli -s=pom close`.

If the POM class does not exist yet, build it with the pom skill's **Build the POM** rules for this page, then continue with the instruction-file cases (positive and negative), not a single happy-path spec only.

## 3. Write the specs

Create or update specs under `src/frontend/`. One `test()` per TC-FE in the instructions file. Map the title to the TC id.

Pattern (from `meterCreate.spec.ts`):

```typescript
import { randomGens } from '../backend/utils/randomGens';
import { test, expect } from './fixtures/FrontEndFixture';
import { getFrontendUrl, openAuthenticatedFrontendPage } from './pom/MainPage';

test('TC-FE-1 (Positive): …', async ({
    POM,
    MainPage,
    Request,
    GeneratePayload,
    Responses,
    FrontendResponses,
    Endpoints,
}) => {
    await test.step('Precondition: create a point of delivery', async () => {
        const podPayload = GeneratePayload.pointsOfDelivery.pod_slp();
        const pod = await Request.post(Endpoints.pod, { data: podPayload });
        await expect(pod).CheckResponse();
        Responses.pod.push(await pod.json());
        FrontendResponses.pod.push(podPayload.identifier);
    });

    await test.step('Precondition: open the page', async () => {
        const pageUrl = new URL('<route>', getFrontendUrl()).toString();
        await openAuthenticatedFrontendPage(MainPage, pageUrl);
    });

    await test.step('Exercise the page object', async () => {
        // pass FrontendResponses.pod[0] into the POM arguments
        // assert the business outcome from the instructions file
    });
});
```

Rules:

- Preconditions that create another business object (POD for a meter, customer for a contract) go through `Request`, `GeneratePayload`, and `Endpoints` inside `test.step('Precondition: …')`. Keep the order from the instructions file and from the closest API spec under `src/tests/`.
- After each create, push the response JSON onto `Responses.<same key>` and push the **on-screen name** onto `FrontendResponses.<same key>`. `FrontendResponses` is `string[]` per key (`src/backend/fixtures/types/responses.ts`). Push the identifier or name the screen shows. Do not push the numeric id.
- Pass `FrontendResponses.<key>[index]` into the POM. The POM does not call the API.
- Do not create that prerequisite through the page under test.
- Nomenclature dropdowns: omit the argument so the POM calls `frontendNomenclatureName`. Do not hardcode the label.
- The page under test is the only UI flow: create, edit, or check the new tab, as the instructions say.
- **Edit or new-tab cases:** create the main object through the API first, push its display name to `FrontendResponses`, open the edit route, then use the POM for the new tab or field.
- **Create cases:** create only the prerequisites through the API. Create the object itself through the POM.
- No `test.beforeAll` / `beforeAll` (Rule 40).
- No portal login, no `jwt` on the deep link, no hardcoded entity ids, no credentials in source.
- Assertions stay in the spec: network result, route, and the visible value from `FrontendResponses`. Seeing a button is not a pass.
- Do not delete created data.

`FrontEndFixture` extends the API fixture, so `Request`, `GeneratePayload`, `Responses`, `FrontendResponses`, and `Endpoints` are already in the test arguments.

## 4. One validator pass

After the POM and specs are saved, invoke **playwright-frontend-validator** once with the spec paths, POM paths, both frontend markdown files, and the phoenix-ui diff summary.

Apply every fix it lists. Then stop editing for quality. **Do not call the validator again.**

If the score was below 80, still apply the one fix list and continue to execution. Note any fix you could not apply in the report under automation improvements.

## 5. Execute

From `Cursor-Project/EnergoTS`, with `PLAYWRIGHT_HTML_OPEN=never`:

```bash
npx playwright test --project=frontend src/frontend/<spec>.spec.ts
```

Re-run a single failed test at most once when the failure is an obvious locator or wait in the spec you just wrote. Do not loop. Do not weaken an assertion to force a pass.

Classify each result:

| Class | When |
|-------|------|
| **Passed** | The test finished green. |
| **Product code defect** | The locator matched, the page was reached, and the business assertion failed (wrong status, missing tab content, message differs from the TC, positive save returned an error). |
| **Automation defect** | Locator timeout, strict-mode collision, wrong route, empty `FrontendResponses`, POM skipped a control, crash before a business assertion, accessible name in phoenix-ui differs from the locator. |
| **Environment blocker** | Missing token, rejected `jwt`, network down, 401 on setup. Not a product defect. |

When both look possible, use **automation defect** if the locator never matched, and **product code defect** if it matched and the expected business outcome failed.

## Report (chat)

Do not write a report file unless the user asks (Rule 0.6).

```text
## Frontend Playwright report
Branch: cursorMerged @ <sha>
Environment: <env>
phoenix-ui: <sha> contained on <origin branches>
POM: <updated files, or already covered>
Validator: <score>/100 on pass 1; fixes applied: <list or none>
Command: npx playwright test --project=frontend <spec>

| Test | Result | Class | Note |
| TC-FE-1 | PASSED | — | |
| TC-FE-2 | FAILED | Product code defect | |

Product code defects: …
Automation improvements still open: …
Environment blockers: …
```

## Confidence (CONF.1)

Use **Playwright Frontend (Rule 45)** in `.cursor/rules/scoring/confidence_scoring_matrix.mdc`. Show the validator sub-score and the run sub-score. Aggregate with **minimum**.

## Footer

`Agents involved: PlaywrightFrontendAgent, PlaywrightFrontendValidatorAgent`
