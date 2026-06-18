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
- **`/report` or explicit user request:** → **Chat reports** (or **Feedback/** if the user names that area for a generic report).
- **`/feedback` or explicit save-feedback request:** → **Feedback** as `Feedback_{Slug}_{HHMM}.md` (workflow below).
- **Rule 32 (bug validation):** analysis stays **in chat**; `BugValidation_*.md` under **Chat reports** only if the user also runs **`/report`** or explicitly asks to save.

No Python `ReportingService`. English on disk (Rule 0.7).

**Atlassian links:** Confluence → `https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/{id}/{slug}`; Jira PDT → `https://oppa-support.atlassian.net/browse/{KEY}`. Never `oppa-support` for wiki. See **`.cursor/rules/integrations/atlassian_link_format.mdc`**.

**Do not:** create `Summary_*.md`, `BugValidation_*.md`, `Feedback_*.md`, or other report files after routine tasks, bug validation, or tests without **`/report`**, **`/feedback`**, or an explicit save request — except **HandsOff** → **HandsOff reports** per Rule 37. **Do not** use `Cursor-Project/reports/YYYY-MM-DD/` as a report root.

## Chat reports naming (optional)

When the user or workflow saves under **Chat reports**:

- Per-agent export: `…/Chat reports/<segment>/{AgentName}_{HHMM}.md` (example: `PhoenixExpert_1430.md`).
- Summary export: `…/Chat reports/<segment>/Summary_{HHMM}.md`.

`<segment>` = `YYYY/<english-month>/<DD>/` per **`reports/README.md`**; **`DD`** is the real calendar day when the file is written.

## Date safety (mandatory)

Always derive `YYYY`, English month folder name, and **`DD`** from the **current system date/time at execution/write**. Never use remembered session metadata if it disagrees with the actual calendar date.

## Feedback saves (`/feedback`)

Path: **`Cursor-Project/reports/Feedback/YYYY/<english-month>/<DD>/Feedback_{Slug}_{HHMM}.md`**.

### Filename slug

`{Slug}` — short **kebab-case** English hint from **this chat** (e.g. `hands-off-reporting-rules`, `misc`). Avoid sensitive data, URLs, ticket keys, usernames. Prefer latest dominant topic if mixed.

### Workflow (order)

1. **Sentiment:** Prefer **`AskQuestion`** with **exactly two** options (`liked` / `disliked` ids). **`prompt` and both option `label` strings MUST share one language** — Georgian session → Georgian question + labels; otherwise English; if mixed, follow the **latest user message** before `/feedback`, else English. Example labels: Georgian **მომეწონა** / **არ მომეწონა**; English **I liked it** / **I did not like it**. If the tool is skipped, ask once in chat in that same language.
2. **Optional detail:** Extra card/chat text → summarized in English under **Optional detail** (omit section if empty).
3. **Synthesize (English):** Session summary; concise **prompt → answer** pairs; **Confidence** (high/medium/low, one sentence; say **inferred** if guessed).
4. **Write file:** Use current system date for `<segment>`; `{HHMM}` = local 24h time.

### On-disk template

```markdown
# Feedback — Session notes

**Date:** YYYY-MM-DD
**User sentiment:** Liked | Disliked | Other

## Optional detail

## Chat context (prompt → answer)

## Session summary

## Confidence

---

Agents involved: …
```

End the **chat** reply with **`Agents involved:`** (Rule 0.1).
