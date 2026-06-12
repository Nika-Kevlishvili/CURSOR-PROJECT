---
description: Ad-hoc quality check on an existing Playwright cursor spec. Scores spec against TC .md files (when present) or Jira reproduce steps. Does NOT modify the spec.
---

# /playwright-validate

**Usage:** `/playwright-validate <JIRA_KEY>`

Example: `/playwright-validate PDT-2971`

## What this command does

1. Resolves `Cursor-Project/EnergoTS/tests/cursor/{JIRA_KEY}-*.spec.ts`.
2. Resolves Backend TC path when `Cursor-Project/test_cases/Backend/*` matches the topic; otherwise uses Jira ticket as alignment source (bug-only automation).
3. Invokes **playwright-test-validator** (`.cursor/agents/playwright-test-validator.md`).
4. Returns scored report in chat: **0–100**, PASS/FAIL (≥80 = pass), numbered issues.

## What it does NOT do

- Does NOT modify spec or TC files.
- Does NOT run Playwright (`energo-ts-run` is separate).

To fix a failing spec, re-run energo-ts-test with validator output as context, or edit the spec and re-run this command.

## Rubric reference

`.cursor/skills/playwright-test-validator/SKILL.md` — **10 criteria (0–100 total)**, pass **≥80/100**, max **3** regeneration loops in HandsOff / energo-ts-test flow.
