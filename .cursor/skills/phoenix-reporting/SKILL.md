---
name: phoenix-reporting
description: Writes markdown under Cursor-Project/reports/ using Chat reports, HandsOff reports, or Feedback plus YYYY/monthname/DD (real calendar day of save). See Cursor-Project/reports/README.md. Rule 0.6 applies for when to write.
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
