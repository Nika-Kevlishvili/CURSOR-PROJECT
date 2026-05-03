# Playwright run — detailed report (file)

**Canonical path:** `Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md` (with `generate-detailed-report.mjs`). **Machine detailed output** (JSON-derived): **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** next to `playwright-report.json` — generated and uploaded with this smart report on **HandsOff / path 3** when JSON exists (**Rule DPR.0**).

**Purpose:** Persist **full** run intelligence on disk. Use for **HandsOff** (`HandsOff reports/…/{JIRA_KEY}.md`) and for **path 3** scoped Slack runs (`Chat reports/…` — see `send-playwright-results-slack.md`).

**Language:** English only (Rule 0.7).

**Slack:** Do **not** paste this entire document into the chat body. Send only the **three short blocks** from **`Slack_report_summary_short_template.md`** (under `Cursor-Project/config/template/`), then **attach** this `.md` via **`Cursor-Project/config/slack/upload-file-to-slack.ps1`** (Tester DM + #ai-report).

---

## Header (run metadata)

| Field | Value |
|--------|--------|
| **Jira** | `{JIRA_KEY}` (or `N/A` for ad-hoc scoped runs) |
| **Title** | From Jira summary or scope description |
| **Date (UTC or local, state which)** | `{YYYY-MM-DD}` |
| **Environment / BASE_URL** | Value used for the run |
| **Spec file(s)** | e.g. `Cursor-Project/EnergoTS/tests/cursor/{name}.spec.ts` |
| **Test case files** (if applicable) | `Cursor-Project/test_cases/Backend/<Topic>.md`, `…/Frontend/<Topic>.md` |
| **Playwright command** | Exact command used |
| **Totals** | X passed, Y failed, Z skipped |
| **Phoenix alignment** (HandsOff only) | Env + switch script exit code + note if mixed state |

---

## Per-test sections (repeat for each executed `test()`)

Use **one block per Playwright test** (same order as run output).

### Test {N}: {Playwright test title as in spec}

| Field | Content |
|--------|--------|
| **Playwright title** | Full `test('…')` title string |
| **Covers test case(s)** | List **TC-BE-N** / **TC-FE-N** from the test title (e.g. `[PDT-2599] TC-BE-5: …`) **and/or** map to the objective in `test_cases/Backend|Frontend/<Topic>.md`. If no TC tag in title, state **Unmapped** and infer from nearest TC by intent. |
| **Objective (from TC)** | Short quote or paraphrase from the matching **Expected result** / **Objective** in the test case `.md`, or from the spec’s intent if TC missing. |
| **Created entities / links** | List URLs or resource references produced during the test. Prefer: portal/UI links if the spec logs them; else **API-style** lines: `{METHOD} {path}` + **resource id** from `Responses.*` or JSON body (e.g. `serviceContractId: 12345`). If the spec uses `test.info().annotate('entity', …)`, copy those here. Use `—` if nothing created in this test. |
| **Expected** | What must hold for this TC (status code, no invoice rows, field values, etc.). |
| **Actual** | What the run did: HTTP status, key response fields, assertion outcome; if failed, first lines of error/stack or response snippet. |
| **Meets expectation** | **Yes** / **No** / **Not run** — one word plus **one short justification** (e.g. "Yes — HTTP 400 as expected" / "No — draft invoices returned but TC expects zero rows"). |

---

## Footnotes (optional)

- **Validator / setup issues:** If Step 4.5 failed or setup failed, short subsection here (do not replace per-test blocks).
- **Follow-ups:** Open questions for the team.

---

## How to collect “links” when specs do not annotate yet

1. Parse **IDs** from Playwright stdout, HTML report, or `playwright-report.json` attachments if present.
2. From spec code / `Responses` usage, list **endpoint + id** (`GET /service-contract/{id}`).
3. Prefer adding **`test.info().annotate('created', …)`** in specs over time so future runs fill this section automatically.
