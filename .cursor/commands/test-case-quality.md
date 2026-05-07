---
description: Ad-hoc quality check on an existing test case topic. Scores Backend/<Topic>.md and Frontend/<Topic>.md against the rubric. Does NOT re-generate test cases.
---

# /test-case-quality

**Usage:** `/test-case-quality <Topic_name>`

Example: `/test-case-quality Invoice_cancellation`

## What this command does

1. Reads `Cursor-Project/test_cases/Backend/<Topic_name>.md` and `Cursor-Project/test_cases/Frontend/<Topic_name>.md`.
2. Invokes the **test-case-quality-validator** subagent (`.cursor/agents/test-case-quality-validator.md`).
3. Returns a scored report in chat: per-TC scores on 6 axes, PASS/FAIL per TC, summary.

## What it does NOT do

- Does NOT modify any test case file.
- Does NOT re-run test case generation.
- Does NOT invoke cross-dependency-finder.

To fix failing TCs after this report, run `/test-case-generate <Jira_key>` with the validator output as context, or manually edit the failing TCs and re-run this command.

## Rubric reference

`Cursor-Project/docs/test_case_quality_rubric.md` — 6 axes (0–2 each), pass ≥ 8/12.
