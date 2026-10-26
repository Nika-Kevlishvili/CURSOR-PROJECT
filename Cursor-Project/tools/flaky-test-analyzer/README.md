# Flaky Test Analyzer & Fixer

Local web application for Playwright HTML reports from the EnergoTS automation repository. It imports ZIP archives from Dev, Dev2, and Test, stores the history in SQLite, and proposes test changes that are applied only after an explicit approval of that exact diff.

The analyzer lives in `Cursor-Project/tools/flaky-test-analyzer`. It reads `Cursor-Project/EnergoTS` and does not modify that repository during import or analysis. Workspace rules keep EnergoTS test files off the agent’s direct edit path; this tool is the place that can write an approved diff at runtime.

## Architecture

```
browser (Vite + React) -- /api --> Express server
                                   |-- ZIP safety and Playwright 1.56 report parser
                                   |-- failure normalization, flaky classification, root-cause notes
                                   |-- source correlation under EnergoTS (read-only until approval)
                                   |-- proposal state machine and exact patch apply
                                   |-- Playwright CLI validation
                                   |-- SQLite history (node:sqlite)
```

EnergoTS today:

- Package `playwright-automationts`, lockfile `@playwright/test` **1.56.0**. `node_modules` is not installed in this checkout.
- `playwright.config.ts` uses `testDir: ./tests`, HTML reporter (`open: 'never'`) plus JSON reporter `playwright-report.json`.
- Local retries are `0` unless `CI` is set, in which case the config uses `1`. Workers are `1` on CI and otherwise unset.
- Environments are selected with `BASE_URL`. URLs are read from `playwright-environments.json`:
  - Dev: `http://10.236.20.11:8091`
  - Dev2: `https://devapps.energo-pro.bg/backend/phoenix-dev2`
  - Test: `https://testapps.energo-pro.bg/backend/phoenix-epres`
- There is no `dev` or `test` script in the EnergoTS `package.json`. Validation calls `node node_modules/@playwright/test/cli.js` directly.
- Tests are `*.spec.ts` under `tests/`, usually imported from `fixtures/baseFixture.ts`, with tags such as `@customer` and titles that include `[REG-n]`.
- The HTML report for Playwright 1.56 appends a `playwrightReportBase64` ZIP to `index.html`. That ZIP contains `report.json` and one JSON file per test file. The JSON reporter is accepted when the HTML payload is absent.

Parsing order:

1. Embedded HTML report ZIP (Playwright 1.56).
2. A Playwright JSON reporter file in the archive.
3. A `application/json` script block inside the HTML file.
4. A clear error when none of those contain test records. Missing fields stay null. They are not filled in.

## Requirements

- Node.js 22.12 or newer (`node:sqlite` is started with `--experimental-sqlite`).
- npm.

## Install

```powershell
cd Cursor-Project/tools/flaky-test-analyzer
npm install
```

To run validation against EnergoTS, install that project’s dependencies separately:

```powershell
cd Cursor-Project/EnergoTS
npm ci
```

The analyzer does not install Playwright for you and does not read `.env`. Credentials stay in the EnergoTS process environment. Do not put tokens in report ZIPs.

## Commands

| Script | What it actually does |
| --- | --- |
| `npm run dev` | API on `127.0.0.1:4178` and the UI on `http://127.0.0.1:5173` |
| `npm run build` | Vite client build and esbuild server bundle |
| `npm start` | Serves the built UI and API on `127.0.0.1:4178` |
| `npm test` | Vitest, including ZIP, parser, classification, approval, and patch tests |
| `npm run typecheck` | `tsc` for the server and the client |

`npm test` does not claim the EnergoTS suite passed. It tests this application.

## Configuration

| Variable | Default |
| --- | --- |
| `FLAKY_ANALYZER_HOST` | `127.0.0.1` |
| `FLAKY_ANALYZER_PORT` | `4178` |
| `FLAKY_ANALYZER_AUTOMATION_ROOT` | `Cursor-Project/EnergoTS` |
| `FLAKY_ANALYZER_DATA_DIR` | `./data` |
| `FLAKY_ANALYZER_RETENTION_DAYS` | `90` |
| `FLAKY_ANALYZER_MAX_ARCHIVE_BYTES` | 80 MB |
| `FLAKY_ANALYZER_MAX_EXPANDED_BYTES` | 200 MB |
| `FLAKY_ANALYZER_MAX_FILES` | 4000 |
| `FLAKY_ANALYZER_MAX_PROCESSING_MS` | 60000 |
| `FLAKY_ANALYZER_MAX_REPEATS` | 5 |
| `FLAKY_ANALYZER_VALIDATION_TIMEOUT_MS` | 15 minutes |

The server refuses a non-loopback host unless `FLAKY_ANALYZER_ALLOW_NON_LOOPBACK=true`.

AI notes are optional and off by default. They turn on only when all of these are set:

- `FLAKY_ANALYZER_AI_ENABLED=true`
- `FLAKY_ANALYZER_AI_BASE_URL`
- `FLAKY_ANALYZER_AI_API_KEY`
- `FLAKY_ANALYZER_AI_MODEL`

Report text and a redacted source excerpt are sent only in that case. AI output is stored as a note. It cannot approve or apply a patch. Instructions embedded in a report are not treated as user instructions.

## Import a report

1. From EnergoTS, after a run, zip the `playwright-report` folder (the folder that contains `index.html`). You can also include `playwright-report.json`.
2. Open **Import**.
3. Choose Dev, Dev2, or Test. That value is stored with the execution. It is not inferred from the ZIP.
4. The label and timestamp are optional. If you omit the timestamp, the report’s own start time is used when the HTML data includes one. Otherwise the field stays empty.
5. Importing the same bytes again returns the existing execution and does not create a second history row.

The UI shows upload progress. Parser warnings are listed after a successful import.

## How a failure is classified

A single failure is **insufficient evidence**. It is not marked confirmed-flaky.

Confirmed intermittent behavior requires a failed attempt followed by a pass, or both outcomes across at least three executions. Two disagreeing executions are **suspected flaky**. Failures that stay inside one environment while another environment has only passes are **environment-specific**. Repeated assertion failures are a **probable application defect**, and the tool does not propose a weaker assertion. A missing environment is shown as **No result**, never as a pass.

Failure frequency is failed, timed-out, and interrupted attempts divided by observed pass/fail attempts. Skipped attempts are left out. Retry recovery is initial failures that later passed, divided by initial failures that had another attempt. The UI prints the numerator and denominator.

## Approval and patches

States: `PROPOSED → APPROVED → APPLIED → VALIDATED`. Also `REJECTED`, `BLOCKED`, `VALIDATION_FAILED`, and `STALE`.

- Import and analysis only read source files.
- **Approve and apply this fix** sends `decision: "approve_and_apply"` and the proposal id. That is the only way a patch is written.
- **Approve only** records approval and does not write.
- **Reject** and **Leave pending** do not write. Leave pending sends no request.
- **Reanalyze** marks an open proposal stale and builds a new one from the current files and stored results.
- Apply checks the file hash captured when the diff was built, checks `git status` for that path, and replaces the approved snippet once. If the file changed, the proposal becomes `STALE` and the new bytes stay as you left them.
- Patches may only touch `tests/`, `fixtures/`, or `utils/` TypeScript files inside the automation root.
- The patch policy rejects removed assertions, new `test.skip` / `fixme`, new retries, higher timeouts, new `waitForTimeout`, and empty `catch` blocks.
- If the write fails, the original contents of the files in that proposal are restored.
- **Revert applied patch** is a separate explicit action. It restores the backed-up file only when the current hash still matches the applied hash.

A diagnostic recommendation has no diff and cannot be applied. Probable product defects and environment-only failures stay in that category.

## Validation

After a patch is applied, **Fix review** can start Playwright. You choose Dev, Dev2, or Test, a repeat count from 1 to 5, and the confirmation checkbox. The command is:

```text
node <EnergoTS>/node_modules/@playwright/test/cli.js test <file> -g <escaped title> --reporter=json
```

`BASE_URL` is set from `playwright-environments.json`. Retries and workers stay as `playwright.config.ts` defines them. Production URLs are refused. The run is the affected test file and title, not the full suite.

The stored result includes the command, environment, tests, start and end, exit code, passed/failed/skipped/interrupted/flaky counts, the JSON report directory, and whether that sample supports the fix. A missing Playwright install, missing auth, or a run that executes zero tests is `BLOCKED`, not `VALIDATED`. One green run is recorded as a sample. The screen says it does not prove an intermittent test is gone.

## History and retention

SQLite file: `data/analyzer.sqlite`. Attachment copies live under `data/storage/`. Restarting the process keeps the database.

After `FLAKY_ANALYZER_RETENTION_DAYS`, attachment directories for old imports are deleted and `attachments_available` is cleared. Result rows stay. The UI says the attachments were removed and that this is not an empty history.

A test that fails again after a proposal reached `VALIDATED` is marked regressed.

## Security limits

- Bind address is loopback. Loopback origins (`127.0.0.1`, `localhost`) may call the API so the built UI on port 4178 can load. Other origins must be listed in `FLAKY_ANALYZER_ORIGINS`.
- Mutating requests require the header `X-Flaky-Analyzer: 1`. Browser origins are limited to the local Vite ports.
- ZIP entries with `..`, absolute paths, or drive prefixes are rejected. The whole archive is refused.
- Archive size, expanded size, file count, per-file size, and processing time are capped.
- HTML and JSON are parsed as data. Scripts in a report are not executed.
- Bearer tokens, JWTs, and password-like assignments are redacted before they are stored.
- No git commit, push, or deploy is performed.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `node:sqlite` fails | Start scripts already pass `--experimental-sqlite`. Use Node 22.12+. |
| Import says the HTML has no report data | Zip the generated `playwright-report` folder, including the full `index.html`, not a screenshot of it. |
| Validation is `BLOCKED` | Run `npm ci` in EnergoTS. Auth still needs the existing `.env` (`PORTAL_USER`, `PASSWORD`, `DEVAUTHAPI` or `TESTAUTHAPI`) in the environment Playwright already uses. This app will not invent those values. |
| Patch becomes `STALE` | The file changed after the diff was created. Use Reanalyze and approve the new diff. |
| Counts are zero | No report has been imported yet. The zeros are the database, not sample data. |

## Tests

`npm test` covers valid and nested HTML reports, the JSON reporter, the HTML JSON fallback, malformed ZIPs, missing reports, path traversal, size and file-count limits, duplicate imports, optional metadata, error signatures, cross-environment identity, retry flakiness, a single failure not being confirmed flaky, environment-specific failures, low confidence on a bare timeout, proposal generation without writing the spec, approval id matching, rejection, stale detection, forbidden paths, transactional restore, missing-versus-pass comparison, blocked validation, and the mutation header.
