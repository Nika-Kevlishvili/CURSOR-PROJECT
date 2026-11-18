# Reg fixer — reference

## The archive

`merged-reports(dd.mm.yyyy).zip` is posted by the Playwright Slack app to the regression channel after the dev, dev2 and test regression workflows finish. It is built by `EnergoTS/src/backend/scripts/mergeReports.js`.

```
merged-reports.zip
  stats.json                  passed / failed / skipped / total per environment
  merged.html                 dashboard with one tab per environment
  jira-update-log.json        REG ticket statuses written to Jira
  status-maps/<env>.json      REG id -> 31 (Passed) or 2 (Failed)
  dev/index.html              Playwright HTML report for dev
  dev/data/<hash>.zip         one trace per failed case
  dev2/...  test/...
  test2/...  dev-fix/...      other runs; ignored unless the user asks
```

The archive is built on Windows, so entry names use backslashes. The extractor handles that.

`<env>/index.html` embeds a base64 zip with `report.json` (one summary row per case) and one `<fileId>.json` per spec file (steps, errors, attachments). Errors are plain strings in the current format and objects with `message` in older ones; the extractor reads both.

## extract-failures.mjs

```bash
node .claude/skills/reg-fixer/scripts/extract-failures.mjs [<zip|folder>] \
  [--envs=dev,dev2,test] [--out=<file.json>] [--registry=<known-bugs.json>] \
  [--extract-to=<empty folder>] [--max-error-chars=6000]
```

Needs Node 18 or newer. No packages. With no path it uses the newest `merged-reports*` zip in `~/Downloads`.

### Output file

| Field | Content |
|-------|---------|
| `source`, `root` | Input path and the folder that holds the environment folders |
| `environmentsInArchive`, `problems` | What the archive contains, and environments that could not be read |
| `statsJson` | `stats.json` as the pipeline wrote it |
| `registry` | Whether the known-bug registry was found and how many entries it has |
| `environments[]` | One per environment, see below |
| `crossEnv[]` | One per distinct failing case: status and error signature on every environment, `failedOn`, `sameErrorEverywhere` |
| `signatureGroups[]` | Failures grouped by error signature, most frequent first |

`environments[]`:

| Field | Content |
|-------|---------|
| `startTime`, `ageDays`, `durationMinutes`, `stats` | When the run started and its totals |
| `failures[]` | Failed and timed-out cases |
| `knownBugs[]` | Cases reported as known Phoenix bugs in this run |
| `filesWithFailures` | Every case of each spec file that has a failure, in source order, with outcome and duration. Use it to find preparation cases and cascades |

`failures[]`:

| Field | Content |
|-------|---------|
| `regId`, `title`, `suitePath`, `file`, `line`, `project`, `tags` | Identity. `file` is relative to `EnergoTS/src/tests` |
| `status` | `failed` or `timedOut` |
| `signature` | One line with ids, numbers and dates masked. The same bug has the same signature on every environment |
| `error` | Full error text without colour codes, cut at `--max-error-chars` |
| `errorLocation` | First stack frame inside EnergoTS, as `src/...:line` |
| `api` | Only for a failed `expect(response).CheckResponse()`: `method`, `endpoint`, `path`, `status`, `responseMessage`, `payload`, `response` |
| `failedStep`, `hungStep` | Step that raised the error; step that never finished (timeouts) |
| `steps` | Top-level steps of the case with durations |
| `tracePath` | Trace zip of the case |
| `attachments` | Other attachments, with text bodies (stdout) up to 4000 characters |
| `markState` | `fixed` or `drift` when the case is in the known-bug registry and did not fail as listed |
| `registry` | The matching registry entry and its state, when `--registry` was given |

## Marker

The marker is a small addition to EnergoTS. It is installed from `templates/known-bug-marker.patch`, which was generated against staging commit `937856e`.

| File | Change |
|------|--------|
| `src/backend/fixtures/knownBugs/known-bugs.json` | New. The registry: `{ "entries": [] }` |
| `src/backend/fixtures/knownBugs/knownBugs.ts` | New. Finds the entry for a case and environment and applies it |
| `src/backend/fixtures/knownBugs/README.md` | New. Entry fields and behavior |
| `src/backend/fixtures/baseFixture.ts` | Adds the automatic `knownBug` fixture, which calls `applyKnownBug` after every test |
| `src/backend/utils/reporters/ReportParser.ts` | `getTestStats` returns `knownBugs` separately. `buildStatusMap` keeps a known-bug case at 2 (Failed) |
| `src/backend/utils/reporters/SlackReporter.ts` | Adds `🐞 N known bugs` to the summary line when N is above zero. "ran" includes them |
| `src/backend/utils/generateReport.ts`, `src/tests/setup/global-teardown.ts` | Types and the log line carry `knownBugs` |
| `src/backend/scripts/mergeReports.js` | `stats.json` and the dashboard card carry `knownBugs` |

### Behavior

The fixture runs after the test body. The environment comes from `BASE_URL`: `phoenix1-dev` is dev, `phoenix2-dev` is dev2, `phoenix-epres` is test.

| Case on a listed environment | Playwright outcome | Annotation | Slack | REG ticket |
|------------------------------|--------------------|------------|-------|------------|
| Fails or times out, error matches `errorPattern` | expected (not failed) | `known-bug` | known bugs | Failed |
| Fails with another error | unexpected (failed) | `known-bug-drift` | failed | Failed |
| Passes | unexpected (failed) | `known-bug-fixed` | failed | Failed |
| Environment not listed | judged normally | none | normal | normal |

In the HTML report a known-bug case is listed with the passed cases, because Playwright has no fourth tab. Search `annot:known-bug` to list them. The annotation text is the Jira key and the summary.

A case in a serial group that fails as a known bug does not stop the cases after it.

If `known-bugs.json` cannot be parsed, the fixture logs a warning and treats no case as a known bug, so failures stay visible.

### Verification after install or change

Run on staging, then delete the probe files and restore the registry.

1. Put temporary entries in `known-bugs.json`: one whose pattern matches, one that will pass, one timeout, one for another environment, one whose pattern does not match.
2. Add a temporary spec under `src/tests/` that imports `test` from `backend/fixtures/baseFixture`, requests no fixtures, and has one case per entry.
3. `BASE_URL=https://devapps.energo-pro.bg/backend/phoenix1-dev/ npx playwright test <probe folder> --project=main`
4. In `src/backend/playwright-report.json` expect: matching failure and timeout `status: expected` with a `known-bug` annotation; the passing one and the non-matching one `status: unexpected` with `known-bug-fixed` and `known-bug-drift`; the other-environment one `unexpected` with no annotation.
5. `ReportParser.getTestStats` on that report returns the known bugs in `knownBugs`, and `buildStatusMap` gives them 2.

### What was verified, and what was not

Verified on 2026-11-18 in a scratch copy of staging `937856e`, with the steps above: the fixture outcomes, `ReportParser` counts and status map, the Slack summary line text, and `mergeReports.js` writing `knownBugs` to `stats.json` and the dashboard card. The same outcomes hold on the path CI uses, `--reporter=blob` followed by `npx playwright merge-reports -c playwright.config.ts ./blob-report`. The patch applies cleanly to that commit.

Not verified: a real regression run on the Windows runner, the Slack upload, and the Jira update. Check the first regression after install: the Slack line should show the known-bug count and the marked cases should carry the `known-bug` annotation.

The patch was generated against `staging`. The regression workflows run from other branches (see SKILL § Which branch the regression runs), so the marker has no effect on the nightly report until it reaches them.
