# Generate Task Report

**On-demand** report files (Rule **0.6** — optional by default; use this command or an explicit user request).

## Layout

All paths: **`Cursor-Project/reports/Chat reports/YYYY/<english-month>/<DD>/…`** per **`Cursor-Project/reports/README.md`**. **`DD`** = real day-of-month when the file is saved (not a fictional range). Reuse `YYYY/month/DD` if it exists; create missing folders only.

## When to write report files

- User asks for a report, export, or “save to reports”.
- This **`/report`** command is invoked.
- A workflow **mandates** a file (e.g. Rule 32 BugValidation, Rule 37 HandsOff — those follow the same README layout under their area).

**Do not** create `Summary_*.md` / per-agent files after every routine task unless the user wants them.

## Report types (Chat reports area)

### 1. Agent-specific reports (optional)

- **Path:** `Cursor-Project/reports/Chat reports/<segment>/{AgentName}_{HHMM}.md` where `<segment>` = `YYYY/<english-month>/<DD>/` per README.
- **Example (file saved on 12 Apr 2026):** `Cursor-Project/reports/Chat reports/2026/april/12/PhoenixExpert_1430.md`

### 2. Summary report (optional)

- **Path:** `Cursor-Project/reports/Chat reports/<segment>/Summary_{HHMM}.md`

## Workflow (this workspace)

There is **no** Python `ReportingService`. Use editor/file tools:

1. Resolve `<segment>` per **`Cursor-Project/reports/README.md`**.
2. Write the markdown file(s) requested.
3. See **phoenix-reporting** skill and `.cursor/agents/report-generator.md`.
