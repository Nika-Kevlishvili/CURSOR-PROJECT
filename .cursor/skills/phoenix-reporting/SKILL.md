---
name: phoenix-reporting
description: On-demand only: writes markdown under Cursor-Project/reports/ using Chat reports, HandsOff reports, or Feedback plus YYYY/monthname/DD per reports/README.md. Rule 0.6 — no automatic post-task files; never use flat reports/YYYY-MM-DD/ at reports root.
---

# Phoenix Reporting

**Authoritative layout:** **`Cursor-Project/reports/README.md`**

Path pattern:

```text
Cursor-Project/reports/<Chat reports|HandsOff reports|Feedback>/YYYY/<english-month>/<DD>/<filename>.md
```

- **Reuse** existing `YYYY`, `monthname`, and **`DD`** folders for that same calendar day; **create** only missing path segments.
- **Rule 37 (HandsOff):** `{JIRA_KEY}.md` → **HandsOff reports** (mandatory when that workflow runs)
- **`/report` or explicit user request:** → **Chat reports** (or **Feedback/** if the user names it). This is the **only** default path for new session/report `.md` files on disk.
- **Rule 32 (bug validation):** analysis stays **in chat**; `BugValidation_*.md` under **Chat reports** only if the user also runs **`/report`** or explicitly asks to save.

No Python `ReportingService`. English on disk (Rule 0.7).

**Do not:** create `Summary_*.md`, `BugValidation_*.md`, or other report files after routine tasks, bug validation, or tests without **`/report`** or an explicit save request — except **HandsOff** → **HandsOff reports** per Rule 37. **Do not** use `Cursor-Project/reports/YYYY-MM-DD/` as a report root.
