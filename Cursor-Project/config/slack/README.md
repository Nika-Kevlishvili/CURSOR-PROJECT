# Slack helpers (`config/slack`)

## `upload-file-to-slack.ps1`

Uploads a **local file** (e.g. the detailed HandsOff `{JIRA_KEY}.md`) to a Slack **channel** or **DM** using Slack Web API external upload (`files.getUploadURLExternal` → binary POST → `files.completeUploadExternal`).

**Requires:** `SLACK_API_TOKEN` (EnergoTS / CI) or `SLACK_BOT_TOKEN` — **Bot User OAuth Token** (`xoxb-...`) with **`files:write`**. **App-level tokens** (`xapp-...`) do **not** work for this upload API. Do **not** commit tokens.

**HandsOff / path 3 — mandatory files:** Deliver **two** attachments when both exist: (1) smart report **`{JIRA_KEY}.md`** (HandsOff) or **ScopedPlaywright_*.md** (path 3), (2) **`Cursor-Project/EnergoTS/playwright-report-detailed.md`** from **`generate-detailed-report.mjs`** (run from **`EnergoTS/`**; same folder as **`playwright-report.json`**). Plus the short three-block Slack message — never replace files with full narrative in chat. Run uploads **after** the short Slack text. For **each** file, upload to (1) Tester **user ID** (DM) if present, then (2) **#ai-report** **`C0AK96S1D7X`** (up to **four** uploads per file pair). Use **`_run-upload-with-dotenv.ps1`** if the token lives in **`EnergoTS/.env`** as **`SLACK_API_TOKEN`**.

Example (from workspace root):

```powershell
$env:SLACK_API_TOKEN = '<from secure storage>'
powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/slack/upload-file-to-slack.ps1" `
  -FilePath "Cursor-Project/reports/HandsOff reports/2026/may/01/PHN-2823.md" `
  -ChannelOrUserId "C0AK96S1D7X" `
  -InitialComment "Detailed Playwright report (HandsOff)"
```

If the token is unavailable **or** upload fails after attempting the script, keep the **Notes** line in the short Slack body with workspace paths so testers can open files locally — **not** as a substitute when upload is possible.
