---
name: test-case-generator
description: Routes test case generation to the correct pipeline (cross-dep → generate → quality validate). Use when user asks to generate test cases or scenarios.
---

# Test Case Generator Skill

Routes test case generation work through the correct **pipeline sequence**.

## When to Apply

- User asks to generate test cases for a bug, task, or feature.
- User wants test scenarios or test derivation from a description.
- User mentions "test cases", "test scenarios", "generate tests".

## Action

1. For full pipeline (recommended), delegate to **qa-workflow** orchestrator — Pipeline 1.
   - Handles: environment → cross-dep → generate → quality validate → report.
2. If cross-dependency data is already available, delegate directly to **test-case-generator** agent with `context['cross_dependency_data']`.
3. After generation, **test-case-quality-validator** MUST score all TCs (max 2 rewrite rounds).

## Do NOT

- Skip the cross-dependency-finder step.
- Skip the quality validation step.
- Duplicate the generation workflow here — the agent has the full procedure.

## Key constraints

- Output: two `.md` files per topic — `test_cases/Backend/<Topic>.md` + `test_cases/Frontend/<Topic>.md`.
- Preconditions: DRY — shared setup once, per-TC deltas only.
- Template: `templates/Test_case_template.md`.
- Quality: `templates/test_case_quality_rubric.md` — 6 axes, 8/12 threshold.

## Reference

- Agent: `agents/test-case-generator.md`
- Quality: `agents/test-case-quality-validator.md`
- Orchestrator: `agents/qa-workflow.md` (Pipeline 1)
