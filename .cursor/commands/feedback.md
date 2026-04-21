# Save Session Feedback

**On-demand** feedback files under **Feedback** (Rule **0.6**). Invoked when the user runs **`/feedback`** or explicitly asks to save feedback to **`Cursor-Project/reports/Feedback/`**.

## Layout

Path: **`Cursor-Project/reports/Feedback/YYYY/<english-month>/<DD>/Feedback_{HHMM}.md`** per **`Cursor-Project/reports/README.md`**. **`DD`** = real day-of-month when the file is saved. Reuse `YYYY/month/DD` if it exists; create missing folders only.

## Rule 0.7 (on-disk language)

The **saved `.md` file** MUST be **English** (headings, body, sentiment labels as **Liked** / **Disliked** / **Other**). Chat may use the user’s language.

## Workflow

Execute **in order**. Do **not** write the file until sentiment (and optional detail if **Other**) is collected.

### Step A — User sentiment (structured choice)

Use the **`AskQuestion`** tool with **one** question and **three** options:

- **Option IDs:** `liked`, `disliked`, `other`
- **Labels:** If the user is writing in **Georgian**, use: **მომეწონა** / **არ მომეწონა** / **სხვა**. Otherwise use: **I liked it** / **I did not like it** / **Other**

**Prompt text (examples):** Ask how they felt about the assistant’s help in **this chat** (adapt to the user’s language).

### Step B — Free text when **Other**

If the user selects **`other`**, ask in chat for a **short free-text** explanation **before** writing the file. Wait for their reply. If they chose **`liked`** or **`disliked`**, skip this step (optional one-line comment in chat is fine but not required for the file).

### Step C — Synthesize from the conversation (English for the file)

From **this chat session** only:

1. **Session summary:** Brief bullet or short paragraphs: main **user prompts** → **outcomes** (what was delivered or decided).
2. **Confidence:** How confident the assistant appeared (or stated). If not stated explicitly, use **high** / **medium** / **low** with **one sentence** rationale and mark as **inferred** if you had to estimate.

### Step D — Write the markdown file

1. Resolve `<segment>` = `YYYY/<english-month>/<DD>/` from the **current system date at execution time** (same rules as **`/report`**: never use stale session dates).
2. **`{HHMM}`** = local time when the file is written (24h, zero-padded), e.g. `1430`.
3. Write **`Cursor-Project/reports/Feedback/<segment>/Feedback_{HHMM}.md`** with **at least** these sections:

```markdown
# Feedback — Session notes

**Date:** YYYY-MM-DD
**User sentiment:** Liked | Disliked | Other

## Optional detail
(Only if Other was selected, or if the user provided extra text — summarize faithfully in English.)

## Session summary
(Prompts → outcomes.)

## Confidence
(Statements from chat and/or inferred assessment.)

---

Agents involved: (per Rule 0.1 — list agents or "None (direct tool usage)")
```

**Note:** The line `Agents involved:` belongs **inside the file** as shown; the chat reply should also end with **Agents involved:** per Rule 0.1.

## Date Safety (MANDATORY)

- Always derive folder date from the **current system date at execution time**.
- Never use remembered or session metadata dates for `YYYY/<english-month>/<DD>`.

## References

- **phoenix-reporting** skill and `.cursor/agents/report-generator.md` for path conventions.
- **Rule 0.6:** Feedback disk writes are allowed when **`/feedback`** runs or the user explicitly requests saving feedback here.
