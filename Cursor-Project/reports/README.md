# Reports (`Cursor-Project/reports/`)

## Top-level areas

| Folder | Use |
|--------|-----|
| **Chat reports/** | Bug validation, optional summaries, `/report`, other chat-session markdown. |
| **HandsOff reports/** | HandsOff Playwright result files: **`{JIRA_KEY}.md`**. |
| **Feedback/** | Feedback captures when the user asks to save them here. |

## Date hierarchy (all three areas)

Every saved report lives under:

```text
<Area>/YYYY/<monthname>/<DD>/<filename>.md
```

- **`YYYY`** — 4-digit **calendar year** on the day the file is written (report creation date). Reuse the folder if it already exists.
- **`monthname`** — English month name, **lowercase** (`january` … `december`) for that same calendar date. Reuse if it exists.
- **`<DD>`** — **Two-digit, zero-padded day-of-month (01–31)** for that **same** calendar date — the **real** day when the report is created/saved, not a made-up range. Example: file saved on **12 April 2026** → **`…/2026/april/12/`**.

### Reuse vs create

1. Compute **`Y`**, **`monthname`**, and **`DD`** from the **actual date/time when you write the file** (use the real “today” / run date for that save operation — do not invent another day).
2. Under `…/<Area>/Y/monthname/`, if folder **`DD`** already exists, save the new file **there** (multiple reports can share the same day folder).
3. If folder **`DD`** does not exist, **create** `…/Y/monthname/DD/` then write the file.

**No fictional day ranges:** the folder name is **only** the real calendar **`DD`** for the save date (e.g. saved on the 12th → folder **`12`**, not `03-14`).

## Full path examples

- Bug validation saved on 12 Apr 2026: `Cursor-Project/reports/Chat reports/2026/april/12/BugValidation_MyBug.md`
- HandsOff run same day: `Cursor-Project/reports/HandsOff reports/2026/april/12/REG-123.md`
- Feedback same day: `Cursor-Project/reports/Feedback/2026/april/12/Stakeholder_notes.md`

## Implementation

There is **no** Python `ReportingService`. Create missing directories with file tools, then write the `.md` file.

Rule **0.6** (`.cursor/rules/main/core_rules.mdc`) stays authoritative for *when* to write files; **this README** is authoritative for *where* under `reports/`.
