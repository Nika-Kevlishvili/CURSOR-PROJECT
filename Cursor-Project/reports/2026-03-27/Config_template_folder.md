# Move workflow templates to `config/template/`

**Date:** 2026-03-27

## Changes

- Created `Cursor-Project/config/template/` with:
  - `Test_case_template.md` (moved from `config/`)
  - `Slack_report_template.md` (moved from `config/`)
  - `README.md` (index of templates)
- Updated references to **`Cursor-Project/config/template/Test_case_template.md`** and **`Cursor-Project/config/template/Slack_report_template.md`** in:
  - `.cursor/agents/playwright-test-validator.md`
  - `.cursor/agents/test-case-generator.md`
  - `.cursor/rules/workflows/handsoff_playwright_report.mdc`
  - `.cursor/rules/workspace/test_cases_structure.mdc`
  - `.cursor/commands/test-case-generate.md`
  - `.cursor/commands/hands-off.md`
- Documented `config/template/` in `file_organization_rules.mdc` and `.cursor/skills/phoenix-file-organization/SKILL.md`.

Agents involved: None (direct tool usage)
