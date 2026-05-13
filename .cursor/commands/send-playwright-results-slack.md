# send-playwright-results-slack

**Path 3 of 3** — see **`Cursor-Project/config/template/Slack_reporting_paths.md`**. Path 1 = bug validation (unchanged). Path 2 = HandsOff.

User-triggered Slack delivery of **scoped** Playwright results (not the full HandsOff pipeline).

## When to use

- The user **explicitly** asks to send Playwright results to Slack for **specific** tests (e.g. by Jira key in `grep`, file path, or test title).
- This is **separate** from **`/HandsOff`**, which already ends with a mandatory Slack step (see `.cursor/commands/hands-off.md`).

## Do not conflate

- **Rule DPR.0** (`.cursor/rules/workflows/playwright_detailed_reporting.mdc`): for **path 3**, **`playwright-report-detailed.md`** is **generated and uploaded** together with the smart **ScopedPlaywright** `.md` when **`playwright-report.json`** exists — same policy as HandsOff. See **`Cursor-Project/config/playwright/README-detailed-reporting.md`** for the generator.

## Workflow (smart reporting + machine file)

1. **Scope** – Resolve the exact Playwright target from the user (e.g. `npx playwright test --grep "PDT-2599"`, or `tests/cursor/....spec.ts`). If ambiguous, ask one clarifying question (Rule CONF.0).
2. **Branch** – `Cursor-Project/EnergoTS` on branch **`cursor`** (Rule ENERGOTS.0 / `energo-ts-run` skill).
3. **Run** – Execute the scoped `npx playwright test ...` and capture pass/fail, titles, failure messages, and (if available) test case `.md` paths for TC mapping.
4. **Machine detailed Markdown** – From **`EnergoTS/`**, if **`playwright-report.json`** exists, run **`node ../config/playwright/generate-detailed-report.mjs`** → **`Cursor-Project/EnergoTS/playwright-report-detailed.md`**. If JSON missing, skip and note in **Notes**.
5. **Smart detailed file** – Build content per **`Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md`** and save under **`Cursor-Project/reports/Chat reports/YYYY/<english-month>/<DD>/ScopedPlaywright_{JIRA_KEY_or_timestamp}.md`** per **`Cursor-Project/reports/README.md`**. If no Jira key, use `ScopedPlaywright_adhoc_{YYYYMMDD-HHmm}.md`.
6. **Jira context** – If the user gave a **Jira key**: use Jira MCP for title, assignee, **Tester** (`customfield_10095`). If no Jira: use `N/A` in the short template fields.
7. **Slack text** – Build **only** the **three blocks** per **`Slack_report_summary_short_template.md`**. Optional **Notes:** (include **`Full narrative:`** + paths only if file upload is **impossible** — missing token, network, etc.).
8. **Slack files (MANDATORY when token exists)** – For **each** of Tester DM (if any) and **`C0AK96S1D7X`** (#ai-report), **upload both** (when the second exists): (a) the **ScopedPlaywright** `.md`, (b) **`Cursor-Project/EnergoTS/playwright-report-detailed.md`**. Use **`upload-file-to-slack.ps1`** with **`SLACK_API_TOKEN`** or **`SLACK_BOT_TOKEN`** — see **`Cursor-Project/config/slack/README.md`**. **`_run-upload-with-dotenv.ps1`** loads **`SLACK_API_TOKEN`** from **`EnergoTS/.env`**. Short text alone is not a complete scoped report.
9. **Recipients** (same as HandsOff when Jira is known):
   - **Tester (DM):** if Jira has Tester (`customfield_10095`), resolve with `slack_search_users`; send the **same short text**, then **upload both** files to that user ID.
   - **#ai-report:** same short text to **`C0AK96S1D7X`**, then **upload both** files.
   - If no Tester in Jira: **text + both file uploads** only to #ai-report.
   - Do **not** send to Assignee unless the user explicitly asks (then confirm).

## Reference

- Short Slack: `Cursor-Project/config/template/Slack_report_summary_short_template.md`
- Detailed file (structure): `Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md`
- Long Slack (optional): `Slack_report_template.md`
- HandsOff parity: `.cursor/commands/hands-off.md` Step 6–7
- Reporting paths: `Cursor-Project/reports/README.md`
