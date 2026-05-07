---
name: test-case-quality-validator
description: Scores test case .md files against the project quality rubric (6 axes, 0–2 each, pass ≥ 8/12). Returns per-TC scores, anti-pattern flags, and rewrite suggestions. Use after test-case-generator produces files, or ad-hoc with /test-case-quality <Topic_name>. READ-ONLY — does not modify files.
disable-model-invocation: true
---

# Test Case Quality Validator Skill

Routes to the **test-case-quality-validator** subagent (`.cursor/agents/test-case-quality-validator.md`).

## When to apply

- After test-case-generator saves `test_cases/Backend/<Topic>.md` + `test_cases/Frontend/<Topic>.md` — invoke this validator as Step 4 (quality gate) before finalising.
- User runs `/test-case-quality <Topic_name>` — ad-hoc check on existing files.
- Parent agent (test-case-generator, hands-off) needs second-pass verification after self-scoring.

## What it does

1. Reads the rubric: `Cursor-Project/docs/test_case_quality_rubric.md`.
2. Reads both TC files for the topic.
3. Scores every TC on 6 axes (Intent uniqueness, Observable expected, Endpoint specificity, Delta clarity, Risk coverage, Readability). Pass ≥ 8/12.
4. Returns structured per-TC scores, failing axis reasons, and rewrite suggestions.
5. Parent re-invokes test-case-generator with feedback (max 2 rewrite rounds).

## Subagent reference

`.cursor/agents/test-case-quality-validator.md`
