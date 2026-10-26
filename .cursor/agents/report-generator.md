---
name: report-generator
model: inherit
description: Saves Chat reports under Cursor-Project/reports/ per reports/README.md. User-requested reports are Word .docx (PDT-3483 layout, not Excel) plus optional .md; canvas still required. Only when /report, /feedback, or explicit save — not after routine tasks or Rule 32 alone.
---

# Report Generator Subagent

Write files under **`Cursor-Project/reports/`** using:

```text
<Chat reports|Feedback>/YYYY/<english-month>/<DD>/<filename>.md
```

**Resolve folders** only per **`Cursor-Project/reports/README.md`**: use the **actual calendar date** when writing the file (`YYYY`, English `monthname`, zero-padded **`DD`**). Reuse `…/YYYY/month/DD/` if it already exists; create only missing path segments.

**Date safety (MANDATORY):**
- Compute date from current system time at the moment the report is written.
- Never reuse a prior chat/system snapshot date for folder naming.
- If an existing report is found under a wrong day for the same session, rewrite it under the correct day and remove the wrong-day copy.

## When to run

- User or parent asks to save a report.
- **`/report`** or equivalent.
- **`/feedback`** → **Feedback** area (`Feedback_{Slug}_{HHMM}.md` per **`phoenix-reporting`** skill).

**Do not** run automatically after every generic task.

## Workflow (no Python ReportingService)

1. Determine **area** (Chat reports / Feedback).
2. Compute **`<segment>`** = `YYYY/<english-month>/<DD>/` from the **current system date** per README.
3. Write `Cursor-Project/reports/<area>/<segment>/<filename>.md` when a markdown companion is needed.
4. For a **user-requested report**, also write a **Word `.docx`** in that same folder per **`.cursor/skills/word-report/SKILL.md`** (not Excel). Still write a Cursor canvas. `/feedback` stays markdown only.

## Output

- Confirm full path saved.
- End with **Agents involved:** (Rule 0.1).

## Constraints

- On-disk text in **English** (Rule 0.7).
