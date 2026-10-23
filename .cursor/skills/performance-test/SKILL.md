---
name: performance-test
description: >-
  Builds and runs EnergoTS Playwright listing performance tests for one Phoenix
  object. Asks which object and environment, reads Swagger and the Phoenix list
  screen, writes one baseline plus one case per UI filter, runs them with
  workers=1, and reports timings plus payloads at or above 2 seconds. Use when
  the user calls performance-test or perfomance-test, or asks for listing
  performance tests the same way as the Points of Delivery cases.
---

# Listing performance tests

Copy the Points of Delivery flow. Do not invent a different shape.

**Reference (read both before writing):**

- `Cursor-Project/EnergoTS/src/tests/cursor/dev2-pod-listing-performance.spec.ts`
- `Cursor-Project/EnergoTS/src/tests/cursor/dev2-pod-listing-performance.fixtures.ts`

Also follow `.cursor/skills/energo-ts-test/SKILL.md` for fixtures, Swagger refresh, `cursor-test.fixtures`, `finalizeTestRunSummary`, and the validator gate. Then run with `.cursor/skills/energo-ts-run/SKILL.md`.

## 0. Ask, then stop

If the current request does not name the business object, ask one question and do not search or write:

> Which object should I performance-test? For example: product contract, customer, invoice.

If the environment is not one of `dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`, ask in the same message. No silent default.

When both are present in the request, do not ask again.

## 1. Gates

- EnergoTS HEAD is `cursorMerged` or `staging`, and `playwright.config.ts` has `testDir: './src/tests'`. Do not checkout.
- Read `Cursor-Project/EnergoTS/playwright-environments.json` for that environment's `url`. That string is `BASE_URL`. Do not take the host from the Swagger `servers` text.
- Run `.cursor/commands/update-swagger-specs.ps1`. Use `Cursor-Project/config/swagger/{env}/swagger-spec.json`. If the id is missing, say so and stop.
- Read Phoenix list UI and the search request DTO. Do not run `switch-phoenix-branches.ps1` when any Phoenix repo has local changes. Say which branch you read.
- Do not edit `Cursor-Project/Phoenix/**`.

## 2. What to cover

Cover the object's **main listing screen** only: the list the UI posts when that screen opens, and every filter control on that screen.

Find the list by matching the UI service `post(.../list)` to a Swagger path. Read the screen's filter component and the request builder (the POD builder drops empty arrays, so the first load is only page, size, and sort). Read the Java search request for enums and the list response for which fields come back.

If the name matches more than one listing screen, ask which screen. Do not add every related endpoint.

## 3. Cases

Write `Cursor-Project/EnergoTS/src/tests/cursor/{env}-{object-slug}-listing-performance.spec.ts` and a sibling `.fixtures.ts`.

Titles use `[{SLUG}-LIST-PERF]` because there is no Jira key. Import `./cursor-test.fixtures`. No `beforeAll`. `test.describe.configure({ mode: 'default' })`. Each test sets a timeout of `4 * 180_000 + 120_000`. The spec fails unless `baseURL` contains the environment URL from `playwright-environments.json`.

| Case | Payload |
|------|---------|
| Baseline | The body the screen sends when every control is empty. Four calls. |
| One test per filter control | Baseline body plus that one control. Four calls. |

Value rules:

- Enum dropdown: the first option in the UI options array. Swagger enum spelling.
- Nomenclature id: GET the same filter URL the dropdown uses, first ACTIVE row. Do not hardcode an id. Do not POST a new nomenclature.
- List display name may be shorter than the nomenclature legal name. Treat a name as the same operator when one starts with the other plus a space or hyphen.
- Numeric from/to: use a number already present on the first unfiltered page. Do not open the detail record to find it.
- Search dropdown: one test per option, plus Exact Match when that checkbox is on the screen. `prompt` comes from a field the list response already returns. `prompt` must be non-empty. An empty prompt is the unfiltered list and must not be sent.
- If the only copy of a search value is on `GET /{object}/{id}` and that call is not 2xx, skip that search case, print the status and message, and continue the other cases. Do not substitute an empty prompt.

Each measured call records HTTP status, listing-load milliseconds (request start until the JSON body is received), seconds, rows, total elements, and UTF-8 body bytes. Print the table with average, min, max, and median. Attach it. A slow 2xx does not fail the test. A non-2xx, a timeout, or a page that is not a list fails the test. When the list DTO has the filtered field, every returned row must match. HTTP 206 is success.

`registerPayload` the request body. End each test with `finalizeTestRunSummary`. Run `playwright-test-validator` and require at least 80 before execution.

## 4. Run

From `Cursor-Project/EnergoTS`, with `BASE_URL` set to the environment URL:

```powershell
npx playwright test src/tests/cursor/{env}-{object-slug}-listing-performance.spec.ts --project=main --workers=1
```

`--workers=1` is required so the timings do not overlap. If `token.json` is missing, run `--project=setup` first. On 401, stop and say the portal rejected `PORTAL_USER` / `PASSWORD` in `EnergoTS/.env`. Do not print those values.

## 5. Chat result

1. Full table: filter, value, matched count, average, min, max.
2. **Developer handoff:** every case whose average is **>= 2.000 s**, slowest first, with the exact JSON body that was posted.
3. Findings when Swagger, the UI, and the live status disagree (Rule QA.2).
4. Confidence and `Agents involved: PerformanceTestAgent, EnergoTSTestAgent, EnergoTS Playwright Test Runner, PhoenixExpert, Senior QA Tester`.

Do not write a report file unless the user asks to save one.
