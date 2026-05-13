# Save Session Feedback

**On-demand** feedback files under **Feedback** (Rule **0.6**). Invoked when the user runs **`/feedback`** or explicitly asks to save feedback to **`Cursor-Project/reports/Feedback/`**.

## Layout

Path: **`Cursor-Project/reports/Feedback/YYYY/<english-month>/<DD>/Feedback_{Slug}_{HHMM}.md`** per **`Cursor-Project/reports/README.md`**. Reuse `YYYY/month/DD` if it exists; create missing folders only.

### Filename slug (required)

`{Slug}` MUST be a short, filesystem-safe hint of what the feedback is about, inferred from **this chat**:

- Use **kebab-case** English: `gitignore-chat-reports`, `hands-off-reporting-rules`, `feedback-ui-language`, `misc`.
- Keep it **short** (2–5 words) and **avoid** sensitive data, URLs, ticket keys, or usernames.
- If the session has multiple unrelated topics, prefer the latest or dominant one, otherwise use `misc`.

## Rule 0.7 (on-disk language)

The **saved `.md` file** MUST be **English** (headings, body, **`User sentiment:`** as **Liked** / **Disliked** / **Other**). Chat may use the user’s language.

## Workflow

Execute **in order**. Write the file only after you know **`User sentiment:`** (from **`AskQuestion`** or, if that was skipped, from the user’s **chat** reply).

### Step A — Sentiment

**Language (critical):** The **`AskQuestion`** **`prompt`** and **both** option **`label`** strings MUST use the **same** language. Do **not** mix English questions with Georgian options (or vice versa). Choose language from **this chat session**:
- If the user’s messages that led to **`/feedback`** are primarily **Georgian** (or the session is Georgian-heavy) → use **Georgian** for the question **and** both options.
- Otherwise → use **English** for the question **and** both options.
- If the session is mixed, follow the language of the **latest user message** before **`/feedback`**. If still unclear, use **English** for the whole card.

**`AskQuestion`:** one question, **two** options only — sentiment **`liked`** vs **`disliked`** (ids can stay `liked` / `disliked`). **Do not** add a third option in the tool call; if the client UI shows an extra “Other” line, ignore it for counting — map free-text under details to **Other** in the saved file when needed.

| Chat language | `prompt` (example) | Option labels (example) |
|---------------|--------------------|-------------------------|
| Georgian | `როგორი იყო ეს სესია შენთვის?` | **მომეწონა** / **არ მომეწონა** |
| English | `How was this session for you?` | **I liked it** / **I did not like it** |

If **`AskQuestion`** is skipped or the answer is unclear, ask once in **chat** in the **same** language you chose above for **Liked** or **Disliked** (or a short mixed reply → **Other** in the file).

### Step B — Optional detail

If the Questions card or **chat** has extra text, summarize it in English under **Optional detail**; omit the section if empty.

### Step C — Synthesize (English)

From **this chat** only: include:

- A short **session summary**
- A brief **chat context** section with the key **prompt → answer** pairs (concise; do not dump the whole transcript)
- **Confidence** (**high** / **medium** / **low**, one sentence; say **inferred** if you guessed)

### Step D — Write the file

1. **`<segment>`** = `YYYY/<english-month>/<DD>/` from **current system date** at write time (never stale dates).
2. **`{HHMM}`** = local time (24h), e.g. `1430`.
3. Compute **`{Slug}`** (see “Filename slug” above).
4. Write **`Cursor-Project/reports/Feedback/<segment>/Feedback_{Slug}_{HHMM}.md`**:

```markdown
# Feedback — Session notes

**Date:** YYYY-MM-DD
**User sentiment:** Liked | Disliked | Other

## Optional detail
(Extra user text in English summary, or remove section if none.)

## Chat context (prompt → answer)
- **Prompt:** ...
  - **Answer:** ...

## Session summary

## Confidence

---

Agents involved: (Rule 0.1 — agents or "None (direct tool usage)")
```

End the **chat** reply with **`Agents involved:`** (Rule 0.1).

## References

- **`phoenix-reporting`** skill, **`.cursor/agents/report-generator.md`**, **Rule 0.6**.
