# Summary — hands-off_playwright_report.mdc frontmatter

**Time:** 2026-03-31 (local HHmm 1323)  
**Task:** Fix `.cursor/rules/workflows/handsoff_playwright_report.mdc` so Cursor can apply the rule (per https://cursor.com/docs/context/rules).

## Changes

- Added **`description`** (quoted string) for “Apply intelligently”: HandsOff triggers, test cases, EnergoTS Playwright, validator, reports, Slack, global-setup, agent questions.
- Added **`globs`** so the rule attaches when relevant files are in context: `hands-off` command/agents, `energo-ts-test`, `playwright-test-validator`, `Cursor-Project/test_cases/**`, `EnergoTS/tests/cursor/**/*.spec.ts`, Slack report template, playwright instructions folder.
- Left **`alwaysApply: false`** to avoid loading this long workflow in every chat; Rule 37 in `workflow_rules.mdc` still governs when HandsOff runs.

## Rationale

- Docs: intelligent rules need a **`description`**; file-scoped rules need **`globs`**. Combining both improves recall for HandsOff-related work without global always-on cost.

## Agents involved

Direct edit; no subagents.
