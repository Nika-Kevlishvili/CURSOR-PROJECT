---
name: test-case-quality-validator
description: STRICT quality validator — scores test case .md files on 10 axes (0–100 total, pass ≥80/100). Returns per-TC scores, anti-pattern flags, and rewrite suggestions. Use after test-case-generator produces files, or ad-hoc with /test-case-quality <Topic_name>. READ-ONLY — does not modify files.
disable-model-invocation: true
---

# Test Case Quality Validator Skill

Routes to the **test-case-quality-validator** subagent (`.cursor/agents/test-case-quality-validator.md`).

## When to apply

- After test-case-generator saves `test_cases/Backend/<Topic>.md` (+ `Frontend/<Topic>.md` when TC-FRONTEND scope includes Frontend) — invoke as Step 2.5 / HandsOff Step 1.5 before Playwright bridge.
- User runs `/test-case-quality <Topic_name>` — ad-hoc check on existing files.
- Parent agent (test-case-generator, hands-off) needs second-pass verification after self-scoring.

## What it does

1. Reads the rubric: `Cursor-Project/docs/test_case_quality_rubric.md` (**10-axis, 0–100 model**).
2. Reads TC file(s) for the topic (Backend required; Frontend when it exists).
3. Scores every TC on **10 axes**. **Pass threshold: 80/100**. **STRICT MODE** — no leniency.
4. Returns structured per-TC scores, failing axis reasons, and mandatory fixes.
5. Parent re-invokes test-case-generator with feedback (**max 3 rewrite iterations**).

## Subagent reference

`.cursor/agents/test-case-quality-validator.md`
