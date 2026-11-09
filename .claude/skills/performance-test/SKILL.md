---
name: performance-test
description: >-
  Builds and runs EnergoTS Playwright listing performance tests for one Phoenix
  object. Asks which object and environment, reads Swagger and the Phoenix list
  screen, writes one baseline plus one case per UI filter, runs them with
  workers=1, adds combination cases that use more than one filter at once, and
  reports in the PHN-4648 layout: every timing, the cases at or above 2 seconds,
  and their request payloads. Use when the user calls performance-test or
  perfomance-test, or asks for listing performance tests the same way as the
  Points of Delivery cases.
---

# Listing performance tests

Copy the Points of Delivery flow. Do not invent a different shape.

**Reference (read all four before writing):**

- `Cursor-Project/EnergoTS/src/tests/cursor/dev2-pod-listing-performance.spec.ts`
- `Cursor-Project/EnergoTS/src/tests/cursor/dev2-pod-listing-performance.fixtures.ts`
- `Cursor-Project/EnergoTS/src/tests/cursor/dev2-product-contract-listing-performance.spec.ts` (single-filter cases and the data-driven `COMBO_CASES` table with its `resolveComboContext`, `combinedMismatches`, and `measureFilter`)
- `Cursor-Project/EnergoTS/src/tests/cursor/dev2-product-contract-listing-performance.fixtures.ts` (`MATCHING_*` constants, `ALL_*_FILTERS`, `loadListedDateWhere`, `loadListingSample` with a `where` argument)

Also follow `.claude/skills/energo-ts-test/SKILL.md` for fixtures, Swagger refresh, `cursor-test.fixtures`, `finalizeTestRunSummary`, and the validator gate. Then run with `.claude/skills/energo-ts-run/SKILL.md`.

## 0. Ask, then stop

If the current request does not name the business object, ask one question and do not search or write:

> Which object should I performance-test? For example: product contract, customer, invoice.

If the environment is not one of `dev`, `dev2`, `test`, `preprod`, `prod`, `experiments`, ask in the same message. No silent default.

When both are present in the request, do not ask again.

## 1. Gates

- EnergoTS HEAD is `cursorMerged` or `staging`, and `playwright.config.ts` has `testDir: './src/tests'`. Do not checkout.
- Read `Cursor-Project/EnergoTS/playwright-environments.json` for that environment's `url`. That string is `BASE_URL`. Do not take the host from the Swagger `servers` text.
- Run `.claude/commands/update-swagger-specs.ps1`. Use `Cursor-Project/config/swagger/{env}/swagger-spec.json`. If the id is missing, say so and stop.
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

### 3a. Combination cases (always generated with the single-filter cases)

After the single-filter cases, add a second `test.describe` tagged `@combination` with cases that put **more than one filter in the same request**, the way a user narrows the screen. Build them from a data-driven `COMBO_CASES` table (name, needs, payload, expected row values). Resolve every nomenclature id, sample date and search prompt inside the test with `test.step('Precondition: …')`. No `beforeAll`.

Cover at least these shapes, using only controls that exist on the screen:

| Shape | Example |
|-------|---------|
| Two select filters | type + status; status + sub-status |
| Select filter + nomenclature | status + product; status + account manager |
| Three nomenclatures | product + account manager + sales channel |
| Toggles together | exclude old versions + exclude future versions |
| Toggle + select | exclude old versions + status |
| Date range + select | activation date + status; perpetuity date + perpetuity status |
| Date range + nomenclatures | activation date + status + account manager |
| Search + filters | contract number + status + type; customer name + status + exclude old; UIC + product |
| Exact match + toggles | exact match on contract number + both exclude toggles |
| Every select filter at once | every multi-select with all options chosen, plus nomenclatures and toggles |

Rules:

- **Every combination must return rows.** A combination that matches zero contracts only times an empty page and proves nothing. Probe the live list (read-only, one filter at a time, then pairs) to find values that overlap. Take dates and search prompts from a page already filtered the same way (`loadListedDateWhere`, `loadListingSample` with a `where` argument), not from the unfiltered first page.
- Where only data can say which ids overlap, put them in named `MATCHING_*` constants with a comment that records what was probed and when. Each such case sets `matching: true` and asserts `totalElements > 0`, so changed data fails with a clear message instead of timing an empty page. This is the one place a hardcoded id is allowed. The single-filter cases still resolve ids from the dropdown URL.
- To widen a multi-select without leaving it empty, choose all its options (`ALL_*_FILTERS`). Say so in the case name.
- If the data has no overlap for a combination (for example a signing type that never co-occurs with a product, manager, and channel), do not time an empty page. Leave that shape out, test the nearest shape that does overlap, and report the gap as a **data limit** finding in the chat result.
- Row checks: where the list DTO has the filtered field, every returned row must match every filter that was applied (`combinedMismatches`).
- Titles: `[{SLUG}-LIST-PERF]: … — combination: <name>`. Matching-data titles start with `matching data:`. Run the set with `--grep "combination"`, and the matching-data set with `--grep "matching data"`.

`registerPayload` the request body. End each test with `finalizeTestRunSummary`. Run `playwright-test-validator` and require at least 80 before execution.

## 4. Run

From `Cursor-Project/EnergoTS`, with `BASE_URL` set to the environment URL:

```powershell
npx playwright test src/tests/cursor/{env}-{object-slug}-listing-performance.spec.ts --project=main --workers=1
```

`--workers=1` is required so the timings do not overlap. If `token.json` is missing or the saved token has expired (HTTP 401 on every call), run `--project=setup` first with the same `BASE_URL`. If setup also gets 401, stop and say the portal rejected `PORTAL_USER` / `PASSWORD` in `EnergoTS/.env`. Do not print those values.

The run covers both the single-filter cases and the combination cases. Parse each case's printed timing table from the log (title, `Filter:`, `Payload:`, the four run rows, Average, Min, Max, Median, and the last page's `Total contracts`).

## 5. Chat result (PHN-4648 layout)

Give the report in the same layout as Jira `PHN-4648` ("Performance testing on Dev 2 (Pod)"), in this order. Use Markdown tables where that ticket used screenshots of tables. Do not post it to Jira. The user pastes it.

**1. Heading line**

`Performance testing on <Environment> environment — checking <object> listing time for <N> <objects>`

`N` is the `totalElements` of the unfiltered baseline. Add the run settings in one line: 4 calls per case, `--workers=1`, page size.

**2. Current timings for <object> search, listing, filters**

One table with a row for **every** case that ran, single-filter first, then combinations, in spec order:

| # | Case | Type | Filter / value | Matched | Avg (s) | Min (s) | Max (s) | Median (s) | Runs ≥ 2 s |
|---|------|------|----------------|---------|---------|---------|---------|------------|------------|

`Type` is `baseline`, `filter`, `search`, or `combination`. Skipped cases appear as a row with the skip reason. Timings use three decimals.

**3. Filters which took 2 or more seconds to load responses**

A second table with only the cases whose **average is ≥ 2.000 s**, slowest first, same columns. If none qualify, write `No case averaged 2 seconds or more.` Directly under it, add a short list titled **Single runs ≥ 2 s (average below 2 s)** with case, the slow run, and the other runs, so a spike on a cold first call is visible. Leave it out when empty.

**4. Payloads for these filters**

First the endpoint line in bold: `endpoint — <BASE_URL>/<path>` (the full URL the test posted to). Then, for **every case in table 3**, slowest first:

**<Case name> — <avg> s**

```json
{ the exact JSON body that was posted, pretty-printed }
```

Include the single-run spikes from table 3 only if the user asks. If table 3 is empty, write `No payloads to hand over.`

**5. Findings**

Data limits (a combination that cannot return rows on this data), Swagger or UI disagreements with the live status (Rule QA.2), and anything skipped.

**6. Footer**

Confidence block in the exact form `**Confidence: XX% (ZONE)**` (CONF.1), then `Agents involved: PerformanceTestAgent, EnergoTSTestAgent, EnergoTS Playwright Test Runner, PhoenixExpert, Senior QA Tester`.

Do not write a report file unless the user asks to save one.
