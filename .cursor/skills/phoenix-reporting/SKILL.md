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
- **Rule 32:** `BugValidation_[Name].md` → **Chat reports**
- **Rule 37:** `{JIRA_KEY}.md` → **HandsOff reports**
- **`/report` / optional:** → **Chat reports** (unless user names Feedback)
- **Feedback:** only when the user asks

No Python `ReportingService`. English on disk (Rule 0.7).

**Do not:** create `Summary_*.md` or agent run logs after routine chat/tasks unless the user asks, `/report`, or a workflow mandates it (Rule 0.6 default). **Do not** use `Cursor-Project/reports/YYYY-MM-DD/` as a report root — only the three named areas above.
