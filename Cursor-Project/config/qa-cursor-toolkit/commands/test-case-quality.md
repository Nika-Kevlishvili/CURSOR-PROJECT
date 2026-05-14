---
description: Ad-hoc quality check on an existing test case topic. Scores Backend/<Topic>.md and Frontend/<Topic>.md against the quality rubric. Does NOT re-generate test cases.
---

# /test-case-quality

**Usage:** `/test-case-quality <Topic_name>`

Example: `/test-case-quality Invoice_cancellation`

## What this command does

1. Reads `test_cases/Backend/<Topic_name>.md` and `test_cases/Frontend/<Topic_name>.md`.
2. Invokes the **test-case-quality-validator** subagent.
3. Returns a scored report in chat: per-TC scores on 6 axes, PASS/FAIL per TC, summary.

## What it does NOT do

- Does NOT modify any test case file.
- Does NOT re-run test case generation.
- Does NOT invoke cross-dependency-finder.

To fix failing TCs after this report, re-run test case generation with the validator output as context, or manually edit the failing TCs and re-run this command.

## Quality rubric

6 axes scored 0–2 each (max 12). Pass threshold: **8/12**.

| Axis | What it measures |
|------|-----------------|
| 1. Intent uniqueness | Does this TC test something distinct? |
| 2. Observable expected result | Is the expected outcome specific and verifiable? |
| 3. Endpoint/action specificity | Are API endpoints or UI actions clearly specified? |
| 4. Delta clarity | Are TC-specific precondition deltas explicit? |
| 5. Risk coverage | Does this TC cover a risk from cross-dependency data? |
| 6. Readability | Is the TC clear, well-structured, and human-readable? |
