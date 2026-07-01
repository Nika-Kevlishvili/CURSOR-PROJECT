# Slack report — short body + attached detailed `.md` (paths 2 & 3)

**Goal:** The Slack **text** stays **short** (three fixed blocks, matching production notifications). **Per-test detail** (TC mapping, links, expected vs actual) lives **only** in the **detailed Markdown file**. **MANDATORY delivery:** that file MUST be sent to Slack as a **real file attachment** (not pasted in full in chat) to **Tester (DM) + #ai-report** whenever **`SLACK_API_TOKEN`** or **`SLACK_BOT_TOKEN`** is available with **`files:write`** — see **`Cursor-Project/config/slack/upload-file-to-slack.ps1`**, **`Cursor-Project/config/slack/_run-upload-with-dotenv.ps1`** (loads token from **`Cursor-Project/EnergoTS/.env`**), and **`Cursor-Project/config/slack/README.md`**. **Do not** satisfy delivery by pasting disk paths in Slack when the upload script can run — paths in **Notes** are **only** a last resort **after** a failed upload attempt (missing token, script error, or no `files:write`).

**Recipients:** Tester DM + **#ai-report** (`C0AK96S1D7X`) per `hands-off.md` Step 7 (or #ai-report only if no Tester).

**Optional long paste:** **`Slack_report_template.md`** only if the user explicitly asks for the full narrative inside Slack.

---

## Slack text — exactly three blocks (required)

Copy **only** this structure into `slack_send_message` (no per-test bullets here):

```
{JIRA_KEY} – Playwright test results

Jira: {JIRA_KEY}
Title: {ticket_title}
Date: {YYYY-MM-DD}
Assignee: "{assignee_display_name}" / Tester: "{tester_display_name}"

Total: {passed_count} passed, {failed_count} failed, {skipped_count} skipped.
```

Notes:

- Title line must read **`Playwright test results`** (not “run summary”).
- Align **`Assignee` / `Tester`** formatting with Jira (use `"—"` and `"N/A"` when missing).
- Do **not** add “Quick results” bullets or full test paragraphs here — those belong in the **attached** detailed `.md`.

---

## Optional fourth block — Notes (environment / caveats)

Use when setup diverged (no Phoenix script, wrong BASE_URL, MCP gaps, etc.), matching field-report style:

```
Notes:
{free_text_notes — e.g. Phoenix branch script not run (no PowerShell). Swagger dev2 refreshed. Full narrative attached as `{JIRA_KEY}.md`.}
```

If file upload is **not** possible **after** trying the upload script (missing token / `files:write` / script failure), include **`Full narrative:`** + workspace paths to **`Cursor-Project/reports/HandsOff reports/…/{JIRA_KEY}.md`** and **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** (or **`Chat reports`** ScopedPlaywright path + machine file for path 3). **Never** use this block as a substitute for uploads when the token is configured.

---

## File attachments — MANDATORY for Playwright test reports (paths 2 & 3)

1. **Smart report:** save using **`Cursor-Project/config/playwright/Playwright_run_detailed_report_template.md`** (HandsOff **`{JIRA_KEY}.md`** or path 3 **ScopedPlaywright_*.md** under **`Chat reports`**).
2. **Machine report:** run **`generate-detailed-report.mjs`** from **`EnergoTS/`** when **`playwright-report.json`** exists → **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** (same folder as the JSON; Rule **DPR.0**).
3. **Upload** **both** files to Slack for **each** destination (Tester user ID + `C0AK96S1D7X`) via **`upload-file-to-slack.ps1`** with `InitialComment` distinguishing e.g. `Smart report ({JIRA_KEY})` vs `Playwright JSON detailed ({JIRA_KEY})`. **#ai-report** is the **report channel** for these attachments.

**`slack_send_message`** MCP cannot attach files; **always** use the upload script (or **`_run-upload-with-dotenv.ps1`**) for files.

---

## Rules

1. **Three blocks** in Slack text stay **minimal** — header, metadata, totals only.
2. **English only** (Rule 0.7).
3. **Never** put full per-test rows in Slack unless the user explicitly requests **`Slack_report_template.md`** long format.
