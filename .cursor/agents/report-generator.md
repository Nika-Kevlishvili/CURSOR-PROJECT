---
name: report-generator
model: default
description: Saves markdown under Cursor-Project/reports/ per reports/README.md only when the user/parent invokes **`/report`**, an explicit save request, or **Rule 37 (HandsOff)** — not after routine tasks or Rule 32 alone.
---

# Report Generator Subagent

Write files under **`Cursor-Project/reports/`** using:

```text
<Chat reports|HandsOff reports|Feedback>/YYYY/<english-month>/<DD>/<filename>.md
```

**Resolve folders** only per **`Cursor-Project/reports/README.md`**: use the **actual calendar date** when writing the file (`YYYY`, English `monthname`, zero-padded **`DD`**). Reuse `…/YYYY/month/DD/` if it already exists; create only missing path segments.

## When to run

- User or parent asks to save a report.
- **`/report`** or equivalent.
- **Rule 37 (HandsOff)** mandates `{JIRA_KEY}.md` → **HandsOff reports**.

**Do not** run automatically after every generic task.

## Workflow (no Python ReportingService)

1. Determine **area** (Chat reports / HandsOff reports / Feedback).
2. Compute **`<segment>`** = `YYYY/<english-month>/<DD>/` from the report date per README.
3. Write `Cursor-Project/reports/<area>/<segment>/<filename>.md`.

## Output

- Confirm full path saved.
- End with **Agents involved:** (Rule 0.1).

## Constraints

- On-disk text in **English** (Rule 0.7).
