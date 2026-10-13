---
name: word-report
description: >-
  Builds shareable Word (.docx) reports in the PDT-3483 exemplar layout instead of Excel.
  Use when the user asks for a report, Word report, რეპორტი, /report, or to save test/analysis
  results as a file they can send. Keep writing Cursor canvases; do not replace canvas with Word.
---

# Word report (not Excel)

When the user asks for a **report**, give them a **Word `.docx`** they can send. Do **not** make Excel the report. Keep writing a **Cursor canvas**. Optionally keep a `.md` companion in Chat reports (Rule 0.6).

**Load this skill** before writing any user-requested report file.

## Exemplar (copy this look)

| Piece | Path |
|-------|------|
| Word look & structure | `Cursor-Project/reports/Chat reports/2026/September/18/PDT-3483_Final_Liability_Amount_Dev_Report.docx` |
| Same content in markdown | `Cursor-Project/reports/Chat reports/2026/September/18/Summary_1133.md` |
| Style helpers | `.cursor/skills/word-report/scripts/docx_style.py` |

Open the `.docx` if unsure. Match Calibri, heading sizes, blue hyperlinks, gray table header, green **Passed**, horizontal rules, live numbers in the story (not a separate Excel 50 example unless that is the live data).

## When it applies

- User says report / რეპორტი / Word / save this / `/report`.
- Test-run write-up they will send (cases, amounts, portal links).

**Unchanged:** `/feedback` markdown; Playwright Slack path 2 (`ScopedPlaywright_*.md`); Rule 32 stays chat-only unless they also ask to save.

## Deliverables (together)

1. **Word** — `Cursor-Project/reports/Chat reports/YYYY/<english-month>/DD/<Topic>_Report.docx` (English name).
2. **Canvas** — still write under the canvases folder. Word does **not** replace canvas.
3. **Chat** — short path to the `.docx` plus the same stories/numbers/links.
4. **Optional `.md`** — same folder, same structure (Rule 0.6).

## Structure (every Word report)

1. **Title** — what was checked, where.
2. **Meta lines** — Ticket (link), Environment, When, Result (`8 / 8 passed` style).
3. **Intro** — human English: what the figure/check means; formula in words.
4. **How we built it** — short bullets (setup gates).
5. **At a glance** — one table: Case | In one line | live numbers | Remaining/result | Passed.
6. **Per-case sections** — heading; one story paragraph **with live amounts**; `Live:` line; bullet **hyperlinks** to Dev objects.
7. **Closing note** — one or two facts (document types, caveats).

Omit Cursor-only footers (Confidence, Agents involved) from the Word file. Keep those in chat.

## Voice

- Human English on disk (Rule 0.7). Chat may stay in the user’s language.
- Use the **live** invoice / compensation / remaining amounts in the story.
- Do not put Excel worked examples (T=50) in the story if live amounts differ.

## Build

Use **python-docx** + `docx_style.py` (fonts, hyperlinks, table, rules). A4 portrait, Calibri, ~1.8 cm margins. Clickable portal URLs.

```text
python .cursor/skills/word-report/scripts/docx_style.py
```

That module is a library — import helpers from a short per-report script, write the `.docx`, delete the one-off script if it is only case data.

## Do not

- Excel `.xlsx` as the report (unless the user explicitly asks for a spreadsheet).
- Stop writing canvases because Word exists.
- Paste Slack-unsafe tables as the only deliverable.
- Auto-write reports after routine tasks (Rule 0.6).
