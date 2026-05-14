---
description: Save the current analysis or session summary as a markdown report to disk.
---

# /report

**Usage:** `/report`

## What this command does

1. Collects the current chat session's analysis, findings, or summary.
2. Determines the report area (e.g. Chat reports, Bug reports, Feedback).
3. Computes the date-organized path: `reports/<area>/YYYY/<month>/<DD>/`.
4. Saves the report as a `.md` file in that path.
5. Confirms the full file path in chat.

## When to use

- After a bug validation: `/report` saves the full analysis to disk.
- After a test case generation session: `/report` saves a summary.
- After any analysis you want to persist: `/report`.

## What it does NOT do

- Does NOT generate new analysis — it saves what was already produced in chat.
- Does NOT auto-run after every task — only when you explicitly invoke `/report`.
